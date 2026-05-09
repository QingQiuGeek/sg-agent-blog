<template>
  <div class="user-dashboard">
    <div class="welcome-header">
      <h2 class="greeting">
        {{ greetingText }}，{{ userStore.userInfo?.nickname || '旅行者' }}
      </h2>
      <p class="subtitle">在这里回顾你的阅读足迹，管理你的互动记录。</p>
    </div>

    <el-row :gutter="20" class="stat-cards-row">
      <el-col :xs="12" :sm="8" v-for="(stat, index) in statData" :key="index">
        <el-card
            class="stat-card"
            shadow="hover"
            @click="navigateTo(stat.route)"
            :style="{ '--icon-color': stat.color, '--icon-bg': stat.bgColor }"
        >
          <div class="stat-icon-wrapper">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="activity-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-with-icon">
                <el-icon class="title-icon color-warning"><StarFilled /></el-icon>
                <span class="header-title">最近收藏</span>
              </div>
              <div class="view-all-link" @click="navigateTo('/user/collections')">
                <span>查看全部</span>
                <el-icon class="link-icon"><ArrowRight /></el-icon>
              </div>
            </div>
          </template>

          <div v-if="recentCollections.length > 0" class="mini-list">
            <SimpleArticleCard
                :articles="recentCollections"
                @click="navToArticle"
            />
          </div>
          <el-empty v-else description="暂无收藏" :image-size="70" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="activity-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title-with-icon">
                <el-icon class="title-icon color-danger"><SuccessFilled /></el-icon>
                <span class="header-title">最近点赞</span>
              </div>
              <div class="view-all-link" @click="navigateTo('/user/likes')">
                <span>查看全部</span>
                <el-icon class="link-icon"><ArrowRight /></el-icon>
              </div>
            </div>
          </template>

          <div v-if="recentLikes.length > 0" class="mini-list">
            <SimpleArticleCard
                :articles="recentLikes"
                @click="navToArticle"
            />
          </div>
          <el-empty v-else description="暂无点赞" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI Token 用量统计 -->
    <el-card class="activity-card token-usage-card" shadow="never" v-loading="tokenLoading">
      <template #header>
        <div class="card-header">
          <div class="title-with-icon">
            <el-icon class="title-icon color-primary"><DataLine /></el-icon>
            <span class="header-title">AI 对话 Token 用量</span>
          </div>
          <span class="token-usage-hint">近 7 天</span>
        </div>
      </template>

      <div class="token-usage-body">
        <div class="token-stat-group">
          <div class="token-stat-item">
            <div class="token-stat-value">{{ formatTokens(tokenUsage.total) }}</div>
            <div class="token-stat-label">总用量</div>
          </div>
          <el-divider direction="vertical" class="token-divider" />
          <div class="token-stat-item">
            <div class="token-stat-value color-primary">{{ formatTokens(tokenUsage.aiTotal) }}</div>
            <div class="token-stat-label">AI 回复</div>
          </div>
          <el-divider direction="vertical" class="token-divider" />
          <div class="token-stat-item">
            <div class="token-stat-value color-warning">{{ formatTokens(tokenUsage.userTotal) }}</div>
            <div class="token-stat-label">用户提问</div>
          </div>
        </div>

        <div ref="chartRef" class="token-usage-chart"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/user.js';
import {
  Star, Pointer, ChatDotRound, ArrowRight,
  StarFilled, SuccessFilled, DataLine
} from '@element-plus/icons-vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { getUserDashboardData, getUserTokenUsage } from "@/api/front/system/userInfo.js";

echarts.use([TitleComponent, TooltipComponent, GridComponent, LegendComponent, LineChart, CanvasRenderer]);

const router = useRouter();
const userStore = useUserStore();

// 欢迎语逻辑
const greetingText = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

// 看板数据
const statData = ref([
  { label: '我的收藏', value: 0, icon: Star, color: '#e6a23c', bgColor: 'rgba(230, 162, 60, 0.1)', route: '/user/collections' },
  { label: '我的点赞', value: 0, icon: Pointer, color: '#f56c6c', bgColor: 'rgba(245, 108, 108, 0.1)', route: '/user/likes' },
  { label: '我的评论', value: 0, icon: ChatDotRound, color: '#409eff', bgColor: 'rgba(64, 158, 255, 0.1)', route: '/user/comments' },
]);

// 列表数据
const recentCollections = ref([]);
const recentLikes = ref([]);

const navigateTo = (path) => path && router.push(path);
const navToArticle = (id) => router.push(`/post/${id}`);

// Token 用量状态
const tokenLoading = ref(false)
const tokenUsage = ref({ total: 0, aiTotal: 0, userTotal: 0, daily: [] })
const chartRef = ref(null)
let chartInstance = null

