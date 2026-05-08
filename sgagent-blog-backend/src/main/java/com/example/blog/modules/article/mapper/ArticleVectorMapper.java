package com.example.blog.modules.article.mapper;

import com.example.blog.modules.article.model.entity.ArticleVector;
import org.apache.ibatis.annotations.*;

/**
 * 文章向量表 Mapper
 * 通过 STRING_TO_VECTOR / VECTOR_TO_STRING 在字符串与 VECTOR 列之间转换
 */
@Mapper
public interface ArticleVectorMapper {

    /**
     * 写入或更新文章向量（基于 article_id 唯一键 upsert）
     */
    @Insert("""
            INSERT INTO article_vector (article_id, embedding, dimension, model_name, source_hash, create_time, update_time)
            VALUES (#{articleId}, STRING_TO_VECTOR(#{embeddingStr}), #{dimension}, #{modelName}, #{sourceHash}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                embedding   = STRING_TO_VECTOR(#{embeddingStr}),
                dimension   = #{dimension},
                model_name  = #{modelName},
                source_hash = #{sourceHash},
                update_time = NOW()
            """)
    int upsert(ArticleVector vector);

    /**
     * 查询某文章当前已存的源文本哈希，用于跳过未变化的重复向量化
     */
    @Select("SELECT source_hash FROM article_vector WHERE article_id = #{articleId}")
    String findSourceHashByArticleId(@Param("articleId") Long articleId);

    /**
     * 删除文章对应的向量记录（文章被物理删除时调用）
     */
    @Delete("DELETE FROM article_vector WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
