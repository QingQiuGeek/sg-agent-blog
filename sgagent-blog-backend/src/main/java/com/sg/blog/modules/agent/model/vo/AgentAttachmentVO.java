package com.sg.blog.modules.agent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Agent 对话附件上传响应：包含 OSS URL 与 Tika 提取的纯文本预览。
 * <p>
 * 前端持有这些字段，发送消息时把整个对象塞进 ChatRequestDTO.attachments 一并送给后端，
 * 后端不持久化文件提取内容，仅在本次 LLM 调用前临时拼接到 prompt 前。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 文件附件")
public class AgentAttachmentVO implements Serializable {

    @Schema(description = "OSS 完整访问 URL")
    private String url;

    @Schema(description = "原始文件名（含扩展名）")
    private String name;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "扩展名（小写，不含点）")
    private String ext;

    @Schema(description = "Tika 提取的纯文本（已截断），可能为空")
    private String content;
}
