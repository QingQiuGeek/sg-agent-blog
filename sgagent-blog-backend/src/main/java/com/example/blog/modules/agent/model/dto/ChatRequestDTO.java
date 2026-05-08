package com.example.blog.modules.agent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 聊天请求")
public class ChatRequestDTO {

    @Schema(description = "会话ID（为空则自动新建会话）")
    private String sessionId;

    @Schema(description = "用户输入内容", example = "介绍一下你自己")
    @NotBlank(message = "内容不能为空")
    @Size(max = 4000, message = "内容长度不能超过 4000")
    private String content;
}
