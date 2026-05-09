package com.example.blog.modules.article.mapper;

import com.example.blog.modules.article.model.entity.ArticleVector;
import com.example.blog.modules.article.model.vo.ArticleHitVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

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

    /**
     * 拉取候选文章向量集，余弦距离在 Java 端计算（MySQL 9.x 社区版无 DISTANCE 函数）。
     * <p>支持按分类名 / 标签名 / 作者昵称做「元数据预过滤」，相当于「Filter-then-Score」式
     * hybrid retrieval：显著缩小候选集，避免无关分类/作者/标签的文章混入语义召回结果。
     * <p>所有过滤参数都是可选的；同时给出多个时按 AND 逻辑。
     * <p>数据量上来后可换成 pgvector / Milvus，或升级到 HeatWave 启用向量索引。
     *
     * @param categoryName 精确匹配分类名（如「Spring Boot」），空则不过滤
     * @param tagNames     标签名列表（任一命中即可），空则不过滤
     * @param authorName   精确匹配作者昵称，空则不过滤
     */
    @Select("""
            <script>
            SELECT a.id              AS articleId,
                   a.title           AS title,
                   a.summary         AS summary,
                   a.cover           AS cover,
                   a.view_count      AS viewCount,
                   a.like_count      AS likeCount,
                   a.favorite_count  AS favoriteCount,
                   a.create_time     AS publishTime,
                   a.user_id         AS authorId,
                   u.nickname        AS authorNickname,
                   VECTOR_TO_STRING(av.embedding) AS embeddingStr
            FROM article_vector av
            JOIN blog_article a ON a.id = av.article_id
            LEFT JOIN sys_user u ON u.id = a.user_id
            <if test="categoryName != null and categoryName != ''">
                JOIN blog_category c ON c.id = a.category_id
            </if>
            WHERE a.is_deleted = 0
              AND a.status = 1
            <if test="categoryName != null and categoryName != ''">
              AND c.name = #{categoryName}
            </if>
            <if test="authorName != null and authorName != ''">
              AND u.nickname = #{authorName}
            </if>
            <if test="tagNames != null and tagNames.size() > 0">
              AND EXISTS (
                SELECT 1 FROM blog_article_tag at
                JOIN blog_tag t ON t.id = at.tag_id
                WHERE at.article_id = a.id
                  AND t.name IN
                  <foreach collection="tagNames" item="tn" open="(" separator="," close=")">#{tn}</foreach>
              )
            </if>
            </script>
            """)
    List<Map<String, Object>> listAllForSearch(@Param("categoryName") String categoryName,
                                               @Param("tagNames") List<String> tagNames,
                                               @Param("authorName") String authorName);
}
