<template>
  <div class="admin-content">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>商品管理</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button type="primary" @click="openAddModal">
        <el-icon><Plus /></el-icon>
        新增商品
      </el-button>
    </div>
    
    <el-card class="main-card" shadow="hover">
      <el-table :data="products" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="category_name" label="分类" width="120">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.category_name || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120">
          <template #default="scope">
            <span class="price">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
      
      <div v-if="products.length === 0" class="empty-state">
        <el-empty description="暂无商品数据" />
      </div>
    </el-card>
    
    <el-dialog :title="editMode ? '编辑商品' : '新增商品'" v-model="showModal" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option label="请选择分类" value="" />
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input type="textarea" v-model="form.description" placeholder="请输入商品描述" :rows="3" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input type="number" v-model.number="form.price" placeholder="请输入价格" step="0.01" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input type="number" v-model.number="form.stock" placeholder="请输入库存" />
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
 * 管理后台商品管理：商品列表、新增、编辑、删除
 *
 * @author 傅崇睿
 */

import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAdminProducts, getAdminCategories, saveAdminProduct, deleteAdminProduct } from '../../api/admin'

const products = ref([])
const categories = ref([])
const showModal = ref(false)
const editMode = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({
  categoryId: '',
  name: '',
  description: '',
  price: '',
  stock: ''
})

const rules = {
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === '' || value === null || value === undefined) {
          callback(new Error('请输入价格'))
        } else if (isNaN(Number(value))) {
          callback(new Error('价格必须是数字'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value === '' || value === null || value === undefined) {
          callback(new Error('请输入库存'))
        } else if (!Number.isInteger(Number(value))) {
          callback(new Error('库存必须是整数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadProducts = async () => {
  try {
    const [productsData, categoriesData] = await Promise.all([
      getAdminProducts(),
      getAdminCategories()
    ])
    products.value = productsData || []
    categories.value = categoriesData || []
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

const openAddModal = () => {
  editMode.value = false
  editingId.value = null
  form.categoryId = ''
  form.name = ''
  form.description = ''
  form.price = ''
  form.stock = ''
  showModal.value = true
  // 在 DOM 渲染完成后清除校验状态
  nextTick(() => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  })
}

const handleEdit = (row) => {
  editMode.value = true
  editingId.value = row.id
  form.categoryId = row.category_id ? row.category_id.toString() : ''
  form.name = row.name
  form.description = row.description || ''
  form.price = row.price.toString()
  form.stock = row.stock.toString()
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  // 清除表单校验状态
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  editMode.value = false
  editingId.value = null
  form.categoryId = ''
  form.name = ''
  form.description = ''
  form.price = ''
  form.stock = ''
}

const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    // 防御性校验：确保分类必填
    if (!form.categoryId) {
      ElMessage.warning('请选择商品分类')
      return
    }
    
    try {
      const data = {
        categoryId: parseInt(form.categoryId),
        name: form.name,
        description: form.description,
        price: parseFloat(form.price),
        stock: parseInt(form.stock),
        status: 1
      }
      if (editMode.value && editingId.value) {
        data.id = editingId.value
      }
      await saveAdminProduct(data)
      ElMessage.success(editMode.value ? '更新成功' : '保存成功')
      closeModal()
      await loadProducts()
    } catch (error) {
      ElMessage.error(editMode.value ? '更新失败' : '保存失败')
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该商品吗？',
      '提示',
      { type: 'warning' }
    )
    
    await deleteAdminProduct(row.id)
    ElMessage.success('删除成功')
    await loadProducts()
  } catch (error) {
    // axios拦截器已经处理了错误弹窗，这里不再重复处理
    if (error && error.message && error.message === 'cancel') {
      ElMessage.info('已取消删除')
    }
  }
}

onMounted(loadProducts)
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

.price {
  color: #409EFF;
  font-weight: 600;
}

.empty-state {
  padding: 40px;
}
</style>
