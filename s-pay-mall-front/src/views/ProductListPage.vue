<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingCart } from '@element-plus/icons-vue'
import { useProduct } from '@/hooks/useProduct'
import { useCart } from '@/hooks/useCart'
import type { ProductVO } from '@/types/product'

const router = useRouter()
const { products, categories, loading, queryParams, setCategory, loadData } = useProduct()
const { addToCart } = useCart()

onMounted(() => {
  loadData()
})

const getProductImage = (product: ProductVO) => {
  const name = product.name || ''
  if (name.includes('华为')) {
    return 'https://img14.360buyimg.com/n1/jfs/t1/192230/37/42491/79929/65790479F630b925b/3b4d455d3e09cb9e.jpg'
  }
  if (name.includes('松鼠') || name.includes('坚果')) {
    return 'https://img14.360buyimg.com/n1/jfs/t1/107937/17/30310/256950/63ff2f59F94a6fe04/30dfa428be6e355c.jpg'
  }
  if (name.includes('星巴克')) {
    return 'https://img14.360buyimg.com/n1/jfs/t1/174404/24/36881/49845/64a66a19F95efba5f/8ee0731fa122ec00.jpg'
  }
  return 'https://via.placeholder.com/400x400.png?text=Product+Image'
}

const handleCategoryChange = (categoryId: number | null) => {
  setCategory(categoryId)
}

const handleAddToCart = async (productId: number) => {
  const success = await addToCart(productId)
  if (!success) {
    router.push('/login')
  }
}
</script>

<template>
  <div class="product-list-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-top">
        <div>
          <h2 class="page-title">商品列表</h2>
          <p class="page-subtitle">共 {{ products.length }} 件商品</p>
        </div>
        <div class="header-actions">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索商品..."
            size="default"
            :prefix-icon="Search"
            class="search-input"
          />
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- Sidebar -->
      <aside class="sidebar">
        <h3 class="sidebar-title">商品分类</h3>
        <el-menu class="category-menu" default-active="">
          <el-menu-item 
            :index="'all'" 
            :class="{ active: queryParams.categoryId === null }"
            @click="handleCategoryChange(null)"
          >
            全部商品
          </el-menu-item>
          <el-menu-item 
            v-for="cat in categories"
            :key="cat.id"
            :index="cat.id.toString()"
            :class="{ active: queryParams.categoryId === cat.id }"
            @click="handleCategoryChange(cat.id)"
          >
            {{ cat.name }}
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- Products Grid -->
      <main class="products-area">
        <el-skeleton v-if="loading" :rows="6" animated />
        
        <div v-else-if="products.length === 0" class="empty-state">
          <el-empty description="没有找到符合条件的商品" />
        </div>

        <div v-else class="products-grid">
          <el-card 
            v-for="product in products"
            :key="product.id"
            class="product-card"
            shadow="hover"
          >
            <div class="product-image">
              <img :src="getProductImage(product)" :alt="product.name" style="width: 100%; height: 100%; object-fit: cover;" />
              <el-tag v-if="product.category_name" type="primary" class="category-tag">
                {{ product.category_name }}
              </el-tag>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description || '暂无描述' }}</p>
              <div class="product-footer">
                <div class="product-price">
                  <span class="price">¥{{ product.price }}</span>
                  <span class="stock">库存 {{ product.stock }} 件</span>
                </div>
                <el-button 
                  type="primary" 
                  size="small" 
                  icon="ShoppingCart"
                  @click="handleAddToCart(product.id)"
                >
                  加入购物车
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.product-list-page {
  min-height: 100%;
}

.page-header {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-subtitle {
  color: #909399;
  font-size: 14px;
  margin: 4px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 240px;
}

.main-content {
  display: flex;
  gap: 20px;
}

.sidebar {
  width: 200px;
  flex-shrink: 0;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px;
}

.category-menu {
  border: none;
}

.category-menu .el-menu-item {
  margin: 4px 0;
  border-radius: 8px;
}

.category-menu .el-menu-item.active {
  background: #e8f4ff;
  color: #409EFF;
}

.products-area {
  flex: 1;
}

.empty-state {
  padding: 60px 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image {
  position: relative;
  width: 100%;
  height: 260px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.category-tag {
  position: absolute;
  top: 12px;
  left: 12px;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  height: 36px;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.stock {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
  }
  
  .category-menu {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .category-menu .el-menu-item {
    flex: 0 0 auto;
    padding: 8px 16px;
  }
}
</style>