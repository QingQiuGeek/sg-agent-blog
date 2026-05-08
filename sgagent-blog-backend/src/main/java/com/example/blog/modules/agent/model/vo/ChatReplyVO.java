package com.example.blog.modules.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
