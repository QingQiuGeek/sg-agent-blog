package com.example.blog.modules.knowledge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库检索命中结果（一条 = 一个 chunk）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbHitVO {

    private Long kbId;

    private String kbName;

    private Long fileId;

    private String fileName;

    private Integer chunkIndex;

    /** 命中的原文 chunk 文本（喂给 LLM 当上下文） */
    private String chunkText;

    /** 余弦相似度分数 [-1,1]，越大越相似 */
    private Double score;
}
