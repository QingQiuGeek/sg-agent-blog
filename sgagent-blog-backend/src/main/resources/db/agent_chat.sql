-- ============================================================
-- AI 对话与知识库相关表（MySQL 9）
-- 当前阶段仅启用 chat_session / chat_message
-- knowledge_base / document / document_vector 为后续 RAG 预留
-- ============================================================

-- ---------------------- 1. 会话表 ----------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id`          VARCHAR(50) NOT NULL COMMENT '会话ID（UUID）',
    `user_id`     BIGINT      NOT NULL COMMENT '所属用户ID',
    `title`       VARCHAR(255) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `delete_time` DATETIME    NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_update` (`user_id`, `update_time` DESC, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话会话表';

-- ---------------------- 2. 会话消息表 ----------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`          VARCHAR(50)  NOT NULL COMMENT '消息ID（UUID）',
    `session_id`  VARCHAR(50)  NOT NULL COMMENT '所属会话ID',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID（冗余便于隔离查询）',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色：user / assistant / system',
    `content`     MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    `token_count` INT          NULL COMMENT '本条消息估算token',
    `sources_json`    JSON     NULL COMMENT '来源引用 JSON 数组（仅 assistant 消息可能有值）',
    `tool_calls_json` JSON     NULL COMMENT 'AI 工具调用 JSON 数组（仅 assistant 消息可能有值）',
    `attachments_json` JSON    NULL COMMENT '用户上传附件元信息 JSON 数组（仅 user 消息可能有值，不含提取文本）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_create` (`session_id`, `create_time`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息表';

-- 增量迁移（已有数据时使用）：补 sources_json / tool_calls_json 列
-- ALTER TABLE `chat_message` ADD COLUMN `sources_json`    JSON NULL COMMENT '来源引用 JSON 数组'   AFTER `token_count`;
-- ALTER TABLE `chat_message` ADD COLUMN `tool_calls_json` JSON NULL COMMENT 'AI 工具调用 JSON 数组' AFTER `sources_json`;
-- ALTER TABLE `chat_message` ADD COLUMN `attachments_json` JSON NULL COMMENT '用户上传附件元信息' AFTER `tool_calls_json`;


-- ---------------------- 3. 知识库表（预留） ----------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id`          BIGINT       NOT NULL COMMENT '知识库ID',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) NULL COMMENT '描述',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0,
    `delete_time` DATETIME     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_update` (`user_id`, `update_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表（RAG 预留）';

-- ---------------------- 4. 文档表（预留） ----------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
    `id`          BIGINT       NOT NULL COMMENT '文档ID',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID',
    `kb_id`       BIGINT       NOT NULL COMMENT '所属知识库ID',
    `filename`    VARCHAR(255) NOT NULL COMMENT '文件名',
    `filetype`    VARCHAR(32)  NULL COMMENT 'pdf/md/txt',
    `size`        BIGINT       NULL COMMENT '文件字节数',
    `oss_url`     VARCHAR(500) NULL COMMENT '原始文件存储URL',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0,
    `delete_time` DATETIME     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_kb` (`kb_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表（RAG 预留）';

-- ---------------------- 5. 文档向量分片表（预留） ----------------------
DROP TABLE IF EXISTS `document_vector`;
CREATE TABLE `document_vector` (
    `id`           BIGINT       NOT NULL COMMENT '分片ID',
    `kb_id`        BIGINT       NOT NULL COMMENT '所属知识库ID',
    `doc_id`       BIGINT       NOT NULL COMMENT '所属文档ID',
    `chunk_index`  INT          NOT NULL DEFAULT 0 COMMENT '分片序号',
    `content`      TEXT         NOT NULL COMMENT '切片文本',
    `embedding`    VECTOR(1024) NOT NULL COMMENT '向量值',
    `dimension`    INT          NOT NULL DEFAULT 1024,
    `model_name`   VARCHAR(64)  NOT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_doc_chunk` (`doc_id`, `chunk_index`),
    KEY `idx_kb` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档向量分片表（RAG 预留）';
