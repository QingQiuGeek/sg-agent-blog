package com.example.blog.modules.article.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章向量实体
 * 注意：embedding 列在 DB 是 VECTOR 类型，无法被 MyBatis-Plus BaseMapper 直接处理。
 * 因此本表使用自定义 Mapper（手写 SQL + STRING_TO_VECTOR / VECTOR_TO_STRING）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleVector implements Serializable {

    private Long id;

    private Long articleId;

    /**
     * 向量的 JSON 字符串表示（"[0.1, 0.2, ...]"）。
     * 写入时由 Mapper 通过 STRING_TO_VECTOR(#{embeddingStr}) 转为 VECTOR；
     * 读取时由 Mapper 通过 VECTOR_TO_STRING(embedding) 转回字符串。
     */
    private String embeddingStr;

    private Integer dimension;

    private String modelName;

    /** 源文本 SHA-256 哈希，用于判断文章内容是否变化以决定是否需要重新向量化 */
    private String sourceHash;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
