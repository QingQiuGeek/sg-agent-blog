package com.example.blog.modules.article.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.example.blog.modules.article.mapper.ArticleVectorMapper;
import com.example.blog.modules.article.model.entity.ArticleVector;
import com.example.blog.modules.article.model.vo.ArticleHitVO;
import com.example.blog.modules.article.service.ArticleVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Service
public class ArticleVectorServiceImpl implements ArticleVectorService {

    /** Embedding 模型单次调用上限：留出余量避免 token 超限（中文 1 字 ≈ 1 token） */
    private static final int MAX_INPUT_CHARS = 6000;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private ArticleVectorMapper articleVectorMapper;

    @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-v3}")
    private String modelName;

    @Override
    @Async("vectorTaskExecutor")
    public void embedAndSaveAsync(Long articleId, String title, String summary, String content) {
        if (articleId == null) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            String text = buildSourceText(title, summary, content);
            if (StrUtil.isBlank(text)) {
                log.warn("文章 {} 源文本为空，跳过向量化", articleId);
                return;
            }

            // 内容未变化则跳过，节省 token
            String hash = SecureUtil.sha256(text);
            String existingHash = articleVectorMapper.findSourceHashByArticleId(articleId);
            if (hash.equals(existingHash)) {
                log.debug("文章 {} 内容未变化，跳过向量化", articleId);
                return;
            }

            // 调用 Embedding 模型生成向量
            Response<Embedding> response = embeddingModel.embed(text);
            float[] vector = response.content().vector();

            ArticleVector record = ArticleVector.builder()
                    .articleId(articleId)
                    .embeddingStr(toJsonArrayString(vector))
                    .dimension(vector.length)
                    .modelName(modelName)
                    .sourceHash(hash)
                    .build();
            articleVectorMapper.upsert(record);

            log.info("文章 {} 向量化完成，dim={}，耗时 {} ms",
                    articleId, vector.length, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 向量化失败不能影响文章发布主流程，仅记录日志由人工/补偿任务处理
            log.error("文章 {} 向量化失败：{}", articleId, e.getMessage(), e);
        }
    }

    @Override
    public List<ArticleHitVO> searchByQuery(String query, int topK,
                                            String categoryName,
                                            List<String> tagNames,
                                            String authorName) {
        if (StrUtil.isBlank(query)) {
            return Collections.emptyList();
        }
        int safeTopK = Math.min(Math.max(topK, 1), 20);
        long start = System.currentTimeMillis();
        try {
            Response<Embedding> response = embeddingModel.embed(query);
            float[] qv = response.content().vector();

            // 元数据预过滤 + Java 端余弦相似度（MySQL 9 社区版无 DISTANCE 函数）
            List<Map<String, Object>> rows = articleVectorMapper.listAllForSearch(
                    StrUtil.trimToNull(categoryName),
                    tagNames,
                    StrUtil.trimToNull(authorName));
            List<ArticleHitVO> hits = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String embStr = (String) row.get("embeddingStr");
                if (StrUtil.isBlank(embStr)) {
                    continue;
                }
                float[] dv = parseVector(embStr);
                if (dv.length != qv.length) {
                    continue;
                }
                double score = cosineSimilarity(qv, dv);

                ArticleHitVO hit = new ArticleHitVO();
                hit.setArticleId(toLong(row.get("articleId")));
                hit.setTitle((String) row.get("title"));
                hit.setSummary((String) row.get("summary"));
                hit.setCover((String) row.get("cover"));
                hit.setViewCount(toLong(row.get("viewCount")));
                hit.setLikeCount(toLong(row.get("likeCount")));
                hit.setFavoriteCount(toLong(row.get("favoriteCount")));
                Object pt = row.get("publishTime");
                if (pt instanceof LocalDateTime ldt) {
                    hit.setPublishTime(ldt);
                }
                hit.setAuthorId(toLong(row.get("authorId")));
                hit.setAuthorNickname((String) row.get("authorNickname"));
                hit.setScore(score);
                hits.add(hit);
            }
            hits.sort(Comparator.comparingDouble(ArticleHitVO::getScore).reversed());
            List<ArticleHitVO> topHits = hits.size() > safeTopK ? hits.subList(0, safeTopK) : hits;
            log.info("文章语义检索：query=[{}], topK={}, filter=[cat={}, tags={}, author={}], scanned={}, hits={}, cost={}ms",
                    query, safeTopK, categoryName, tagNames, authorName,
                    rows.size(), topHits.size(), System.currentTimeMillis() - start);
            return new ArrayList<>(topHits);
        } catch (Exception e) {
            log.error("文章语义检索失败：query=[{}]，{}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** 解析 STRING_TO_VECTOR 的字符串形式（形如 "[0.1,-0.2,...]"） */
    private float[] parseVector(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isEmpty()) return new float[0];
        String[] parts = trimmed.split(",");
        float[] arr = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            arr[i] = Float.parseFloat(parts[i].trim());
        }
        return arr;
    }

    /** 余弦相似度 [-1,1]，越大越相似；维度已校验 */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void deleteByArticleId(Long articleId) {
        if (articleId == null) {
            return;
        }
        try {
            int rows = articleVectorMapper.deleteByArticleId(articleId);
            if (rows > 0) {
                log.info("已删除文章 {} 的向量记录", articleId);
            }
        } catch (Exception e) {
            log.error("删除文章 {} 向量记录失败：{}", articleId, e.getMessage(), e);
        }
    }

    /**
     * 构造用于向量化的源文本：标题 + 摘要 + 正文（截断），权重靠拼接顺序自然体现
     */
    private String buildSourceText(String title, String summary, String content) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(title)) {
            sb.append(title).append('\n');
        }
        if (StrUtil.isNotBlank(summary)) {
            sb.append(summary).append('\n');
        }
        if (StrUtil.isNotBlank(content)) {
            String c = content.length() > MAX_INPUT_CHARS
                    ? content.substring(0, MAX_INPUT_CHARS)
                    : content;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    /**
     * float[] 序列化成 STRING_TO_VECTOR 接受的 JSON 数组字符串
     */
    private String toJsonArrayString(float[] vector) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            sj.add(Float.toString(v));
        }
        return sj.toString();
    }
}
