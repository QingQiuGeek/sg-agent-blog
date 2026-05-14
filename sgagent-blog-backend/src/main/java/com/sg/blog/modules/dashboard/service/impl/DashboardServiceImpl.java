package com.sg.blog.modules.dashboard.service.impl;

import com.sg.blog.modules.agent.mapper.ChatMessageMapper;
import com.sg.blog.modules.article.model.dto.ArticleCategoryCountDTO;
import com.sg.blog.modules.article.service.ArticleService;
import com.sg.blog.modules.dashboard.model.vo.DashboardVO;
import com.sg.blog.modules.dashboard.service.DashboardService;
import com.sg.blog.modules.monitor.model.vo.VisitTrendVO;
import com.sg.blog.modules.monitor.service.VisitService;
import com.sg.blog.modules.operation.service.CommentService;
import com.sg.blog.modules.user.model.vo.TokenUsageVO;
import com.sg.blog.modules.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private ArticleService articleService;

    @Resource
    private UserService userService;

    @Resource
    private CommentService commentService;

    @Resource
    private VisitService visitService;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    /** 近 7 天 Token 用量曲线（含今天） */
    private static final int TOKEN_USAGE_WINDOW_DAYS = 7;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @SuppressWarnings("unchecked")
    public DashboardVO getDashboardData() {
        DashboardVO vo = new DashboardVO();

        // 获取基础数据
        Long articleCount = articleService.count();
        Long userCount = userService.count();
        Long commentCount = commentService.count();
        Long visitCount = visitService.countTotalVisits();

        // 获取趋势图数据 (折线图)
        VisitTrendVO trendVO = visitService.getVisitTrendStats();

        DashboardVO.VisitTrend visitTrend = DashboardVO.VisitTrend.builder()
                .dates(trendVO.getDates())
                .pvCounts(trendVO.getPvCounts())
                .build();

        // 获取分类占比数据 (饼图)
        List<ArticleCategoryCountDTO> categoryStats = articleService.countArticleByCategoryId();

        // DTO 转换逻辑
        List<DashboardVO.CategoryPie> categoryPieList = Optional.ofNullable(categoryStats)
                .orElse(Collections.emptyList())
                .stream()
                .map(dto -> DashboardVO.CategoryPie.builder()
                        // 假设 ArticleService 已经填充了分类名称
                        // 如果没有填充，这里可能是 "未知分类" 或者 null
                        .name(dto.getCategoryName() != null ? dto.getCategoryName() : null)
                        .value(dto.getCount() != null ? dto.getCount() : 0L)
                        .build())
                .filter(pie -> pie.getValue() > 0) // 过滤掉文章数为0的（可选）
                .collect(Collectors.toList());

        // 全站 Token 用量（总量 + 近 7 天曲线）
        TokenUsageVO tokenUsage = buildAllTokenUsage();

        // 组装最终 DashboardVO
        return DashboardVO.builder()
                .articleCount(articleCount)
                .userCount(userCount)
                .commentCount(commentCount)
                .visitCount(visitCount)
                .visitTrend(visitTrend)
                .categoryPie(categoryPieList)
                .tokenUsage(tokenUsage)
                .build();
    }

    /** 汇总全站 AI Token 用量（不按 userId 过滤） */
    private TokenUsageVO buildAllTokenUsage() {
        long aiTotal = 0L;
        long userTotal = 0L;
        for (Map<String, Object> row : chatMessageMapper.sumAllTokensGroupByRole()) {
            String role = (String) row.get("role");
            long total = ((Number) row.getOrDefault("total", 0L)).longValue();
            if ("assistant".equals(role)) aiTotal = total;
            else if ("user".equals(role)) userTotal = total;
        }

        // 0 占位 7 天，保证曲线不断点
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(TOKEN_USAGE_WINDOW_DAYS - 1L);
        Map<String, TokenUsageVO.Daily> dailyMap = new LinkedHashMap<>(TOKEN_USAGE_WINDOW_DAYS);
        for (int i = 0; i < TOKEN_USAGE_WINDOW_DAYS; i++) {
            String key = startDate.plusDays(i).format(DATE_FMT);
            dailyMap.put(key, TokenUsageVO.Daily.builder()
                    .date(key).aiTokens(0L).userTokens(0L).total(0L).build());
        }

        List<Map<String, Object>> dailyRows = chatMessageMapper.sumAllTokensDailyGroupByRole(
                startDate.atStartOfDay());
        for (Map<String, Object> row : dailyRows) {
            String dateKey = formatDateKey(row.get("d"));
            if (dateKey == null) continue;
            TokenUsageVO.Daily d = dailyMap.get(dateKey);
            if (d == null) continue;
            String role = (String) row.get("role");
            long total = ((Number) row.getOrDefault("total", 0L)).longValue();
            if ("assistant".equals(role)) {
                d.setAiTokens(d.getAiTokens() + total);
            } else if ("user".equals(role)) {
                d.setUserTokens(d.getUserTokens() + total);
            }
            d.setTotal(d.getAiTokens() + d.getUserTokens());
        }

        return TokenUsageVO.builder()
                .total(aiTotal + userTotal)
                .aiTotal(aiTotal)
                .userTotal(userTotal)
                .daily(new ArrayList<>(dailyMap.values()))
                .build();
    }

    /** MyBatis 对 SQL DATE() 的返回类型可能是 java.sql.Date / LocalDate / String */
    private String formatDateKey(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate ld) return ld.format(DATE_FMT);
        if (dateObj instanceof Date d) return d.toLocalDate().format(DATE_FMT);
        if (dateObj instanceof java.util.Date ud) {
            return new java.sql.Date(ud.getTime()).toLocalDate().format(DATE_FMT);
        }
        return dateObj.toString();
    }
}