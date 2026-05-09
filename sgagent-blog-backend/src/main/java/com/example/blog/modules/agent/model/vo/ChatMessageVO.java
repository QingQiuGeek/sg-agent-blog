package com.example.blog.modules.agent.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "AI 消息视图")
public class ChatMessageVO {

    private String id;

    /** user / assistant / system */
    private String role;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "AI 消息附带的来源引用（仅 assistant 消息可能有值）")
    private List<ArticleSourceVO> sources;

    @Schema(description = "AI 消息生成过程中的工具调用记录（仅 assistant 消息可能有值）")
    private List<ToolInvocationVO> toolCalls;
}
