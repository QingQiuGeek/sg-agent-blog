package com.example.blog.modules.knowledge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文件向量实体（每 chunk 一行）。
 * <p>
 * embedding 列在 DB 是 VECTOR(1024) 类型，无法被 MyBatis-Plus BaseMapper 直接处理，
 * 因此本表用自定义 Mapper（手写 SQL + STRING_TO_VECTOR / VECTOR_TO_STRING）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVector implements Serializable {

    private Long id;

    /** 所属知识库ID（冗余便于检索过滤） */
    private Long kbId;

    /** 所属文件ID */
    private Long fileId;

    /** chunk 序号（从 0 开始） */
    private Integer chunkIndex;

    /** chunk 原文（用于 RAG 检索后回填上下文） */
    private String chunkText;

    /** 向量的 JSON 字符串表示，写入时通过 STRING_TO_VECTOR 转换为 VECTOR */
    private String embeddingStr;

    private Integer dimension;

    private String modelName;

    /** chunk 文本的 SHA-256 哈希 */
    private String sourceHash;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
