package com.example.blog.modules.knowledge.service;

import com.example.blog.modules.knowledge.model.vo.KbHitVO;

import java.util.List;

/**
 * 知识库向量化与语义检索服务
 * <ul>
 *   <li>{@link #embedFileAsync}：对单个文件做切片 + embedding + 入库（异步），并维护文件 status</li>
 *   <li>{@link #deleteByFileId}：物理删除某文件的全部 chunk 向量</li>
 *   <li>{@link #deleteByKbId}：物理删除某知识库的全部 chunk 向量</li>
 *   <li>{@link #searchByQuery}：基于自然语言查询，从指定 kb 范围检索 top-K chunk</li>
 * </ul>
 */
public interface KnowledgeBaseVectorService {

    /**
     * 异步对单个文件做向量化：切片 + embedding + 入库 + 更新文件 status / chunk_count。
     * 失败不会抛出异常（仅写 status=FAILED 并记录日志），不影响主流程。
     */
    void embedFileAsync(Long fileId, String fileText);

    /**
     * 物理删除某文件的全部 chunk 向量
     */
    void deleteByFileId(Long fileId);

    /**
     * 物理删除某知识库下所有文件的 chunk 向量
     */
    void deleteByKbId(Long kbId);

    /**
     * 基于自然语言查询，在指定知识库 ID 集合范围内检索 top-K chunk。
     * @param query 查询文本
     * @param kbIds 知识库ID集合（必须非空，由调用方保证已做权限过滤）
     * @param topK  返回前 K 条，建议 1-10
     */
    List<KbHitVO> searchByQuery(String query, List<Long> kbIds, int topK);
}
