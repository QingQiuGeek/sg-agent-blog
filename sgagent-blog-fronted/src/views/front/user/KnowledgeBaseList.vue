<template>
  <el-card class="user-page-card kb-list-card" shadow="never">
    <template #header>
      <div class="page-header">
        <div class="header-left">
          <el-icon class="header-icon color-primary"><Collection /></el-icon>
          <span class="header-title">我的知识库</span>
          <el-tag size="small" type="info" effect="plain" round class="kb-count">
            {{ kbList.length }} 个
          </el-tag>
        </div>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建知识库</el-button>
      </div>
    </template>

    <div v-loading="loading">
      <el-empty v-if="!loading && kbList.length === 0" description="还没有知识库，点击右上角创建一个吧">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新建知识库</el-button>
      </el-empty>

      <div v-else class="kb-grid">
        <el-card
            v-for="kb in kbList"
            :key="kb.id"
            shadow="hover"
            class="kb-card"
            @click="goDetail(kb.id)"
        >
          <div class="kb-card-head">
            <el-icon class="kb-card-icon"><Folder /></el-icon>
            <div class="kb-card-meta">
              <div class="kb-card-name">{{ kb.name }}</div>
              <div class="kb-card-desc">{{ kb.description || '暂无描述' }}</div>
            </div>
          </div>
          <div class="kb-card-foot">
            <div class="kb-card-stat">
              <el-icon><Document /></el-icon>
              <span>{{ kb.fileCount || 0 }} 个文件</span>
            </div>
            <div class="kb-card-actions" @click.stop>
              <el-button size="small" link :icon="Edit" @click="handleEdit(kb)">编辑</el-button>
              <el-button size="small" link type="danger" :icon="Delete" @click="handleDelete(kb)">删除</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="给知识库起个名字" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="可选，简单介绍下这个知识库"
              maxlength="255"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Collection, Document, Edit, Delete, Folder, Plus } from '@element-plus/icons-vue';
import { addKb, deleteKb, getMyKbs, updateKb } from '@/api/front/user/kb.js';

const router = useRouter();

const loading = ref(false);
const kbList = ref([]);

const dialogVisible = ref(false);
const dialogTitle = ref('新建知识库');
const submitting = ref(false);
const formRef = ref(null);
const form = reactive({ id: null, name: '', description: '' });

const rules = {
  name: [
    { required: true, message: '知识库名称不能为空', trigger: 'blur' },
    { max: 64, message: '名称不能超过 64 个字符', trigger: 'blur' }
  ],
  description: [{ max: 255, message: '描述不能超过 255 个字符', trigger: 'blur' }]
};

const loadList = async () => {
  loading.value = true;
  try {
    const res = await getMyKbs();
    if (res.code === 200) kbList.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.id = null;
  form.name = '';
  form.description = '';
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = '新建知识库';
  dialogVisible.value = true;
};

const handleEdit = (kb) => {
  form.id = kb.id;
  form.name = kb.name;
  form.description = kb.description || '';
  dialogTitle.value = '编辑知识库';
  dialogVisible.value = true;
};

const submit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload = { name: form.name, description: form.description || null };
      if (form.id) {
        await updateKb({ id: form.id, ...payload });
        ElMessage.success('修改成功');
      } else {
        await addKb(payload);
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      await loadList();
    } finally {
      submitting.value = false;
    }
  });
};

const handleDelete = async (kb) => {
  try {
    await ElMessageBox.confirm(
        `确定删除知识库「${kb.name}」吗？其下所有文件与向量都会被删除，且不可恢复。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    );
  } catch { return; }
  await deleteKb(kb.id);
  ElMessage.success('已删除');
  await loadList();
};

const goDetail = (id) => router.push(`/user/kbs/${id}`);

onMounted(loadList);
</script>

<style scoped>
.kb-list-card .page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.kb-list-card .header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-icon {
  font-size: 18px;
  vertical-align: middle;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.kb-count {
  margin-left: 4px;
}
.color-primary { color: var(--el-color-primary); }

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.kb-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.kb-card:hover {
  transform: translateY(-2px);
}
.kb-card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.kb-card-icon {
  font-size: 28px;
  color: var(--el-color-primary);
  margin-top: 2px;
  flex-shrink: 0;
}
.kb-card-meta { flex: 1; min-width: 0; }
.kb-card-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kb-card-desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}
.kb-card-foot {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.kb-card-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.kb-card-actions {
  display: flex;
  gap: 4px;
}
</style>
