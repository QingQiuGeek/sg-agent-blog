package com.sg.blog.modules.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 回复中的「来源引用」单条记录（精简版，仅用于前端卡片展示）
 * <p>
 * 兼容两种来源：
 * <ul>
 *   <li>{@code type="article"}：站内文章，articleId 有值，前端跳 /post/:id</li>
 *   <li>{@code type="web"}：联网搜索结果，url 有值，前端外链打开</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 回复来源引用")
public class ArticleSourceVO implements Serializable {

    @Schema(description = "来源类型：article / web")
    private String type;

    @Schema(description = "文章 ID（type=article 时有值），前端用于跳转 /post/:id")
    private Long articleId;

    @Schema(description = "外链 URL（type=web 时有值）")
    private String url;

    @Schema(description = "文章/网页标题")
    private String title;

    @Schema(description = "作者昵称（仅 article 类型有值）")
    private String author;
}
