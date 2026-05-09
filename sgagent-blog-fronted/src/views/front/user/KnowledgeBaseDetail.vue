<template>
  <el-card class="user-page-card kb-detail-card" shadow="never">
    <template #header>
      <div class="page-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" link @click="$router.push('/user/kbs')">返回</el-button>
          <el-divider direction="vertical" />
          <el-icon class="header-icon color-primary"><Folder /></el-icon>
          <span class="header-title">{{ kb.name || '加载中…' }}</span>
        </div>
        <div class="header-desc" v-if="kb.description">{{ kb.description }}</div>
      </div>
    </template>

    <!-- 上传区 -->
    <el-upload
        ref="uploadRef"
        class="kb-upload"
        action="#"
        drag
        multiple
        :show-file-list="false"
        :http-request="customUpload"
        :before-upload="beforeUpload"
        :disabled="uploading"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="upload-tip">
          支持 docx / pdf / txt / md / 表格 / 代码 等文档型文件，单个文件 ≤ 10MB；
          上传后会自动提取文本并向量化用于知识库检索。
        </div>
      </template>
    </el-upload>

    <!-- 文件列表 -->
    <div class="files-section" v-loading="loading">
      <div class="files-header">
        <span class="files-title">文件列表</span>
        <el-tag size="small" type="info" effect="plain" round>{{ files.length }} 个</el-tag>
        <div class="grow"></div>
        <el-button size="small" :icon="Refresh" @click="loadFiles" :loading="loading">刷新</el-button>
      </div>

      <el-empty v-if="!loading && files.length === 0" description="还没有上传任何文件" :image-size="80" />

      <el-table v-else :data="files" stripe class="files-table" style="width:100%">
        <el-table-column label="文件名" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-icon class="file-icon"><Document /></el-icon>
            <a :href="row.fileUrl" target="_blank" rel="noopener" class="file-link">{{ row.fileName }}</a>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="ext" width="70" />
        <el-table-column label="大小" width="80">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="向量化状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light" class="vector-status">
              <el-icon v-if="row.status === 'PENDING'" class="is-loading"><Loading /></el-icon>
              <span>{{ statusText(row.status) }}</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Chunks" prop="chunkCount" width="70" align="center" />
        <el-table-column label="上传时间" width="140">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="75" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              <span style="margin-left:2px">删除</span>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft, Delete, Document, Folder, Loading, Refresh, UploadFilled
} from '@element-plus/icons-vue';
import { deleteKbFile, getKb, getKbFiles, uploadKbFile } from '@/api/front/user/kb.js';

const route = useRoute();
// 雪花 id 是 19 位 Long，超过 JS Number 安全整数范围，必须保持字符串
const kbId = ref(String(route.params.kbId));

const kb = reactive({ id: null, name: '', description: '', fileCount: 0 });
const files = ref([]);
const loading = ref(false);
const uploading = ref(false);
let pollTimer = null;

const formatSize = (bytes) => {
  const v = Number(bytes || 0);
  if (v < 1024) return v + ' B';
  if (v < 1024 * 1024) return (v / 1024).toFixed(1) + ' KB';
  return (v / 1024 / 1024).toFixed(1) + ' MB';
};

const statusText = (s) => ({ PENDING: '向量化中', SUCCESS: '已就绪', FAILED: '失败' })[s] || s;

// 上传时间紧凑显示（MM-DD HH:mm），避免占太宽列后产生水平滚动
const formatTime = (s) => {
  if (!s) return '';
  const m = String(s).match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/);
  return m ? `${m[2]}-${m[3]} ${m[4]}:${m[5]}` : s;
};
const statusType = (s) => ({ PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger' })[s] || 'info';

const loadKb = async () => {
  const res = await getKb(kbId.value);
  if (res.code === 200) Object.assign(kb, res.data);
};

const loadFiles = async () => {
  loading.value = true;
  try {
    const res = await getKbFiles(kbId.value);
    if (res.code === 200) files.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

// 仅当存在 PENDING 文件时才启动轮询，全部就绪后自动停止，避免无意义请求
const ensurePolling = () => {
  const hasPending = files.value.some(f => f.status === 'PENDING');
  if (hasPending && !pollTimer) {
    pollTimer = setInterval(async () => {
      await loadFiles();
      if (!files.value.some(f => f.status === 'PENDING')) {
        clearInterval(pollTimer);
        pollTimer = null;
      }
    }, 3000);
  }
};

const beforeUpload = (file) => {
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error('文件不能超过 10MB');
    return false;
  }
  return true;
};

const customUpload = async (options) => {
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', options.file);
    const res = await uploadKbFile(kbId.value, formData);
    if (res.code === 200) {
      ElMessage.success(`上传成功：${options.file.name}`);
      await loadFiles();
      ensurePolling();
      options.onSuccess(res);
    } else {
      ElMessage.error(res.msg || '上传失败');
      options.onError(new Error(res.msg));
    }
  } catch (e) {
    options.onError(e);
  } finally {
    uploading.value = false;
  }
};

const handleDelete = async (file) => {
  try {
    await ElMessageBox.confirm(
        `确定删除文件「${file.fileName}」吗？其向量也会一并删除。`,
        '删除确认',
        { type: 'warning' }
    );
  } catch { return; }
  await deleteKbFile(file.id);
  ElMessage.success('已删除');
  await loadFiles();
};

onMounted(async () => {
  await loadKb();
  await loadFiles();
  ensurePolling();
});

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style scoped>
.kb-detail-card .page-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-icon { font-size: 18px; }
.header-title { font-size: 16px; font-weight: 600; }
.header-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-left: 78px;
}
.color-primary { color: var(--el-color-primary); }

.kb-upload { margin-bottom: 20px; }
.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.files-section { margin-top: 8px; }
.files-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.files-title { font-size: 15px; font-weight: 600; }
.grow { flex: 1; }

.file-icon {
  margin-right: 6px;
  color: var(--el-color-primary);
  vertical-align: middle;
}
.file-link {
  color: var(--el-text-color-primary);
  text-decoration: none;
}
.file-link:hover {
  color: var(--el-color-primary);
  text-decoration: underline;
}

/* 状态 tag：spinner + 文字水平居中对齐
   注意 el-tag 内部有 .el-tag__content wrapper（v2.3+），需要用 :deep 命中 */
.vector-status {
  white-space: nowrap;
}
.vector-status :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}
/* 兼容旧版本 el-tag（无 .el-tag__content 子元素时直接命中 tag 自身） */
.vector-status:not(:has(.el-tag__content)) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}
.vector-status .el-icon {
  flex-shrink: 0;
  font-size: 12px;
}

.is-loading {
  animation: rotating 2s linear infinite;
}
@keyframes rotating {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}
</style>
