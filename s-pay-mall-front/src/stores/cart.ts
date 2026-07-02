/**
 * 购物车状态管理 Store
 * 职责：状态管理、计算属性、业务逻辑编排
 * DDD 分层：Application/Domain Layer（应用层/领域层）
 *
 * 架构原则：
 * - 不直接处理 HTTP 请求（委托给 cartRepository）
 * - 不处理数据清洗（由 cartRepository 完成）
 * - 只关注状态变化和用户交互逻辑
 *
 * @author 傅崇睿
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cartRepository } from '../repositories/cartRepository'
import type { CartItem } from '../types/domain/cart'

export const useCartStore = defineStore('cart', () => {
  // ==================== 状态定义 ====================
  const items = ref<CartItem[]>([])
  const loading = ref(false)
  const updatingProductIds = ref<Set<number>>(new Set())

  // ==================== 计算属性 ====================
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

  // ==================== 业务方法 ====================
  
  /**
   * 加载购物车数据
   * - 未登录时清空购物车
   * - 调用仓储层获取数据
   * - 异常时兜底为空数组
   */
  const loadCart = async (): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) {
      items.value = []
      return
    }
    
    try {
      loading.value = true
      items.value = await cartRepository.fetchItems()
    } catch (error) {
      console.error('获取购物车失败:', error)
      items.value = [] // 异常时兜底为空数组，避免白屏
    } finally {
      loading.value = false
    }
  }

  /**
   * 添加商品到购物车
   * - 检查登录状态
   * - 调用仓储层添加
   * - 成功后刷新购物车
   */
  const addToCart = async (productId: number, quantity: number = 1): Promise<boolean> => {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录')
      return false
    }
    
    try {
      loading.value = true
      await cartRepository.add({ productId, quantity })
      ElMessage.success('商品已成功加入购物车')
      await loadCart()
      return true
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '添加失败')
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新购物车商品数量
   * - 参数校验
   * - 防止重复更新
   * - 调用仓储层更新
   */
  const updateQuantity = async (productId: number, quantity: number): Promise<void> => {
    const safeQuantity = Number(quantity)
    const token = localStorage.getItem('token')
    
    if (!token || isNaN(safeQuantity) || safeQuantity < 1) {
      throw new Error('参数无效')
    }
    
    if (updatingProductIds.value.has(productId)) {
      throw new Error('正在更新中，请稍后')
    }
    
    updatingProductIds.value.add(productId)
    try {
      await cartRepository.updateQuantity({ productId, quantity: safeQuantity })
      await loadCart()
    } catch (error) {
      throw error
    } finally {
      updatingProductIds.value.delete(productId)
    }
  }

  /**
   * 删除购物车项
   * - 调用仓储层删除
   * - 成功后刷新购物车
   */
  const removeItem = async (cartItemId: number): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) return
    
    try {
      console.log('【删除购物车项】itemId:', cartItemId)
      await cartRepository.delete(cartItemId)
      ElMessage.success('删除成功')
      await loadCart()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  /**
   * 切换商品选中状态
   * - 纯内存操作，无需调用 API
   */
  const toggleSelect = (cartItemId: number): void => {
    const item = items.value.find(i => i.id === cartItemId)
    if (item) {
      item.selected = !item.selected
    }
  }

  /**
   * 全选/取消全选
   * - 纯内存操作，无需调用 API
   */
  const toggleAll = (): void => {
    const newSelected = !allSelected.value
    items.value.forEach(item => {
      item.selected = newSelected
    })
  }

  /**
   * 清空购物车
   * - 调用仓储层清空
   * - 成功后刷新购物车
   */
  const clearCart = async (): Promise<void> => {
    const token = localStorage.getItem('token')
    if (!token) return
    
    try {
      await cartRepository.clear()
      ElMessage.success('购物车已清空')
      await loadCart()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '清空失败')
    }
  }

  /**
   * 获取已选中的商品
   * - 纯计算逻辑，无需调用 API
   */
  const getSelectedItems = (): CartItem[] => {
    return items.value.filter(item => item.selected)
  }

  // ==================== 导出接口 ====================
  return {
    // 状态
    items,
    loading,
    
    // 计算属性
    totalAmount,
    totalCount,
    allSelected,
    
    // 业务方法
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