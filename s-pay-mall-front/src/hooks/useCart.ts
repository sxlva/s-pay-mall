import { useCartStore } from '../stores/cart'
import { storeToRefs } from 'pinia'

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
