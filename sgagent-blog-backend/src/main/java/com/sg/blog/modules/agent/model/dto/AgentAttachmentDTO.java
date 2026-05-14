package com.sg.blog.modules.agent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息时随附的文件附件，与 {@link com.sg.blog.modules.agent.model.vo.AgentAttachmentVO} 字段一致。
 * 由前端把上传接口返回的对象原样回传，后端把 content 拼到 prompt 前供 AI 理解。
 */
@Data
@Schema(description = "聊天消息附件")
public class AgentAttachmentDTO implements Serializable {

    @Schema(description = "OSS URL")
    private String url;

    @Schema(description = "原始文件名")
    private String name;

    @Schema(description = "文件大小（字节）")
    private Long size;

    @Schema(description = "扩展名（小写）")
    private String ext;

    @Schema(description = "Tika 提取的纯文本内容")
    private String content;
}
