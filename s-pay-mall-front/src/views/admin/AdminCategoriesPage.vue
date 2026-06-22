<template>
  <div class="admin-content">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>分类管理</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button type="primary" @click="openAddModal">
        <el-icon><Plus /></el-icon>
        新增分类
      </el-button>
    </div>
    
    <el-card class="main-card" shadow="hover">
      <el-table :data="categories" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200">
          <template #default="scope">
            <div class="category-info">
              <el-icon class="category-icon"><Folder /></el-icon>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary" 
              icon="Edit" 
              @click="handleEdit(scope.row)"
            />
            <el-button 
              size="small" 
              type="danger" 
              icon="Delete" 
              @click="handleDelete(scope.row)"
            />
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="categories.length === 0" class="empty-state">
        <el-empty description="暂无分类数据" />
      </div>
    </el-card>
    
    <el-dialog :title="editMode ? '编辑分类' : '新增分类'" v-model="showModal" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal">取消</el-button>
        <el-button type="primary" @click="handleSave">{{ editMode ? '更新' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 管理后台分类管理：分类列表、新增、编辑、删除
 *
 * @author 傅崇睿
 */

import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Folder } from '@element-plus/icons-vue'
import { getAdminCategories, saveAdminCategory, deleteAdminCategory } from '../../api/admin'

const categories = ref([])
const showModal = ref(false)
const editMode = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ]
}

const load = async () => {
  try {
    const data = await getAdminCategories()
    categories.value = data || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const openAddModal = () => {
  editMode.value = false
  editingId.value = null
  form.name = ''
  showModal.value = true
}

const handleEdit = (row) => {
  editMode.value = true
  editingId.value = row.id
  form.name = row.name
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editMode.value = false
  editingId.value = null
  form.name = ''
}

const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      const data = editMode.value ? { ...form, id: editingId.value } : form
      await saveAdminCategory(data)
      ElMessage.success(editMode.value ? '更新成功' : '保存成功')
      closeModal()
      await load()
    } catch (error) {
      ElMessage.error(editMode.value ? '更新失败' : '保存失败')
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该分类吗？',
      '提示',
      { type: 'warning' }
    )
    
    await deleteAdminCategory(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    // axios拦截器已经处理了错误弹窗，这里不再重复处理
    if (error && error.message && error.message === 'cancel') {
      ElMessage.info('已取消删除')
    }
  }
}

load()
</script>

<style scoped>
.admin-content {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.main-card {
  border-radius: 8px;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-icon {
  color: #409EFF;
}

.empty-state {
  padding: 40px;
}
</style>
