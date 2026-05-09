package com.example.blog.modules.article.service;

import com.example.blog.modules.article.model.vo.ArticleHitVO;

import java.util.List;

/**
 * 文章向量化服务：负责调用 EmbeddingModel 把文章正文转成向量并落库，
 * 同时提供基于余弦距离的语义检索能力，供 AI Agent Tool 调用。
 */
public interface ArticleVectorService {

    /**
     * 异步对文章进行向量化并 upsert 到 article_vector 表
     * 失败不会抛出异常（仅记录日志），不影响主流程
     */
    void embedAndSaveAsync(Long articleId, String title, String summary, String content);

    /**
     * 删除文章对应的向量记录
     */
    void deleteByArticleId(Long articleId);

    /**
     * 基于自然语言查询做语义检索，返回 top-K 已发布文章及其元信息
     *
     * @param query 用户查询文本（自然语言）
     * @param topK  返回前 K 条，建议 1-10
     */
    List<ArticleHitVO> searchByQuery(String query, int topK);
}
