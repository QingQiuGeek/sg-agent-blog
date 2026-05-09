package com.example.blog.modules.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.modules.agent.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 按用户聚合 token 用量（按 role 分组）
     * <p>返回形如 [{role: "user", total: 1234}, {role: "assistant", total: 9876}]
     */
    @Select("""
            SELECT role                              AS role,
                   COALESCE(SUM(token_count), 0)     AS total
            FROM chat_message
            WHERE user_id = #{userId}
              AND is_deleted = 0
            GROUP BY role
            """)
    List<Map<String, Object>> sumTokensByUserGroupByRole(@Param("userId") Long userId);

    /**
     * 按用户 + 时间范围聚合每日 token 用量（按 role 分组），用于近 N 天曲线图
     * <p>返回形如 [{d: "2024-05-03", role: "user", total: 123}, ...]
     */
    @Select("""
            SELECT DATE(create_time)                 AS d,
                   role                              AS role,
                   COALESCE(SUM(token_count), 0)     AS total
            FROM chat_message
            WHERE user_id = #{userId}
              AND is_deleted = 0
              AND create_time >= #{startTime}
            GROUP BY DATE(create_time), role
            ORDER BY DATE(create_time) ASC
            """)
    List<Map<String, Object>> sumTokensByUserDailyGroupByRole(@Param("userId") Long userId,
                                                              @Param("startTime") LocalDateTime startTime);

    /**
     * 全站 token 用量聚合（按 role 分组），用于管理后台仪表盘
     */
    @Select("""
            SELECT role                              AS role,
                   COALESCE(SUM(token_count), 0)     AS total
            FROM chat_message
            WHERE is_deleted = 0
            GROUP BY role
            """)
    List<Map<String, Object>> sumAllTokensGroupByRole();

    /**
     * 全站按日 + role 分组聚合，用于管理后台仪表盘近 N 天曲线
     */
    @Select("""
            SELECT DATE(create_time)                 AS d,
                   role                              AS role,
                   COALESCE(SUM(token_count), 0)     AS total
            FROM chat_message
            WHERE is_deleted = 0
              AND create_time >= #{startTime}
            GROUP BY DATE(create_time), role
            ORDER BY DATE(create_time) ASC
            """)
    List<Map<String, Object>> sumAllTokensDailyGroupByRole(@Param("startTime") LocalDateTime startTime);
}
