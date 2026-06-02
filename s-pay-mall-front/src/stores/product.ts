import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ProductVO, CategoryVO, ProductQueryParams } from '@/types/product'

const API_BASE = '/mall-api/v1'

export const useProductStore = defineStore('product', () => {
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

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token')
    return token ? { 'Authorization': `Bearer ${token}` } : {}
  }

  const loadCategories = async () => {
    try {
      const response = await fetch(`${API_BASE}/categories`, {
        headers: getAuthHeaders()
      })
      const data = await response.json()
      if (data.code === '0000') {
        categories.value = data.data
      }
    } catch (error) {
      console.error('获取分类失败:', error)
    }
  }

  const loadProducts = async () => {
    try {
      loading.value = true
      const url = new URL(`${API_BASE}/products`, window.location.origin)

      // 直接传递 categoryId 到后端进行强过滤，严禁在前端做逻辑映射
      if (queryParams.value.categoryId !== null && queryParams.value.categoryId !== undefined) {
        url.searchParams.set('categoryId', queryParams.value.categoryId.toString())
      }

      if (queryParams.value.keyword) {
        url.searchParams.set('keyword', queryParams.value.keyword)
      }

      if (queryParams.value.minPrice !== undefined) {
        url.searchParams.set('minPrice', queryParams.value.minPrice.toString())
      }

      if (queryParams.value.maxPrice !== undefined) {
        url.searchParams.set('maxPrice', queryParams.value.maxPrice.toString())
      }

      if (queryParams.value.status !== undefined) {
        url.searchParams.set('status', queryParams.value.status.toString())
      }

      const response = await fetch(url.toString(), {
        headers: getAuthHeaders()
      })
      const data = await response.json()
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

  const loadData = async () => {
    if (initialized.value) {
      await loadProducts()
      return
    }
    await loadCategories()
    await loadProducts()
    initialized.value = true
  }

  const setCategory = (categoryId: number | null) => {
    queryParams.value.categoryId = categoryId
  }

  const setKeyword = (keyword: string) => {
    queryParams.value.keyword = keyword
  }

  const setPriceRange = (minPrice?: number, maxPrice?: number) => {
    queryParams.value.minPrice = minPrice
    queryParams.value.maxPrice = maxPrice
  }

  const setStatus = (status?: number) => {
    queryParams.value.status = status
  }

  watch(queryParams, () => {
    loadProducts()
  }, { deep: true })

  return {
    products,
    categories,
    loading,
    queryParams,
    initialized,
    loadProducts,
    loadCategories,
    loadData,
    setCategory,
    setKeyword,
    setPriceRange,
    setStatus,
  }
})