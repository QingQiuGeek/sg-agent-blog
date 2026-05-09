package com.example.blog.modules.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新建知识库请求")
public class KnowledgeBaseAddDTO {

    @Schema(description = "知识库名称", example = "我的论文笔记")
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 64, message = "知识库名称不能超过 64 个字符")
    private String name;

    @Schema(description = "知识库描述", example = "存放各类论文与读书笔记")
    @Size(max = 255, message = "描述不能超过 255 个字符")
    private String description;
}
