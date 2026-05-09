package com.example.blog.modules.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.blog.common.base.BaseLogicEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 知识库元信息（用户私有）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseLogicEntity {

    // id 来自父类 BaseEntity，带 @TableId(ASSIGN_ID)，勿在这里重复声明字段避免子类字段屏蔽父类注解

    /** 所有者用户ID */
    private Long userId;

    /** 知识库名称 */
    private String name;

    /** 知识库描述 */
    private String description;

    /** 文件数量（冗余） */
    private Integer fileCount;
}
