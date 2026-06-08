import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { CartItem } from '../types/cart'

const API_BASE = '/mall-api/v1'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)
  const updatingProductIds = ref<Set<number>>(new Set())

  const totalAmount = computed(() => {
    return items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.itemAmount, 0)
  })

  const totalCount = computed(() => {
    return items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.quantity, 0)
  })

  const allSelected = computed(() => {
    return items.value.length > 0 && items.value.every(item => item.selected)
  })

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token')
    return token ? { 'Authorization': `Bearer ${token}` } : {}
  }

  const loadCart = async (): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) {
      items.value = []
      return
    }
    try {
      loading.value = true
      const response = await fetch(`${API_BASE}/cart`, {
        headers: getAuthHeaders()
      })
      const data = await response.json()
      console.log('【购物车原始数据】', data) // 调试日志
      if (data.code === '0000') {
        const rawItems = data.data || []
        // 全兼容防守模式：深度追踪字段，避免 undefined
        items.value = rawItems.map((item: any) => {
          // 1. 深度追踪主键 ID（兼容 id, cartId, itemId）
          const safeId = Number(item.id || item.cartId || item.itemId || 0);
          // 2. 深度追踪商品单价（兼容 price, productPrice, skuPrice）
          const safePrice = Number(item.price || item.productPrice || item.skuPrice || 0);
          // 3. 深度追踪购买数量（兼容 quantity, count, num）
          const safeQuantity = Number(item.quantity || item.count || item.num || 0);
          
          return {
            id: safeId,
            productId: Number(item.productId || 0),
            productName: String(item.productName || item.title || '未知商品'),
            productPrice: safePrice,
            quantity: safeQuantity,
            selected: item.selected === undefined ? false : Boolean(item.selected),
            itemAmount: Number(item.itemAmount || (safePrice * safeQuantity) || 0),
            stock: Number(item.stock || 0)
          }
        });
        console.log('【购物车清洗后数据】', items.value) // 调试日志
      } else {
        items.value = []
      }
    } catch (error) {
      console.error('获取购物车失败:', error)
      items.value = [] // 异常时兜底为空数组，避免白屏
    } finally {
      loading.value = false
    }
  }

  const addToCart = async (productId: number, quantity: number = 1): Promise<boolean> => {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录')
      return false
    }
    try {
      loading.value = true
      const response = await fetch(`${API_BASE}/cart`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ productId, quantity })
      })
      const data = await response.json()
      if (data.code === '0000') {
        ElMessage.success('商品已成功加入购物车')
        await loadCart()
        return true
      } else {
        ElMessage.error(data.info || '添加失败')
        return false
      }
    } catch (error) {
      ElMessage.error('添加失败')
      return false
    } finally {
      loading.value = false
    }
  }

  const updateQuantity = async (productId: number, quantity: number): Promise<void> => {
    const token = localStorage.getItem('token')
    const safeQuantity = Number(quantity)
    if (!token || isNaN(safeQuantity) || safeQuantity < 1) {
      throw new Error('参数无效')
    }
    
    if (updatingProductIds.value.has(productId)) {
      throw new Error('正在更新中，请稍后')
    }
    
    updatingProductIds.value.add(productId)
    try {
      const response = await fetch(`${API_BASE}/cart/quantity`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ productId, quantity: safeQuantity })
      })
      const data = await response.json()
      if (data.code === '0000') {
        await loadCart()
      } else {
        throw new Error(data.info || '更新失败')
      }
    } catch (error) {
      throw error
    } finally {
      updatingProductIds.value.delete(productId)
    }
  }

  const removeItem = async (cartItemId: number): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      console.log('【删除购物车项】itemId:', cartItemId) // 调试日志
      const response = await fetch(`${API_BASE}/cart/delete?itemId=${cartItemId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      })
      const data = await response.json()
      console.log('【删除响应】', data) // 调试日志
      if (data.code === '0000') {
        ElMessage.success('删除成功')
        await loadCart()
      } else {
        ElMessage.error(data.info || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error) // 调试日志
      ElMessage.error('删除失败')
    }
  }

  const toggleSelect = (cartItemId: number): void => {
    const item = items.value.find(i => i.id === cartItemId)
    if (item) {
      item.selected = !item.selected
    }
  }

  const toggleAll = (): void => {
    const newSelected = !allSelected.value
    items.value.forEach(item => {
      item.selected = newSelected
    })
  }

  const clearCart = async (): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      const response = await fetch(`${API_BASE}/cart`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      })
      const data = await response.json()
      if (data.code === '0000') {
        ElMessage.success('购物车已清空')
        await loadCart()
      }
    } catch (error) {
      ElMessage.error('清空失败')
    }
  }

  const getSelectedItems = (): CartItem[] => {
    return items.value.filter(item => item.selected)
  }

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
})
