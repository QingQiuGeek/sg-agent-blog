package com.sg.blog.modules.article.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章语义检索命中结果 VO
 * <p>
 * 同时承担两种用途：
 * - 给 LLM Tool 调用返回 JSON（精简字段足以让模型回答用户问题）
 * - 给前端「来源引用」卡片展示（标题、作者、阅读量等元信息 + 文章 id 用于跳转）
 */
@Data
public class ArticleHitVO implements Serializable {

    private Long articleId;

    private String title;

    private String summary;

    private String cover;

    private Long viewCount;

    private Long likeCount;

    private Long favoriteCount;

    private LocalDateTime publishTime;

    private Long authorId;

    private String authorNickname;

    /** 余弦相似度，[0,1]，越大越相似 */
    private Double score;
}
