package com.sg.blog.modules.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改知识库请求")
public class KnowledgeBaseUpdateDTO {

    @Schema(description = "知识库ID")
    @NotNull(message = "知识库ID不能为空")
    @Positive(message = "知识库ID必须为正数")
    private Long id;

    @Schema(description = "知识库名称")
    @Size(max = 64, message = "知识库名称不能超过 64 个字符")
    private String name;

    @Schema(description = "知识库描述")
    @Size(max = 255, message = "描述不能超过 255 个字符")
    private String description;
}
