package com.sg.blog.modules.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.sg.blog.modules.knowledge.mapper.DocumentVectorMapper;
import com.sg.blog.modules.knowledge.mapper.KnowledgeBaseFileMapper;
import com.sg.blog.modules.knowledge.model.entity.DocumentVector;
import com.sg.blog.modules.knowledge.model.entity.KnowledgeBaseFile;
import com.sg.blog.modules.knowledge.model.vo.KbHitVO;
import com.sg.blog.modules.knowledge.service.KnowledgeBaseVectorService;
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
public class KnowledgeBaseVectorServiceImpl implements KnowledgeBaseVectorService {

    /** 单 chunk 字符上限 */
    private static final int CHUNK_SIZE = 800;
    /** 相邻 chunk 重叠字符数（保留上下文连续性） */
    private static final int CHUNK_OVERLAP = 200;
    /** 单文件最多切多少个 chunk，防止超大文件失控 */
    private static final int MAX_CHUNKS_PER_FILE = 200;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private DocumentVectorMapper documentVectorMapper;

    @Resource
    private KnowledgeBaseFileMapper knowledgeBaseFileMapper;

    @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-v3}")
    private String modelName;

    /* ===================== 向量化 ===================== */

    @Override
    @Async("vectorTaskExecutor")
    public void embedFileAsync(Long fileId, String fileText) {
        if (fileId == null) return;
        long start = System.currentTimeMillis();

        // 先把旧向量清掉，确保重传/重试时不会留下脏数据
        try {
            documentVectorMapper.deleteByFileId(fileId);
        } catch (Exception e) {
            log.warn("清理旧 chunk 向量失败 fileId={}: {}", fileId, e.getMessage());
        }

        if (StrUtil.isBlank(fileText)) {
            updateFileStatus(fileId, KnowledgeBaseFile.STATUS_FAILED, 0, "文件文本为空，无法向量化");
            return;
        }

        try {
            List<String> chunks = chunk(fileText, CHUNK_SIZE, CHUNK_OVERLAP, MAX_CHUNKS_PER_FILE);
            int success = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                if (StrUtil.isBlank(chunkText)) continue;
                try {
                    Response<Embedding> response = embeddingModel.embed(chunkText);
                    float[] vector = response.content().vector();

                    KnowledgeBaseFile fileRow = knowledgeBaseFileMapper.selectById(fileId);
                    Long kbId = fileRow != null ? fileRow.getKbId() : null;
                    if (kbId == null) {
                        log.warn("文件已被删除，跳过向量化 fileId={}", fileId);
                        return;
                    }

                    DocumentVector record = DocumentVector.builder()
                            .kbId(kbId)
                            .fileId(fileId)
                            .chunkIndex(i)
                            .chunkText(chunkText)
                            .embeddingStr(toJsonArrayString(vector))
                            .dimension(vector.length)
                            .modelName(modelName)
                            .sourceHash(SecureUtil.sha256(chunkText))
                            .build();
                    documentVectorMapper.insertOne(record);
                    success++;
                } catch (Exception e) {
                    log.warn("chunk 向量化失败 fileId={} chunkIndex={}: {}", fileId, i, e.getMessage());
                }
            }

            updateFileStatus(fileId,
                    success > 0 ? KnowledgeBaseFile.STATUS_SUCCESS : KnowledgeBaseFile.STATUS_FAILED,
                    success,
                    success > 0 ? null : "全部 chunk 向量化失败");

            log.info("文件 {} 向量化完成 chunks={}/{}，耗时 {} ms",
                    fileId, success, chunks.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("文件 {} 向量化整体失败：{}", fileId, e.getMessage(), e);
            updateFileStatus(fileId, KnowledgeBaseFile.STATUS_FAILED, 0,
                    StrUtil.subWithLength(e.getMessage(), 0, 480));
        }
    }

    @Override
    public void deleteByFileId(Long fileId) {
        if (fileId == null) return;
        try {
            int rows = documentVectorMapper.deleteByFileId(fileId);
            if (rows > 0) log.info("已删除文件 {} 的 {} 条 chunk 向量", fileId, rows);
        } catch (Exception e) {
            log.error("删除文件 {} 向量失败：{}", fileId, e.getMessage(), e);
        }
    }

    @Override
    public void deleteByKbId(Long kbId) {
        if (kbId == null) return;
        try {
            int rows = documentVectorMapper.deleteByKbId(kbId);
            if (rows > 0) log.info("已删除知识库 {} 的 {} 条 chunk 向量", kbId, rows);
        } catch (Exception e) {
            log.error("删除知识库 {} 向量失败：{}", kbId, e.getMessage(), e);
        }
    }

    /* ===================== 检索 ===================== */

    @Override
    public List<KbHitVO> searchByQuery(String query, List<Long> kbIds, int topK) {
        if (StrUtil.isBlank(query) || kbIds == null || kbIds.isEmpty()) {
            return Collections.emptyList();
        }
        int safeTopK = Math.min(Math.max(topK, 1), 10);
        long start = System.currentTimeMillis();
        try {
            Response<Embedding> response = embeddingModel.embed(query);
            float[] qv = response.content().vector();

            List<Map<String, Object>> rows = documentVectorMapper.listForSearchByKbIds(kbIds);
            List<KbHitVO> hits = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String embStr = (String) row.get("embeddingStr");
                if (StrUtil.isBlank(embStr)) continue;
                float[] dv = parseVector(embStr);
                if (dv.length != qv.length) continue;
                double score = cosineSimilarity(qv, dv);
                hits.add(KbHitVO.builder()
                        .kbId(toLong(row.get("kbId")))
                        .kbName((String) row.get("kbName"))
                        .fileId(toLong(row.get("fileId")))
                        .fileName((String) row.get("fileName"))
                        .chunkIndex(toInt(row.get("chunkIndex")))
                        .chunkText((String) row.get("chunkText"))
                        .score(score)
                        .build());
            }
            hits.sort(Comparator.comparingDouble(KbHitVO::getScore).reversed());
            List<KbHitVO> top = hits.size() > safeTopK ? hits.subList(0, safeTopK) : hits;
            log.info("知识库语义检索：query=[{}], kbIds={}, scanned={}, topK={}, hits={}, cost={}ms",
                    query, kbIds, rows.size(), safeTopK, top.size(), System.currentTimeMillis() - start);
            return new ArrayList<>(top);
        } catch (Exception e) {
            log.error("知识库语义检索失败：query=[{}]，{}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ===================== 工具方法 ===================== */

    /** 滑动窗口切片：每 size 字一段，相邻段 overlap 字重叠 */
    private List<String> chunk(String text, int size, int overlap, int maxChunks) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) return result;
        String norm = text.replaceAll("\\s+", " ").trim();
        int len = norm.length();
        int step = Math.max(size - overlap, 1);
        for (int i = 0; i < len; i += step) {
            int end = Math.min(i + size, len);
            result.add(norm.substring(i, end));
            if (result.size() >= maxChunks || end == len) break;
        }
        return result;
    }

    private void updateFileStatus(Long fileId, String status, int chunkCount, String errorMessage) {
        KnowledgeBaseFile update = new KnowledgeBaseFile();
        update.setId(fileId);
        update.setStatus(status);
        update.setChunkCount(chunkCount);
        update.setErrorMessage(errorMessage);
        update.setUpdateTime(LocalDateTime.now());
        knowledgeBaseFileMapper.updateById(update);
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

    private String toJsonArrayString(float[] vector) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float v : vector) sj.add(Float.toString(v));
        return sj.toString();
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
