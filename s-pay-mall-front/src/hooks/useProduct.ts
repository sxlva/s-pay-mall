/**
 * 商品 Hook：封装 productStore 的响应式数据和方法
 *
 * @author 傅崇睿
 */

import { storeToRefs } from 'pinia'
import { useProductStore } from '@/stores/product'

/**
 * 商品组合式函数
 * @returns 商品响应式状态和方法
 */
export function useProduct() {
  const productStore = useProductStore()

  const {
    products,
    categories,
    loading,
    queryParams,
  } = storeToRefs(productStore)

  const loadData = productStore.loadData
  const loadProducts = productStore.loadProducts
  const loadCategories = productStore.loadCategories
  const setCategory = productStore.setCategory
  const setKeyword = productStore.setKeyword
  const setPriceRange = productStore.setPriceRange
  const setStatus = productStore.setStatus

  return {
    products,
    categories,
    loading,
    queryParams,
    loadData,
    loadProducts,
    loadCategories,
    setCategory,
    setKeyword,
    setPriceRange,
    setStatus,
  }
}