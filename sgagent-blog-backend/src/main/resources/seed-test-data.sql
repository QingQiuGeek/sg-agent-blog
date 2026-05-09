-- =====================================================================
-- 测试数据种子脚本：15 用户 + 10 分类 + 10 标签 + 20 文章 + 标签关联
-- ID 区间：用户 1001-1015 / 分类 2001-2010 / 标签 3001-3010 / 文章 4001-4020
-- 关联表 blog_article_tag id 区间：5001-5060
-- 密码统一为 "123456" 的 BCrypt 哈希（与项目登录加密一致）
-- 重复执行安全：先按 id 区间清理再插入
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
use `sg-agent-blog`;
-- ---------- 清理（仅清测试 ID 区间，不影响真实数据） ----------
# DELETE FROM `blog_article_tag` WHERE `id` BETWEEN 5001 AND 5099;
# DELETE FROM `blog_article`     WHERE `id` BETWEEN 4001 AND 4020;
# DELETE FROM `blog_tag`         WHERE `id` BETWEEN 3001 AND 3010;
# DELETE FROM `blog_category`    WHERE `id` BETWEEN 2001 AND 2010;
# DELETE FROM `sys_user`         WHERE `id` BETWEEN 1001 AND 1015;

-- ---------- 1. 15 个用户（密码均为 123456） ----------
INSERT INTO `sys_user` (`id`, `email`, `password`, `nickname`, `bio`, `role`, `status`, `is_deleted`, `create_time`) VALUES
(1001, 'alice@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '爱丽丝',     '前端工程师，热爱开源',         'USER', 0, 0, '2024-01-05 10:00:00'),
(1002, 'bob@test.com',     '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '鲍勃',       '后端老兵，Java/Go',            'USER', 0, 0, '2024-01-08 11:00:00'),
(1003, 'charlie@test.com', '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '查理',       'AI 算法工程师',                'USER', 0, 0, '2024-01-12 09:30:00'),
(1004, 'diana@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '黛安娜',     '产品经理 / 写点技术博客',      'USER', 0, 0, '2024-01-18 14:20:00'),
(1005, 'eric@test.com',    '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '艾瑞克',     '运维 + DevOps',                'USER', 0, 0, '2024-02-02 08:15:00'),
(1006, 'fiona@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '菲奥娜',     '全栈，TypeScript 爱好者',      'USER', 0, 0, '2024-02-10 16:00:00'),
(1007, 'george@test.com',  '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '乔治',       '区块链 / Web3 探索者',         'USER', 0, 0, '2024-02-22 13:45:00'),
(1008, 'helen@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '海伦',       '数据库 DBA',                   'USER', 0, 0, '2024-03-01 10:10:00'),
(1009, 'ivan@test.com',    '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '伊万',       '游戏开发，Unity / UE',         'USER', 0, 0, '2024-03-12 17:30:00'),
(1010, 'julia@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '茱莉亚',     'UI/UX 设计师 + 前端',          'USER', 0, 0, '2024-03-25 09:00:00'),
(1011, 'kevin@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '凯文',       '架构师，分布式系统',           'USER', 0, 0, '2024-04-05 15:00:00'),
(1012, 'linda@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '琳达',       '机器学习 / NLP',               'USER', 0, 0, '2024-04-15 11:11:00'),
(1013, 'mike@test.com',    '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '迈克',       '安全研究 / 渗透测试',          'USER', 0, 0, '2024-04-28 12:30:00'),
(1014, 'nina@test.com',    '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '妮娜',       '移动端开发，Flutter',          'USER', 0, 0, '2024-05-08 18:00:00'),
(1015, 'oscar@test.com',   '$2a$10$7JB720yubVSZvUI0E6qB0OOvPyqyT7P6pX9pFwZlV1dF6vR5HUWai', '奥斯卡',     '编译原理爱好者',               'USER', 0, 0, '2024-05-20 20:00:00');

-- ---------- 2. 10 个分类 ----------
INSERT INTO `blog_category` (`id`, `name`, `create_time`) VALUES
(2001, '前端开发',     '2024-01-01 00:00:00'),
(2002, '后端开发',     '2024-01-01 00:00:00'),
(2003, '人工智能',     '2024-01-01 00:00:00'),
(2004, '数据库',       '2024-01-01 00:00:00'),
(2005, '运维与部署',   '2024-01-01 00:00:00'),
(2006, '区块链',       '2024-01-01 00:00:00'),
(2007, '算法与数据结构','2024-01-01 00:00:00'),
(2008, '移动开发',     '2024-01-01 00:00:00'),
(2009, '游戏开发',     '2024-01-01 00:00:00'),
(2010, '工程实践',     '2024-01-01 00:00:00');

-- ---------- 3. 10 个标签 ----------
INSERT INTO `blog_tag` (`id`, `name`, `create_time`) VALUES
(3001, 'Java',        '2024-01-01 00:00:00'),
(3002, 'Spring Boot', '2024-01-01 00:00:00'),
(3003, 'Vue',         '2024-01-01 00:00:00'),
(3004, 'React',       '2024-01-01 00:00:00'),
(3005, 'MySQL',       '2024-01-01 00:00:00'),
(3006, 'Redis',       '2024-01-01 00:00:00'),
(3007, 'Docker',      '2024-01-01 00:00:00'),
(3008, 'Python',      '2024-01-01 00:00:00'),
(3009, 'Web3',        '2024-01-01 00:00:00'),
(3010, 'LLM',         '2024-01-01 00:00:00');

-- ---------- 4. 20 篇文章（status=1 已发布） ----------
INSERT INTO `blog_article` (`id`, `title`, `summary`, `content`, `view_count`, `like_count`, `favorite_count`, `is_top`, `is_carousel`, `status`, `category_id`, `user_id`, `create_time`) VALUES
(4001, 'Vue 3 组合式 API 入门指南',           'Composition API 与 Options API 的对比，setup 写法与响应式核心',                  '# Vue 3 组合式 API\n\nVue 3 推出 Composition API，以 `setup` 为核心。本文从 `ref`、`reactive` 讲到 `computed` 与 `watch`，并结合实战示例。', 1521,  86, 23, 1, 0, 1, 2001, 1001, '2024-02-01 10:00:00'),
(4002, 'React Hooks 最佳实践 10 条',           '深入 useEffect 依赖项陷阱、useMemo/useCallback 的真实价值',                       '# React Hooks 最佳实践\n\n## 1. useEffect 依赖完整性\n## 2. 闭包陷阱\n## 3. 自定义 Hook 抽象\n...',  2103, 152, 47, 0, 1, 1, 2001, 1006, '2024-02-15 14:30:00'),
(4003, 'Spring Boot 3 接入 Langchain4j',       '基于 ChatModel + Tools 的 ReAct 实现，附完整可运行代码',                          '# Spring Boot 3 + Langchain4j\n\nLangchain4j 提供 `ChatModel` 抽象与 Tool 注解。本文展示如何在 Spring Boot 3 中实现一个支持工具调用的 AI 助手。', 3450, 280, 95, 1, 1, 1, 2003, 1002, '2024-03-05 09:00:00'),
(4004, 'MySQL 索引底层原理与 explain 实战',    'B+ 树、覆盖索引、最左前缀、回表，以及如何读懂 explain',                           '# MySQL 索引\n\n## B+ 树为什么是 B+ 树\n## explain 关键列：type / key / Extra\n## 实战：从慢查询到秒查询', 4280, 312, 128, 0, 0, 1, 2004, 1008, '2024-03-12 16:00:00'),
(4005, 'Redis 7 新特性盘点：Functions / ACL', 'Functions 取代 EVAL，ACL 细粒度权限，Stream 持续演进',                            '# Redis 7\n\n- Functions：替代 EVAL 的脚本方案\n- ACL v2：基于 selector 的精细控制\n- Sharded Pub/Sub', 1890,  98, 31, 0, 0, 1, 2004, 1008, '2024-03-20 11:30:00'),
(4006, 'Docker 多阶段构建瘦身镜像',           '从 1.2GB 到 80MB，多阶段 + distroless 实战',                                       '# Docker 多阶段构建\n\n## 第一阶段：build\n## 第二阶段：runtime（distroless）\n## 体积对比与启动速度', 2310, 175, 56, 0, 0, 1, 2005, 1005, '2024-04-02 13:00:00'),
(4007, 'Kubernetes Pod 调度策略详解',         'NodeSelector / Affinity / Taints 与 Tolerations，Topology Spread',               '# K8s 调度\n\n## NodeSelector\n## Affinity / Anti-Affinity\n## Taints & Tolerations\n## TopologySpreadConstraints', 1654,  77, 19, 0, 0, 1, 2005, 1005, '2024-04-10 17:45:00'),
(4008, '从零实现一个 LLM Agent',               'ReAct 循环、Tool Calling、上下文管理三件套，附 Java 代码',                        '# LLM Agent 实战\n\nAgent = LLM + Tools + Memory + Loop。本文从最小可运行例子起步，逐步加入工具与记忆。', 5120, 421, 168, 1, 1, 1, 2003, 1003, '2024-04-22 10:30:00'),
(4009, 'Transformer 架构图解（2024 版）',     '从 Attention is All You Need 到 GPT-4 / Llama，演进图谱',                          '# Transformer 图解\n\n## Self-Attention\n## Multi-Head\n## Position Encoding\n## 演进：Decoder-only 时代', 3870, 295, 102, 1, 0, 1, 2003, 1012, '2024-05-01 09:15:00'),
(4010, 'Web3 钱包签名机制：EIP-712 详解',      'Typed Data 签名、防钓鱼、Permit 与 Permit2',                                       '# EIP-712\n\n相比 personal_sign，EIP-712 让用户能看到结构化数据。本文讲解原理与前端集成。',                  1420,  92, 28, 0, 0, 1, 2006, 1007, '2024-05-08 15:30:00'),
(4011, '智能合约升级模式：Proxy & UUPS',       'Transparent Proxy / UUPS / Beacon 三种主流升级方案对比',                          '# 合约升级\n\n## Transparent Proxy\n## UUPS（推荐）\n## Beacon Proxy\n## 风险与最佳实践',                   1180,  73, 22, 0, 0, 1, 2006, 1007, '2024-05-15 18:00:00'),
(4012, '动态规划经典 20 题精讲',                '从最长公共子序列到背包问题，附 Java 解法',                                          '# DP 20 题\n\n本文整理 LeetCode 上经典 DP 题，按子序列、背包、状态机、区间四类讲解。',                          2790, 198, 84, 0, 0, 1, 2007, 1011, '2024-05-22 12:00:00'),
(4013, '红黑树 vs AVL：图解平衡因子',           '插入/删除旋转的真实差别，以及为什么 Java HashMap 选红黑树',                       '# 红黑树 vs AVL\n\n两者都 O(log n)，差别在「平衡严格度」。AVL 查更快，红黑树写更快。',                          1340,  88, 25, 0, 0, 1, 2007, 1011, '2024-05-28 19:30:00'),
(4014, 'Flutter 状态管理三巨头横评',           'Provider / Riverpod / Bloc 怎么选',                                                '# Flutter 状态管理\n\n## Provider：简单够用\n## Riverpod：编译期安全\n## Bloc：事件驱动',                      1620, 110, 33, 0, 0, 1, 2008, 1014, '2024-06-04 10:45:00'),
(4015, 'Unity DOTS 入门：ECS 思想',             '从 GameObject 到 ECS，性能十倍提升的秘密',                                          '# Unity DOTS\n\n## Entity / Component / System\n## Burst Compiler\n## Job System',                            980,  61, 14, 0, 0, 1, 2009, 1009, '2024-06-12 14:00:00'),
(4016, 'Git 高级技巧：rebase / cherry-pick / reflog', '从此告别 reset --hard 后的恐慌',                                              '# Git 高级技巧\n\n## interactive rebase\n## cherry-pick 跨分支移植\n## reflog 救命', 2650, 230, 88, 0, 1, 1, 2010, 1004, '2024-06-20 11:00:00'),
(4017, 'CI/CD 流水线设计：从 GitHub Actions 到 ArgoCD', '蓝绿部署 / 金丝雀 / GitOps 三件套实战',                                       '# CI/CD 实战\n\n## GitHub Actions：CI\n## ArgoCD：CD with GitOps\n## 金丝雀发布', 1880, 142, 49, 0, 0, 1, 2010, 1005, '2024-06-28 16:30:00'),
(4018, 'TypeScript 类型体操精讲',                'infer / 条件类型 / 模板字面量类型，写出库级类型',                                  '# TS 类型体操\n\n本文从 `infer`、条件类型、模板字面量类型三个核心讲起，最后实现一个简易 ORM 类型层。',          2140, 165, 58, 0, 0, 1, 2001, 1010, '2024-07-05 09:30:00'),
(4019, 'Python 异步编程：asyncio 深入',          '事件循环、协程、Task、Future，避坑指南',                                            '# asyncio 深入\n\n## 事件循环\n## coroutine vs Task\n## 常见坑：阻塞调用、异常吞没',                            1750, 118, 38, 0, 0, 1, 2002, 1003, '2024-07-12 13:20:00'),
(4020, 'OAuth2 与 OIDC 终极指南',                'Authorization Code with PKCE、隐式流的废弃、ID Token 验签',                       '# OAuth2 / OIDC\n\n## 四种 Grant\n## PKCE 必选\n## OIDC 加什么\n## 实战：Spring Authorization Server',         2980, 215, 76, 0, 1, 1, 2002, 1013, '2024-07-20 17:00:00');

-- ---------- 5. 文章-标签关联（每篇 2-3 个标签） ----------
INSERT INTO `blog_article_tag` (`id`, `article_id`, `tag_id`, `create_time`) VALUES
-- 4001 Vue 入门 -> Vue
(5001, 4001, 3003, '2024-02-01 10:00:00'),
-- 4002 React Hooks -> React
(5002, 4002, 3004, '2024-02-15 14:30:00'),
-- 4003 Spring Boot + Langchain4j -> Java, Spring Boot, LLM
(5003, 4003, 3001, '2024-03-05 09:00:00'),
(5004, 4003, 3002, '2024-03-05 09:00:00'),
(5005, 4003, 3010, '2024-03-05 09:00:00'),
-- 4004 MySQL 索引 -> MySQL
(5006, 4004, 3005, '2024-03-12 16:00:00'),
-- 4005 Redis 7 -> Redis
(5007, 4005, 3006, '2024-03-20 11:30:00'),
-- 4006 Docker 多阶段 -> Docker
(5008, 4006, 3007, '2024-04-02 13:00:00'),
-- 4007 K8s 调度 -> Docker
(5009, 4007, 3007, '2024-04-10 17:45:00'),
-- 4008 LLM Agent -> Java, LLM
(5010, 4008, 3001, '2024-04-22 10:30:00'),
(5011, 4008, 3010, '2024-04-22 10:30:00'),
-- 4009 Transformer -> Python, LLM
(5012, 4009, 3008, '2024-05-01 09:15:00'),
(5013, 4009, 3010, '2024-05-01 09:15:00'),
-- 4010 EIP-712 -> Web3
(5014, 4010, 3009, '2024-05-08 15:30:00'),
-- 4011 合约升级 -> Web3
(5015, 4011, 3009, '2024-05-15 18:00:00'),
-- 4012 DP 20 题 -> Java
(5016, 4012, 3001, '2024-05-22 12:00:00'),
-- 4013 红黑树 -> Java
(5017, 4013, 3001, '2024-05-28 19:30:00'),
-- 4014 Flutter（无适配标签，仅占位一个 React 表示前端栈作对照）—— 改为不打标签更真实，下面留空跳过
-- 4015 Unity DOTS（无对应技术栈标签，跳过）
-- 4016 Git 技巧 -> Docker（关联弱，跳过）
-- 4017 CI/CD -> Docker
(5018, 4017, 3007, '2024-06-28 16:30:00'),
-- 4018 TS 类型体操 -> React, Vue（前端通用，给两个）
(5019, 4018, 3003, '2024-07-05 09:30:00'),
(5020, 4018, 3004, '2024-07-05 09:30:00'),
-- 4019 asyncio -> Python
(5021, 4019, 3008, '2024-07-12 13:20:00'),
-- 4020 OAuth2 -> Java, Spring Boot
(5022, 4020, 3001, '2024-07-20 17:00:00'),
(5023, 4020, 3002, '2024-07-20 17:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 验证查询：执行后可跑这些查看是否生效
-- =====================================================================
-- SELECT COUNT(*) AS user_cnt     FROM sys_user      WHERE id BETWEEN 1001 AND 1015;     -- 期望 15
-- SELECT COUNT(*) AS category_cnt FROM blog_category WHERE id BETWEEN 2001 AND 2010;     -- 期望 10
-- SELECT COUNT(*) AS tag_cnt      FROM blog_tag      WHERE id BETWEEN 3001 AND 3010;     -- 期望 10
-- SELECT COUNT(*) AS article_cnt  FROM blog_article  WHERE id BETWEEN 4001 AND 4020;     -- 期望 20
-- SELECT COUNT(*) AS rel_cnt      FROM blog_article_tag WHERE id BETWEEN 5001 AND 5099;  -- 期望 23

-- =====================================================================
-- 重要：执行完上面的 SQL 后，还需要清理 Redis 缓存，否则前台看不到新数据！
-- 这是因为 listPortalCategories / listPortalTags / WebmasterInfo 都缓存了 1 天。
-- 进入 redis-cli 执行：
--   DEL blog:category:list blog:tag:list blog:webmaster:info
-- =====================================================================