// 数字缩写：>=1万显示 "1.2万"，>=1千显示 "1.2k"
const formatTokens = (n) => {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  if (v >= 1000) return (v / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return v.toString()
}

// 加载数据
const loadDashboardData = async () => {
  try {
    const res = await getUserDashboardData();
    if (res.code === 200) {
      statData.value[0].value = res.data.count.favorite;
      statData.value[1].value = res.data.count.like;
      statData.value[2].value = res.data.count.comment;
      recentCollections.value = res.data.recentFavorites;
      recentLikes.value = res.data.recentLikes;
    }
  } catch (error) {
    console.error('获取总览数据失败', error);
  }
};

const loadTokenUsage = async () => {
  tokenLoading.value = true
  try {
    const res = await getUserTokenUsage()
    if (res.code === 200 && res.data) {
      tokenUsage.value = res.data
      await nextTick()
      renderChart()
    }
  } catch (e) {
    console.error('获取 Token 用量失败', e)
  } finally {
    tokenLoading.value = false
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  const daily = tokenUsage.value.daily || []
  const dates = daily.map(d => d.date?.slice(5)) // MM-dd
  const aiSeries = daily.map(d => d.aiTokens ?? 0)
  const userSeries = daily.map(d => d.userTokens ?? 0)

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['AI 回复', '用户提问'], right: 10, top: 0 },
    grid: { left: 36, right: 16, top: 30, bottom: 24 },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLine: { lineStyle: { color: '#ddd' } } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      {
        name: 'AI 回复', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        areaStyle: { opacity: 0.15 }, itemStyle: { color: '#409eff' }, data: aiSeries,
      },
      {
        name: '用户提问', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        areaStyle: { opacity: 0.15 }, itemStyle: { color: '#e6a23c' }, data: userSeries,
      },
    ],
  }, true)
}

const onResize = () => chartInstance && chartInstance.resize()

onMounted(() => {
  loadDashboardData();
  loadTokenUsage();
  window.addEventListener('resize', onResize)
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.user-dashboard {
  padding-bottom: 20px;
}

/* 欢迎区 */
.welcome-header {
  margin-top: 5px;
  margin-bottom: 25px;
  padding-left: 5px;
}
.greeting {
  font-family: 'SmileySans', sans-serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 10px 0;
  letter-spacing: 1px;
}
.subtitle {
  font-size: 15px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

/* 数据看板 */
.stat-cards-row {
  margin-bottom: 30px;
}
.stat-card {
  border: none;
  border-radius: 16px;
  background-color: var(--el-bg-color-overlay);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--el-box-shadow-light);
}
:deep(.stat-card .el-card__body) {
  padding: 25px 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 提取行内样式：使用传入的 CSS 变量控制颜色 */
.stat-icon-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
  background-color: var(--icon-bg); /* CSS 变量 */
}

.stat-icon-wrapper .el-icon {
  color: var(--icon-color); /* CSS 变量 */
}

.stat-value {
  font-family: 'SmileySans', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.stat-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin-top: 5px;
}

/* 内容展示卡片 */
.activity-card {
  border: none;
  border-radius: 16px;
  background-color: var(--el-bg-color-overlay);
  margin-bottom: 20px;
}

:deep(.activity-card .el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon {
  font-size: 18px;
}
.header-title {
  font-family: 'SmileySans', sans-serif;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.view-all-link {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.3s ease;
  user-select: none;
}

.view-all-link .link-icon {
  font-size: 14px;
  margin-left: 2px;
  transition: transform 0.3s ease;
}

.view-all-link:hover {
  color: var(--el-color-primary);
}

.view-all-link:hover .link-icon {
  transform: translateX(3px);
}

.color-warning { color: #e6a23c; }
.color-danger { color: #f56c6c; }
.color-primary { color: var(--el-color-primary); }

/* ========== Token 用量卡片 ========== */
.token-usage-card {
  margin-bottom: 0;
}
.token-usage-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.token-usage-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.token-stat-group {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 10px 0 4px;
}
.token-stat-item {
  flex: 1;
  text-align: center;
}
.token-stat-value {
  font-family: 'SmileySans', sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}
.token-stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.token-divider {
  height: 36px;
}
.token-usage-chart {
  width: 100%;
  height: 240px;
}
@media screen and (max-width: 768px) {
  .token-stat-value { font-size: 20px; }
  .token-usage-chart { height: 200px; }
}

.mini-list {
  margin: -10px 0;
}

@media screen and (max-width: 768px) {
  .stat-cards-row { margin-bottom: 15px; }
  .stat-card { margin-bottom: 15px; }
  .greeting { font-size: 22px; }
}
</style>