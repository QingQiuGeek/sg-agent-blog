package com.example.blog.modules.article.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.example.blog.modules.article.mapper.ArticleVectorMapper;
import com.example.blog.modules.article.model.entity.ArticleVector;
import com.example.blog.modules.article.service.ArticleVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
