package com.example.blog.modules.knowledge.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文件视图")
public class KnowledgeBaseFileVO {

    private Long id;

    private Long kbId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String contentType;

    private String ext;

    /** PENDING / SUCCESS / FAILED */
    private String status;

    private Integer chunkCount;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
