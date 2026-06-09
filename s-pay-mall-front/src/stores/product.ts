/**
 * 商品状态管理 Store
 * 职责：状态管理、计算属性、业务逻辑编排
 * DDD 分层：Application/Domain Layer（应用层/领域层）
 * 
 * 架构原则：
 * - 不直接处理 HTTP 请求（委托给 productRepository）
 * - 不处理数据清洗（由 productRepository 完成）
 * - 只关注状态变化和用户交互逻辑
 */
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { productRepository } from '@/repositories/productRepository'
import type { ProductVO, CategoryVO, ProductQueryParams } from '@/types/product'

export const useProductStore = defineStore('product', () => {
  // ==================== 状态定义 ====================
  const products = ref<ProductVO[]>([])
  const categories = ref<CategoryVO[]>([])
  const loading = ref(false)
  const queryParams = ref<ProductQueryParams>({
    categoryId: null,
    keyword: '',
    minPrice: undefined,
    maxPrice: undefined,
    status: undefined,
  })
  const initialized = ref(false)

  // ==================== 业务方法 ====================
  
  /**
   * 加载分类列表
   * - 调用仓储层获取数据
   * - 异常时兜底为空数组
   */
  const loadCategories = async () => {
    try {
      categories.value = await productRepository.fetchCategories()
    } catch (error) {
      console.error('获取分类失败:', error)
      categories.value = [] // 异常时兜底为空数组
    }
  }

  /**
   * 加载商品列表
   * - 调用仓储层获取数据
   * - 异常时显示错误提示
   */
  const loadProducts = async () => {
    try {
      loading.value = true
      products.value = await productRepository.fetchProducts(queryParams.value)
    } catch (error) {
      console.error('获取商品失败:', error)
      ElMessage.error('获取商品失败')
      products.value = [] // 异常时兜底为空数组
    } finally {
      loading.value = false
    }
  }

  /**
   * 初始化数据（仅首次加载）
   * - 先加载分类
   * - 再加载商品
   * - 标记已初始化
   */
  const loadData = async () => {
    if (initialized.value) {
      await loadProducts()
      return
    }
    await loadCategories()
    await loadProducts()
    initialized.value = true
  }

  /**
   * 设置分类 ID
   * - 更新查询参数
   * - 自动触发商品列表刷新（通过 watch）
   */
  const setCategory = (categoryId: number | null) => {
    queryParams.value.categoryId = categoryId
  }

  /**
   * 设置搜索关键词
   * - 更新查询参数
   * - 自动触发商品列表刷新（通过 watch）
   */
  const setKeyword = (keyword: string) => {
    queryParams.value.keyword = keyword
  }

  /**
   * 设置价格区间
   * - 更新查询参数
   * - 自动触发商品列表刷新（通过 watch）
   */
  const setPriceRange = (minPrice?: number, maxPrice?: number) => {
    queryParams.value.minPrice = minPrice
    queryParams.value.maxPrice = maxPrice
  }

  /**
   * 设置商品状态筛选
   * - 更新查询参数
   * - 自动触发商品列表刷新（通过 watch）
   */
  const setStatus = (status?: number) => {
    queryParams.value.status = status
  }

  // ==================== 响应式监听 ====================
  watch(queryParams, () => {
    loadProducts()
  }, { deep: true })

  // ==================== 导出接口 ====================
  return {
    // 状态
    products,
    categories,
    loading,
    queryParams,
    initialized,
    
    // 业务方法
    loadProducts,
    loadCategories,
    loadData,
    setCategory,
    setKeyword,
    setPriceRange,
    setStatus,
  }
})