<template>
  <div class="cart-container">
    <el-card class="cart-card" shadow="never">
      <template #header>
        <div class="cart-header">
          <div class="header-left">
            <el-icon class="header-icon" color="#409EFF"><ShoppingCart /></el-icon>
            <div class="header-info">
              <h2>购物车</h2>
              <p>共 {{ totalCount }} 件商品</p>
            </div>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="items.length === 0 && !loading" description="购物车是空的">
          <el-button type="primary" @click="$router.push('/products')">去购物</el-button>
        </el-empty>

        <div v-else>
          <div class="select-all">
            <el-checkbox v-model="isAllSelected" @change="toggleAll">
              全选
            </el-checkbox>
          </div>

          <div v-for="item in items" :key="item.id" class="cart-item">
            <el-checkbox v-model="item.selected" @change="() => toggleSelect(item.id)" />
            <div class="item-image">
              <div class="image-placeholder">
                <el-icon :size="32"><Picture /></el-icon>
              </div>
            </div>
            <div class="item-info">
              <!-- null 安全访问：兜底显示 -->
              <h3 class="item-name">{{ item.productName || '未知商品' }}</h3>
              <div class="item-bottom">
                <!-- null 安全访问：价格兜底为 0 -->
                <span class="item-price">¥{{ ((item.productPrice || 0)).toFixed(2) }}</span>
                <div class="quantity-control">
                  <el-button
                    size="small"
                    :icon="Minus"
                    @click="handleUpdateQuantity(item, -1)"
                    :disabled="(item.quantity || 0) <= 1"
                  />
                  <span class="quantity">{{ item.quantity || 0 }}</span>
                  <el-button 
                    size="small" 
                    :icon="Plus" 
                    @click="handleUpdateQuantity(item, 1)" 
                    :disabled="(item.quantity || 0) >= (item.stock || 0)"
                  />
                </div>
                <!-- null 安全访问：小计兜底为 0 -->
                <span class="item-amount">小计: ¥{{ ((item.itemAmount || 0)).toFixed(2) }}</span>
                <el-button type="danger" :icon="Delete" circle @click="handleRemoveItem(item.id)" />
              </div>
            </div>
          </div>

          <div class="cart-footer">
            <div class="summary">
              <div class="summary-row">
                <span>商品小计</span>
                <span>{{ totalCount }} 件商品</span>
              </div>
              <div class="summary-row total">
                <span>合计</span>
                <span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
              </div>
            </div>
            <el-button type="primary" size="large" class="checkout-btn" @click="handleCheckout">
              去结算 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, Plus, Minus, Delete, ArrowRight, Picture } from '@element-plus/icons-vue'
import { useCart } from '../hooks/useCart'

const router = useRouter()
const route = useRoute()
const {
  items,
  loading,
  totalAmount,
  totalCount,
  allSelected,
  loadCart,
  updateQuantity,
  removeItem,
  toggleSelect,
  toggleAll,
  getSelectedItems,
} = useCart()

const isAllSelected = computed({
  get: () => allSelected.value,
  set: () => toggleAll()
})

const handleUpdateQuantity = async (item, delta) => {
  const currentQuantity = Number(item.quantity) || 0
  const newQuantity = currentQuantity + delta
  if (newQuantity < 1) return
  
  try {
    await updateQuantity(item.productId, newQuantity)
  } catch (error) {
    item.quantity = currentQuantity
    ElMessage.error(error.message || '更新失败')
  }
}

const handleRemoveItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeItem(itemId)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleCheckout = () => {
  const selectedItems = getSelectedItems()
  if (selectedItems.length === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }
  localStorage.setItem('checkout_products', JSON.stringify(selectedItems))
  router.push('/checkout')
}

watch(() => route.path, (newPath) => {
  if (newPath === '/cart') {
    loadCart()
  }
}, { immediate: true })
</script>

<style scoped>
.cart-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px;
}

.cart-card {
  border-radius: 12px;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 32px;
}

.header-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-info p {
  font-size: 14px;
  color: #909399;
  margin: 4px 0 0;
}

.cart-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
  margin-bottom: 16px;
}

.item-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #e4e8eb;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 4px;
}

.item-category {
  font-size: 13px;
  color: #909399;
  margin: 0 0 auto;
}

.item-bottom {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}

.item-price {
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 32px;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.cart-footer {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
  margin-top: 20px;
}

.summary {
  margin-bottom: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}

.summary-row.total {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.checkout-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}
</style>
