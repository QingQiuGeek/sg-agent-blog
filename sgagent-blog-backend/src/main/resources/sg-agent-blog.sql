/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : blog_db

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 09/03/2026 09:01:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
create database if not exists `sg-agent-blog` ;
use `sg-agent-blog` ;
-- ----------------------------
-- Table structure for blog_article
-- ----------------------------
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 封面 ',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 标题 ',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 摘要 ',
  `is_ai_summary` tinyint(1) NULL DEFAULT 0 COMMENT ' 摘要来源 0: 人工 1:AI',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Markdown 内容 ',
  `content_html` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'HTML 内容 ',
  `view_count` bigint NULL DEFAULT 0 COMMENT ' 浏览量 ',
  `like_count` bigint NULL DEFAULT 0 COMMENT ' 点赞数量 ',
  `favorite_count` bigint NOT NULL DEFAULT 0 COMMENT ' 收藏数量 ',
  `is_top` tinyint(1) NULL DEFAULT 0 COMMENT ' 置顶 0: 否 1: 是 ',
  `is_carousel` tinyint(1) NOT NULL DEFAULT 0 COMMENT ' 是否首页轮播 0: 否 1: 是 ',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT ' 状态 1: 发布 0: 草稿 ',
  `category_id` bigint NULL DEFAULT NULL COMMENT ' 分类 ID',
  `user_id` bigint NOT NULL COMMENT ' 作者 ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT ' 逻辑删除 0: 未删 1: 已删 ',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_article_favorite
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_favorite`;
CREATE TABLE `blog_article_favorite`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `article_id` bigint NOT NULL COMMENT ' 文章 ID',
  `user_id` bigint NOT NULL COMMENT ' 用户 ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 收藏时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_user`(`article_id` ASC, `user_id` ASC) USING BTREE COMMENT ' 联合唯一索引，防止重复收藏 ',
  INDEX `idx_user`(`user_id` ASC) USING BTREE COMMENT ' 用户 ID 索引，便于查询用户收藏列表 '
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章收藏表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_article_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_like`;
CREATE TABLE `blog_article_like`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `article_id` bigint NOT NULL COMMENT ' 文章 ID',
  `user_id` bigint NOT NULL COMMENT ' 用户 ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 点赞时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_user`(`article_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章点赞表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_article_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `article_id` bigint NOT NULL COMMENT ' 文章 ID',
  `tag_id` bigint NOT NULL COMMENT ' 标签 ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_tag`(`article_id` ASC, `tag_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章标签关联表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 分类名称 ',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章分类表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_comment
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 评论内容 ',
  `like_count` bigint NULL DEFAULT 0 COMMENT ' 点赞数量 ',
  `user_id` bigint NOT NULL COMMENT ' 评论用户 ID',
  `article_id` bigint NOT NULL COMMENT ' 文章 ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT ' 父评论 ID， 0 表示顶级评论 ',
  `reply_comment_id` bigint NULL DEFAULT 0 COMMENT ' 被回复的评论 ID， 0 表示直接回复父评论 ',
  `reply_user_id` bigint NULL DEFAULT 0 COMMENT ' 被回复的用户 ID，用于显示和通知 ',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT ' 逻辑删除： 0- 未删除， 1- 已删除 ',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_article_id`(`article_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_reply_comment_id`(`reply_comment_id` ASC) USING BTREE,
  INDEX `idx_reply_user_id`(`reply_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章评论表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_comment_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment_like`;
CREATE TABLE `blog_comment_like`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `comment_id` bigint NOT NULL COMMENT ' 评论 ID',
  `user_id` bigint NOT NULL COMMENT ' 用户 ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 点赞时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_comment_user`(`comment_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 评论点赞表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for blog_daily_visit
-- ----------------------------
DROP TABLE IF EXISTS `blog_daily_visit`;
CREATE TABLE `blog_daily_visit`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `date` date NOT NULL COMMENT ' 日期 ',
  `views` bigint NULL DEFAULT 0 COMMENT ' 访问次数 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_date`(`date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 每日访问统计表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_danmaku
-- ----------------------------
DROP TABLE IF EXISTS `blog_danmaku`;
CREATE TABLE `blog_danmaku`  (
  `id` bigint NOT NULL COMMENT ' 弹幕 ID',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 弹幕内容 ',
  `user_id` bigint NULL DEFAULT NULL COMMENT ' 发送者 ID( 游客为空 )',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 昵称 ( 游客自动生成 )',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 头像 ( 游客使用默认 )',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT ' 逻辑删除标识 (0 未删除 ,1 已删除 )',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 弹幕留言表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_feedback
-- ----------------------------
DROP TABLE IF EXISTS `blog_feedback`;
CREATE TABLE `blog_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ' 主键 ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT ' 反馈用户 ID ( 为空表示游客匿名反馈 )',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT ' 反馈类型 (0- 意见建议 , 1-BUG 反馈 , 2- 商务合作 , 3- 其他 )',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 反馈详细内容 ',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 附加截图 ( 存放 JSON 数组或逗号分隔的 URL)',
  `contact_email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 联系邮箱 ',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT ' 处理状态 (0- 待处理 , 1- 处理中 , 2- 已解决 , 3- 已驳回 )',
  `admin_reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 管理员回复内容 ',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT ' 逻辑删除 (0- 正常 , 1- 删除 )',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2029210026780835843 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 用户意见反馈表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_report
-- ----------------------------
DROP TABLE IF EXISTS `blog_report`;
CREATE TABLE `blog_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ' 主键 ID',
  `user_id` bigint NOT NULL COMMENT ' 举报人 ID',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 举报目标类型 (COMMENT- 评论 , ARTICLE- 文章 , USER- 用户 )',
  `target_id` bigint NOT NULL COMMENT ' 举报目标 ID ( 如具体的评论 ID)',
  `reason` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 举报原因 ( 如：垃圾广告、违法违规、人身攻击、低俗色情等 )',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 举报人补充的详细说明 ',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT ' 处理状态 (0- 待处理 , 1- 举报属实已处罚 , 2- 驳回 / 恶意举报 )',
  `admin_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 管理员内部处理备注 ',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT ' 逻辑删除 (0- 正常 , 1- 删除 )',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2029486452587286531 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 内容举报投诉表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for blog_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 标签名称 ',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 文章标签表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `id` bigint NOT NULL COMMENT ' 任务 ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT ' 任务名称 ',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT ' 任务组名 ',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 调用目标字符串 ( 例如 : taskService.runParams(\"1\"))',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'cron 执行表达式 ',
  `misfire_policy` tinyint(1) NULL DEFAULT 3 COMMENT ' 计划执行错误策略 (1: 立即执行 2: 执行一次 3: 放弃执行 )',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT ' 状态 (0: 正常 1: 暂停 )',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT ' 备注信息 ',
  `create_time` datetime NULL DEFAULT NULL COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT NULL COMMENT ' 更新时间 ',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 定时任务调度表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ' 主键 ',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 邮箱 ',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 登录 IP 地址 ',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 登录地点 ',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 浏览器类型 ',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作系统 ',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT ' 登录状态 (1 成功 0 失败 )',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 提示消息 ( 如 : 密码错误、账号锁定等 )',
  `create_time` datetime NULL DEFAULT NULL COMMENT ' 登录时间 ',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2030654501021523971 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 系统登录日志表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `to_user_id` bigint NOT NULL COMMENT ' 接收方用户 ID ( 谁收到了这条消息 )',
  `from_user_id` bigint NULL DEFAULT NULL COMMENT ' 发送方用户 ID ( 谁触发的动作，系统通知可为 NULL)',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 消息大类： SYSTEM- 系统通知 , LIKE- 点赞 , COMMENT- 评论 / 回复 ',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 消息标题 ( 例如：\" 系统升级通知 \" 或 \" 新评论提醒 \")',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 消息主体内容 ( 例如评论的具体文字，或反馈的处理结果 )',
  `biz_id` bigint NULL DEFAULT NULL COMMENT ' 关联业务 ID ( 例如：文章 ID、评论 ID、反馈单 ID)',
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 业务类型 (ARTICLE, COMMENT, FEEDBACK)',
  `target_id` bigint NULL DEFAULT NULL COMMENT ' 具体目标 ID( 如评论 ID)，用于精准跳转 ',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT ' 阅读状态： 0- 未读， 1- 已读 ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_to_user_id`(`to_user_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`to_user_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 系统消息 / 通知表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 公告内容 ',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT ' 状态 1: 显示 0: 隐藏 ',
  `is_top` tinyint(1) NULL DEFAULT 0 COMMENT ' 是否置顶 1: 是 0: 否 ',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_top`(`is_top` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 系统公告表 ' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `id` bigint NOT NULL COMMENT ' 主键 ',
  `user_id` bigint NULL DEFAULT NULL COMMENT ' 操作人 ID',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作人昵称 ',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作 IP',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 用户代理 (User-Agent)',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作模块 ',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作类型 ',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'HTTP 请求方式 (GET/POST/PUT 等 )',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 操作描述 ',
  `method` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 请求方法 ( 类名 . 方法名 )',
  `params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT ' 请求参数 ',
  `result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT ' 返回结果 / 异常信息 ',
  `cost_time` int NULL DEFAULT NULL COMMENT ' 执行耗时 (ms)',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT ' 操作状态 1 成功 0 失败 ',
  `create_time` datetime NULL DEFAULT NULL COMMENT ' 创建时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 系统操作日志表 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sys_sensitive_word`;
CREATE TABLE `sys_sensitive_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT ' 主键 ',
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 敏感词 ',
  `create_time` datetime NULL DEFAULT NULL COMMENT ' 创建时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_word`(`word` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2025513595477884930 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 敏感词库 ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL COMMENT ' 主键 ID',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 邮箱 ',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 密码 ',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 昵称 ',
  `bio` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 个人简介 / 个性签名 ',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 头像 URL',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'USER' COMMENT ' 角色 : SUPER_ADMIN/ADMIN/USER',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT ' 状态 : 0- 正常 , 1- 禁用 , 2- 注销冷静期 ',
  `disable_end_time` datetime NULL DEFAULT NULL COMMENT ' 封禁到期时间 ( 为空表示未封禁 , 极大值表示永久封禁 )',
  `disable_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ' 封禁原因 ',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT ' 创建时间 ',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ' 更新时间 ',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT ' 逻辑删除 0: 未删 1: 已删 ',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT ' 注销申请时间 ',
  `delete_time` datetime NULL DEFAULT NULL COMMENT ' 删除时间 ',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = ' 系统用户表 ' ROW_FORMAT = DYNAMIC;

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
                                `sources_json` JSON NULL COMMENT '来源引用 JSON 数组',
                                tool_calls_json JSON NULL COMMENT 'AI 工具调用 JSON 数组',
                                `attachments_json` JSON NULL COMMENT '用户上传附件元信息',
                                `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_session_create` (`session_id`, `create_time`),
                                KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息表';

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


SET FOREIGN_KEY_CHECKS = 1;
