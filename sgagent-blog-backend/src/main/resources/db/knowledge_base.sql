-- ============================================================
-- 知识库相关表（个人中心「我的知识库」+ AI 知识库检索 tool）
-- 三张表：
--   knowledge_base       —— 知识库元信息（用户私有）
--   knowledge_base_file  —— 知识库下的文件元信息（OSS 存储）
--   document_vector      —— 文件切片后的向量，每 chunk 一行
-- 设计说明：
--   1) 切片策略：每文件切多 chunk（默认 800 字 / 200 字重叠），
--      每 chunk 单独做 embedding 并落库，检索时取 top-K 个 chunk。
--   2) 严格私有：knowledge_base.user_id 决定可见范围，
--      AI tool 检索时强制按当前请求勾选的 kb_id 过滤。
--   3) 状态字段：knowledge_base_file.status 反映向量化进度，
--      前端可展示「向量化中 / 已就绪 / 失败」。
-- ============================================================

DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `user_id`      BIGINT       NOT NULL COMMENT '所有者用户ID',
    `name`         VARCHAR(64)  NOT NULL COMMENT '知识库名称',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '知识库描述',
    `file_count`   INT          NOT NULL DEFAULT 0 COMMENT '文件数量（冗余）',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `delete_time`  DATETIME              DEFAULT NULL COMMENT '逻辑删除时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库元信息';


DROP TABLE IF EXISTS `knowledge_base_file`;
CREATE TABLE `knowledge_base_file` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `kb_id`         BIGINT       NOT NULL COMMENT '所属知识库ID',
    `user_id`       BIGINT       NOT NULL COMMENT '上传者用户ID（冗余便于鉴权）',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_url`      VARCHAR(500) NOT NULL COMMENT 'OSS 存储 URL',
    `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件字节数',
    `content_type`  VARCHAR(100)          DEFAULT NULL COMMENT 'MIME 类型',
    `ext`           VARCHAR(20)           DEFAULT NULL COMMENT '扩展名（小写，无点）',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '向量化状态：PENDING / SUCCESS / FAILED',
    `chunk_count`   INT          NOT NULL DEFAULT 0 COMMENT '已生成的向量 chunk 数',
    `error_message` VARCHAR(500)          DEFAULT NULL COMMENT '失败原因（仅 status=FAILED 时有效）',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    `delete_time`   DATETIME              DEFAULT NULL COMMENT '逻辑删除时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文件元信息';


DROP TABLE IF EXISTS `document_vector`;
CREATE TABLE `document_vector` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id`         BIGINT       NOT NULL COMMENT '所属知识库ID（冗余便于检索过滤）',
    `file_id`       BIGINT       NOT NULL COMMENT '所属文件ID',
    `chunk_index`   INT          NOT NULL COMMENT 'chunk 序号（从 0 开始）',
    `chunk_text`    MEDIUMTEXT   NOT NULL COMMENT 'chunk 原文，用于 RAG 上下文回填',
    `embedding`     VECTOR(1024) NOT NULL COMMENT '向量值，由 STRING_TO_VECTOR 写入',
    `dimension`     INT          NOT NULL DEFAULT 1024 COMMENT '向量维度',
    `model_name`    VARCHAR(64)  NOT NULL COMMENT '生成向量所用的模型名',
    `source_hash`   CHAR(64)     NOT NULL COMMENT 'chunk 文本的 SHA-256，用于增量去重',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档向量表（每 chunk 一行）';
