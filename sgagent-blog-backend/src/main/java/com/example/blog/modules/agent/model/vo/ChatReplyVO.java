package com.example.blog.modules.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 回复结果")
public class ChatReplyVO {

    @Schema(description = "会话ID（首次发送时自动新建后回传）")
    private String sessionId;

    @Schema(description = "用户消息ID")
    private String userMessageId;

    @Schema(description = "AI 消息ID")
    private String assistantMessageId;

    @Schema(description = "AI 回复内容")
    private String content;

    @Schema(description = "本次回答引用到的站内文章列表（按相似度去重后的精简信息）")
    private List<ArticleSourceVO> sources;

    @Schema(description = "本次回答中 AI 调用过的工具列表（按调用顺序）")
    private List<ToolInvocationVO> toolCalls;
}
