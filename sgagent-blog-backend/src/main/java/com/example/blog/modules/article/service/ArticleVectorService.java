package com.example.blog.modules.article.service;

/**
 * 文章向量化服务：负责调用 EmbeddingModel 把文章正文转成向量并落库
 * 当前阶段只做"写入"，向量检索功能后续单独实现
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
}
