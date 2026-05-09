<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="card stat-card">
          <div class="icon-wrapper primary"><el-icon size="32"><Document /></el-icon></div>
          <div class="flex-spacer"></div>
          <el-statistic title="文章数量" :value="data.articleCount" class="custom-statistic" />
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="card stat-card">
          <div class="icon-wrapper success"><el-icon size="32"><User /></el-icon></div>
          <div class="flex-spacer"></div>
          <el-statistic title="用户数量" :value="data.userCount" class="custom-statistic" />
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="card stat-card">
          <div class="icon-wrapper warning"><el-icon size="32"><ChatDotRound /></el-icon></div>
          <div class="flex-spacer"></div>
          <el-statistic title="评论数量" :value="data.commentCount" class="custom-statistic" />
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="card stat-card">
          <div class="icon-wrapper info"><el-icon size="32"><View /></el-icon></div>
          <div class="flex-spacer"></div>
          <el-statistic title="总访问量" :value="data.visitCount" class="custom-statistic" />
        </div>
      </el-col>

      <el-col :sm="24" :md="10"><div class="card chart-card" id="line"></div></el-col>

      <el-col :sm="24" :md="14">
        <div class="card chart-card token-card">
          <div class="token-header">
            <span class="token-title">全站 AI Token 用量</span>
            <span class="token-hint">近 7 天</span>
          </div>

          <div class="token-stats">
            <div class="token-stat-item">
              <div class="token-stat-value">{{ formatTokens(tokenData.total) }}</div>
              <div class="token-stat-label">总用量</div>
            </div>
            <div class="token-divider"></div>
            <div class="token-stat-item">
              <div class="token-stat-value color-primary">{{ formatTokens(tokenData.aiTotal) }}</div>
              <div class="token-stat-label">AI 回复</div>
            </div>
            <div class="token-divider"></div>
            <div class="token-stat-item">
              <div class="token-stat-value color-warning">{{ formatTokens(tokenData.userTotal) }}</div>
              <div class="token-stat-label">用户提问</div>
            </div>
          </div>

          <div id="token-chart" class="token-chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, watch } from "vue";
import * as echarts from 'echarts';
import { useDark } from '@vueuse/core';
import { getDashboardData } from "@/api/admin/dashboard.js";
import { useRequest } from "@/composables/useRequest.js"; // 引入 Hook

const isDark = useDark();

const data = reactive({ articleCount: 0, userCount: 0, commentCount: 0, visitCount: 0 });
const tokenData = reactive({ total: 0, aiTotal: 0, userTotal: 0, daily: [] });

// 数字缩写：>=1万 -> "1.2万"，>=1千 -> "1.2k"
const formatTokens = (n) => {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  if (v >= 1000) return (v / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return v.toString()
}

let currentTrendData = null;
let currentTokenDaily = null;
let lineChart = null;
let tokenChart = null;
let resizeTimer = null;

// 使用 useRequest 接管数据请求
const { execute: fetchDashboard } = useRequest(getDashboardData);

onMounted(() => {
  initDashboard();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  disposeCharts();
});

watch(isDark, () => {
  if (currentTrendData) renderLineChart(currentTrendData);
  if (currentTokenDaily) renderTokenChart(currentTokenDaily);
});

const initDashboard = async () => {
  // useRequest 成功时直接返回 res.data
  const resData = await fetchDashboard();
  if (resData) {
    data.articleCount = resData.articleCount;
    data.userCount = resData.userCount;
    data.commentCount = resData.commentCount;
    data.visitCount = resData.visitCount;

    currentTrendData = resData.visitTrend;

    if (resData.tokenUsage) {
      tokenData.total = resData.tokenUsage.total ?? 0
      tokenData.aiTotal = resData.tokenUsage.aiTotal ?? 0
      tokenData.userTotal = resData.tokenUsage.userTotal ?? 0
      tokenData.daily = resData.tokenUsage.daily ?? []
      currentTokenDaily = tokenData.daily
    }

    await nextTick(() => {
      renderLineChart(currentTrendData);
      if (currentTokenDaily) renderTokenChart(currentTokenDaily);
    });
  }
};

const renderLineChart = (trendData) => {
  let chartDom = document.getElementById('line');
  if (lineChart) lineChart.dispose();
  lineChart = echarts.init(chartDom, isDark.value ? 'dark' : null);
  lineOption.backgroundColor = 'transparent';
  lineOption.xAxis.data = trendData.dates || [];
  lineOption.series[0].data = trendData.pvCounts || [];
  lineChart.setOption(lineOption);
  setTimeout(() => lineChart.resize(), 100);
}

const renderTokenChart = (daily) => {
  const chartDom = document.getElementById('token-chart');
  if (!chartDom) return;
  if (tokenChart) tokenChart.dispose();
  tokenChart = echarts.init(chartDom, isDark.value ? 'dark' : null);
  const dates = (daily || []).map(d => d.date?.slice(5));
  const aiSeries = (daily || []).map(d => d.aiTokens ?? 0);
  const userSeries = (daily || []).map(d => d.userTokens ?? 0);
  tokenChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { data: ['AI 回复', '用户提问'], right: 10, top: 0 },
    grid: { left: 36, right: 16, top: 30, bottom: 24 },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      { name: 'AI 回复', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        areaStyle: { opacity: 0.15 }, itemStyle: { color: '#409eff' }, data: aiSeries },
      { name: '用户提问', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        areaStyle: { opacity: 0.15 }, itemStyle: { color: '#e6a23c' }, data: userSeries },
    ],
  });
  setTimeout(() => tokenChart.resize(), 100);
}

const handleResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    if (lineChart) lineChart.resize({ animation: { duration: 0 } });
    if (tokenChart) tokenChart.resize({ animation: { duration: 0 } });
  }, 200);
};

const disposeCharts = () => {
  if (lineChart) { lineChart.dispose(); lineChart = null; }
  if (tokenChart) { tokenChart.dispose(); tokenChart = null; }
};

// ================= ECharts 配置 =================
let lineOption = { title: { text: '近7天访问量', left: 'left', }, grid: { left: '8%', right: '8%', bottom: '8%', top: '15%', containLabel: true }, xAxis: { type: 'category', data: [] }, tooltip: { trigger: 'axis', formatter: '{b} <br/>访客 : {c}' }, yAxis: { type: 'value' }, series: [ { data: [], type: 'line', smooth: true } ] };
</script>

<style scoped>
.dashboard-container {
  width: 100%;
}

/* ==========================================
   顶部数据卡片布局
   ========================================== */
.stat-card {
  display: flex;
  align-items: center;
  /* 卡片的其余样式(背景色、阴影)由全局 .card 接管 */
}

/* 弹簧占位符，把图标和文字推向两侧 */
.flex-spacer {
  flex: 1;
}

/* 图标的高级感微透明背景 */
.icon-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon-wrapper.primary { background: rgba(64, 158, 255, 0.1); color: #409EFF; }
.icon-wrapper.success { background: rgba(103, 194, 58, 0.1); color: #67C23A; }
.icon-wrapper.warning { background: rgba(230, 162, 60, 0.1); color: #E6A23C; }
.icon-wrapper.info    { background: rgba(144, 147, 153, 0.1); color: #909399; }

/* ==========================================
   Statistic 统计组件自定义样式
   ========================================== */
.custom-statistic {
  text-align: right; /* 确保标题和数字靠右对齐 */
}

:deep(.custom-statistic .el-statistic__head) {
  justify-content: flex-end;
  margin-bottom: 8px;
}

:deep(.custom-statistic .el-statistic__content) {
  justify-content: flex-end;
  font-weight: bolder;
  font-size: 26px;
}

/* ==========================================
   图表卡片样式
   ========================================== */
.chart-card {
  height: 400px;
  overflow: hidden;
  padding: 15px;
}

/* ==========================================
   Token 用量卡片
   ========================================== */
.token-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.token-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.token-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.token-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.token-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 6px 0 4px;
}
.token-stat-item {
  flex: 1;
  text-align: center;
}
.token-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.1;
}
.token-stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.token-divider {
  width: 1px;
  height: 36px;
  background: var(--el-border-color-lighter);
}
.token-chart {
  flex: 1;
  width: 100%;
  min-height: 240px;
}
.color-primary { color: var(--el-color-primary); }
.color-warning { color: #e6a23c; }
</style>