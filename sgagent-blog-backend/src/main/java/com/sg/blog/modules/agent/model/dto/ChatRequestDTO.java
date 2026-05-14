package com.sg.blog.modules.agent.model.dto;

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

    /**
     * 是否启用联网搜索工具（webSearch）。默认 false：
     * 仅当用户在输入框点击「联网」按钮显式开启时才会带 true，
     * 后端会据此决定是否把 webSearch 注册到本次 LLM 可调用的工具列表里。
     */
    @Schema(description = "是否启用联网搜索（webSearch 工具开关）", defaultValue = "false")
    private Boolean webSearchEnabled = false;

    /**
     * 本次消息附带的文件（上传接口返回的对象原样回传）。
     * 后端把每个 attachment 的 content（Tika 提取文本）拼接到 prompt 前，仅本次调用生效，不持久化。
     */
    @Schema(description = "附件列表（最多 5 个）")
    @jakarta.validation.constraints.Size(max = 5, message = "附件最多 5 个")
    private java.util.List<AgentAttachmentDTO> attachments;

    /**
     * 用户在输入框勾选的「我的知识库」ID 列表。仅当非空时，
     * 后端才会把 searchKnowledgeBase 工具加入 LLM 可调用列表，且把范围限定为这些 ID。
     * 是否真正调用、调几次由 LLM 决定。
     */
    @Schema(description = "勾选的知识库ID列表（开启知识库检索 tool 的开关；为空表示不开启）")
    @jakarta.validation.constraints.Size(max = 10, message = "最多勾选 10 个知识库")
    private java.util.List<Long> selectedKbIds;
}
