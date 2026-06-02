<template>
  <div class="home-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">S-Pay Mall 聚合支付商城</h1>
        <p class="hero-subtitle">安全、便捷、高效的一站式购物体验，让支付更简单</p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="$router.push('/products')">
            立即购物 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" @click="$router.push('/register')">免费注册</el-button>
        </div>
      </div>
    </div>

    <!-- Features Section -->
    <div class="features-section">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <div class="feature-card">
            <el-icon class="feature-icon" color="#409EFF"><Lightning /></el-icon>
            <h3>极速支付</h3>
            <p>毫秒级响应，支付体验流畅</p>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <div class="feature-card">
            <el-icon class="feature-icon" color="#67C23A"><Lock /></el-icon>
            <h3>安全可靠</h3>
            <p>多重加密，保障交易安全</p>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <div class="feature-card">
            <el-icon class="feature-icon" color="#909399"><Connection /></el-icon>
            <h3>聚合支付</h3>
            <p>支持多种支付方式</p>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Products Section -->
    <div class="products-section">
      <div class="section-header">
        <h2>热门商品</h2>
        <p>精选优质商品，满足您的购物需求</p>
      </div>

      <!-- Category Filter -->
      <div class="category-filter">
        <el-tag
          v-for="cat in categories"
          :key="cat.name"
          :class="{ active: activeCategory === cat.name }"
          @click="handleCategoryChange(cat.name, cat.id)"
        >
          {{ cat.name }}
        </el-tag>
      </div>

      <el-row :gutter="20" v-loading="loading">
        <el-col v-if="products.length === 0 && !loading" :span="24">
          <el-empty description="暂无商品" />
        </el-col>

        <el-col
          v-for="product in products.slice(0, 6)"
          :key="product.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="8"
          :xl="6"
        >
          <el-card class="product-card" shadow="hover" :body-style="{ padding: '0px' }">
            <div class="product-image">
              <img :src="getProductImage(product)" :alt="product.name" />
              <el-tag v-if="product.category_name" class="category-tag" type="primary">
                {{ product.category_name }}
              </el-tag>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description || '暂无描述' }}</p>
              <div class="product-price">
                <span class="price">¥{{ product.price }}</span>
                <span class="stock">库存 {{ product.stock }} 件</span>
              </div>
              <div class="product-actions">
                <el-button type="info" plain size="small" @click="addToCart(product.id)">
                  <el-icon><ShoppingCart /></el-icon> 加入购物车
                </el-button>
                <el-button type="primary" size="small" @click="buyNow(product)">
                  立即购买
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div v-if="products.length > 6" class="more-link">
        <el-button type="primary" text @click="$router.push('/products')">
          查看更多商品 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ShoppingCart,
  ArrowRight,
  Lightning,
  Lock,
  Connection
} from '@element-plus/icons-vue'

const router = useRouter()
const products = ref<Array<any>>([])
const loading = ref<boolean>(true)
const categories = ref<Array<{ name: string; id: number | null }>>([
  { name: '全部', id: null },
  { name: '数码产品', id: 1 },
  { name: '服装配饰', id: 2 },
  { name: '食品饮料', id: 3 }
])
const activeCategory = ref<string>('全部')
const activeCategoryId = ref<number | null>(null)

const getProductImage = (product: { id: number; name: string; image_url?: string }): string => {
  if (product.image_url && product.image_url.startsWith('http')) {
    return product.image_url
  }
  const colors = ['667eea', '764ba2', 'f093fb', '4facfe', '43e97b', 'fa709a', '30cfd0', 'a8edea']
  const color = colors[product.id % colors.length]
  const text = encodeURIComponent(product.name.substring(0, 6))
  return `https://via.placeholder.com/400x400/${color}/ffffff?text=${text}`
}

const fetchProducts = async (categoryId: number | null = null) => {
  try {
    loading.value = true
    const token = localStorage.getItem('token')
    const url = new URL('/mall-api/v1/products', window.location.origin)
    // 传递 categoryId 而非 category name，确保后端正确过滤
    if (categoryId !== null && categoryId !== undefined) {
      url.searchParams.set('categoryId', categoryId.toString())
    }
    const response = await fetch(url.toString(), {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    })
    const data = await response.json()
    // 后端返回的 code 是字符串 "0000"，不是数字 0
    if (data.code === '0000') {
      products.value = data.data
    }
  } catch (error) {
    console.error('获取商品失败:', error)
    ElMessage.error('获取商品失败')
  } finally {
    loading.value = false
  }
}

const handleCategoryChange = (categoryName: string, categoryId: number | null) => {
  activeCategory.value = categoryName
  activeCategoryId.value = categoryId
  fetchProducts(categoryId)
}

const addToCart = async (productId: number): Promise<void> => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const response = await fetch('/mall-api/v1/cart', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ productId, quantity: 1 })
    })
    const data = await response.json()
    if (data.code === '0000') {
      ElMessage.success('已添加到购物车')
    } else {
      ElMessage.error(data.info || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败，请登录后重试')
  }
}

const buyNow = (product: { id: number }): void => {
  addToCart(product.id)
  router.push('/cart')
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.home-page {
  min-height: 100%;
}

.hero-section {
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 80px 20px;
  text-align: center;
  color: white;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.hero-content {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 20px;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin-bottom: 40px;
}

.hero-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero-buttons .el-button {
  padding: 20px 40px;
  font-size: 16px;
}

.features-section {
  max-width: 1200px;
  margin: -40px auto 0;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

.feature-card {
  background: white;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.feature-card p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.products-section {
  max-width: 1200px;
  margin: 60px auto;
  padding: 0 20px;
}

.section-header {
  text-align: center;
  margin-bottom: 40px;
}

.section-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.section-header p {
  color: #909399;
  font-size: 14px;
}

.category-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.category-filter .el-tag {
  padding: 8px 20px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  color: #606266;
}

.category-filter .el-tag:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
  color: #409EFF;
}

.category-filter .el-tag.active {
  background: #409EFF;
  border-color: #409EFF;
  color: white;
}

.product-card {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  overflow: hidden;
}

.product-image img {
  position: absolute;
  top: 0;
  left: 0;
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

.product-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
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

.product-actions {
  display: flex;
  gap: 8px;
}

.product-actions .el-button {
  flex: 1;
  padding: 8px 0;
}

.more-link {
  text-align: center;
  margin-top: 40px;
}
</style>
