-- ============================================================
-- 文章向量表（MySQL 9 原生 VECTOR 类型）
-- 模型：text-embedding-v3 (DashScope/百炼) 默认 1024 维
-- 用途：AI 语义检索的底层存储（先存储不查询）
-- 写入：使用 STRING_TO_VECTOR('[0.1, 0.2, ...]')
-- 读取：使用 VECTOR_TO_STRING(embedding)
-- 后续可选：建立 VECTOR INDEX 以支持 ANN 近似检索
-- ============================================================

DROP TABLE IF EXISTS `article_vector`;

CREATE TABLE `article_vector` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`   BIGINT       NOT NULL COMMENT '文章ID（关联 article.id，1:1）',
    `embedding`    VECTOR(1024) NOT NULL COMMENT '向量值（FLOAT 数组，由 STRING_TO_VECTOR 写入）',
    `dimension`    INT          NOT NULL DEFAULT 1024 COMMENT '向量维度',
    `model_name`   VARCHAR(64)  NOT NULL COMMENT '生成向量所用的模型名，例如 text-embedding-v3',
    `source_hash`  CHAR(64)     NOT NULL COMMENT '源文本(标题+摘要+正文)的 SHA-256 哈希，用于跳过未变化的文章',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_id` (`article_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章向量表';
