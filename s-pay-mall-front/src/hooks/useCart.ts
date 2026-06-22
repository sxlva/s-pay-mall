/**
 * 购物车 Hook：封装 cartStore 的响应式数据和方法
 *
 * @author 傅崇睿
 */

import { useCartStore } from '../stores/cart'
import { storeToRefs } from 'pinia'

/**
 * 购物车组合式函数
 * @returns 购物车响应式状态和方法
 */
export function useCart() {
  const cartStore = useCartStore()
  const { items, loading, totalAmount, totalCount, allSelected } = storeToRefs(cartStore)

  const loadCart = cartStore.loadCart
  const addToCart = cartStore.addToCart
  const updateQuantity = cartStore.updateQuantity
  const removeItem = cartStore.removeItem
  const toggleSelect = cartStore.toggleSelect
  const toggleAll = cartStore.toggleAll
  const clearCart = cartStore.clearCart
  const getSelectedItems = cartStore.getSelectedItems

  return {
    items,
    loading,
    totalAmount,
    totalCount,
    allSelected,
    loadCart,
    addToCart,
    updateQuantity,
    removeItem,
    toggleSelect,
    toggleAll,
    clearCart,
    getSelectedItems,
  }
}
