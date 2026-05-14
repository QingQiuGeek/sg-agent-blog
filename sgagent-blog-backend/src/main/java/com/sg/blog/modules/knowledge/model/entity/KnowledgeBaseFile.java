package com.sg.blog.modules.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sg.blog.common.base.BaseLogicEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 知识库文件元信息（实际文件落 OSS 的 kb 目录）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base_file")
public class KnowledgeBaseFile extends BaseLogicEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // id 来自父类 BaseEntity，带 @TableId(ASSIGN_ID)，勿在这里重复声明字段避免子类字段屏蔽父类注解

    /** 所属知识库ID */
    private Long kbId;

    /** 上传者用户ID（冗余便于鉴权 / 列表反查） */
    private Long userId;

    /** 原始文件名 */
    private String fileName;

    /** OSS 存储 URL */
    private String fileUrl;

    /** 文件字节数 */
    private Long fileSize;

    /** MIME 类型 */
    private String contentType;

    /** 扩展名（小写，无点） */
    private String ext;

    /** 向量化状态：PENDING / SUCCESS / FAILED */
    private String status;

    /** 已生成的向量 chunk 数 */
    private Integer chunkCount;

    /** 失败原因 */
    private String errorMessage;
}
