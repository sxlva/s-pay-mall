import { storeToRefs } from 'pinia'
import { useProductStore } from '@/stores/product'

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