<template>
  <div class="SGAgent-Blog-logo" :class="{ 'is-collapse': isCollapse }" @click="goToHome">
    <img class="logo-icon" src="/logo.png" alt="SGAgent-Blog Logo" />

    <div class="logo-text">
      <span class="text-ctrl">SGAgent</span>
      <span class="text-blog">Blog</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, withDefaults } from 'vue';
import { useRouter } from 'vue-router';

// 定义组件属性，允许外部控制大小
interface Props {
  size?: number; // Logo 的整体高度 (像素)
  isCollapse?: boolean;
}

// 设置默认值，默认高度为 40px
const props = withDefaults(defineProps<Props>(), {
  size: 40,
  isCollapse: false,
});

const router = useRouter();

// 点击 Logo 跳转回首页
const goToHome = () => {
  // 只有当路由实例存在时才尝试跳转
  if (router) {
    router.push('/');
  }
};
</script>

<style scoped>
/* 使用 Vue 3 的 v-bind 特性动态绑定 size 属性 */
.SGAgent-Blog-logo {
  --logo-height: v-bind('size + "px"');

  display: inline-flex;
  align-items: center; /* 垂直居中对齐，比 baseline 更稳重 */
  height: var(--logo-height);
  cursor: pointer;
  background: transparent;
  user-select: none; /* 防止文字被选中 */
  transition: opacity 0.2s ease, transform 0.2s ease;
  vertical-align: middle; /* 改善在行内元素中的对齐 */
}

.SGAgent-Blog-logo:hover {
  opacity: 0.85; /* 悬停时稍微变淡 */
  transform: translateY(-1px); /* 轻微上浮，增加动感 */
}

/* Logo 图标样式：使用 public/logo.png */
.logo-icon {
  height: 100%; /* 与容器同高，图片自身有留白 */
  width: auto;
  max-height: var(--logo-height);
  margin-right: 8px;
  flex-shrink: 0; /* 防止图片被压缩 */
  object-fit: contain; /* 保持比例不变形 */
  display: block;
  transition: margin-right 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 折叠时：图标右边距归零 */
.is-collapse .logo-icon {
  margin-right: 0 !important;
}

.logo-text {
  display: flex;
  align-items: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  font-weight: 700;
  letter-spacing: -0.5px;
  font-size: calc(var(--logo-height) * 0.6);

  line-height: 1.2;
  padding-bottom: 2px;

  max-width: 150px;
  opacity: 1;
  white-space: nowrap;

  overflow: hidden;

  transition: max-width 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), opacity 0.2s ease;
}

/* 折叠时：文字宽度收缩至0，同时透明度消失 */
.is-collapse .logo-text {
  max-width: 0;
  opacity: 0;
}

/* "SGAgent" 的样式 */
.text-ctrl {
  color: var(--el-color-primary);
}

/* "Blog" 的样式 */
.text-blog {
  color: var(--el-text-color-regular);
  margin-left: 2px; /* SGAgent 和 Blog 之间的微小间距 */
}
</style>