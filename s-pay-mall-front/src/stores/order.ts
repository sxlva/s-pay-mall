/**
 * 订单状态管理 Store
 * 职责：状态管理、计算属性、业务逻辑编排
 * DDD 分层：Application/Domain Layer（应用层/领域层）
 *
 * 架构原则：
 * - 不直接处理 HTTP 请求（委托给 orderRepository）
 * - 不处理数据清洗（由 orderRepository 完成）
 * - 只关注状态变化和用户交互逻辑
 *
 * @author 傅崇睿
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { orderRepository } from '@/repositories/orderRepository'
import type { Order, OrderCreateResult, OrderListParams } from '@/types/order'

export const useOrderStore = defineStore('order', () => {
  // ==================== 状态定义 ====================
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const loading = ref(false)

  // ==================== 计算属性 ====================
  const orderCount = computed(() => orders.value.length)

  const ordersByStatus = computed(() => {
    return (status: string) => orders.value.filter(order => order.status === status)
  })

  const pendingOrders = computed(() => orders.value.filter(order => order.status === 'INIT'))
  const paidOrders = computed(() => orders.value.filter(order => order.status === 'PAID'))
  const shippedOrders = computed(() => orders.value.filter(order => order.status === 'SHIPPED'))
  const completedOrders = computed(() => orders.value.filter(order => order.status === 'DONE'))
  const canceledOrders = computed(() => orders.value.filter(order => order.status === 'CANCELED'))

  // ==================== 业务方法 ====================
  
  /**
   * 加载订单列表
   * - 调用仓储层获取数据
   * - 异常时抛出错误
   */
  const loadOrders = async (params?: OrderListParams): Promise<void> => {
    loading.value = true
    try {
      orders.value = await orderRepository.fetchOrders(params)
    } catch (error) {
      console.error('加载订单列表失败:', error)
      ElMessage.error('加载订单列表失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建新订单
   * - 调用仓储层创建订单
   * - 处理支付表单 HTML 返回
   * - 成功后刷新订单列表
   * @returns 支付表单 HTML 字符串（如需立即支付）或 null
   */
  const createNewOrder = async (address: string): Promise<string | null> => {
    loading.value = true
    try {
      const result = await orderRepository.create(address)
      
      // 检查是否需要立即支付（返回支付表单 HTML）
      const payUrl = result.payUrl || result._html || result.html
      if (typeof payUrl === 'string' && payUrl.includes('<form')) {
        return payUrl
      }
      
      // 无需立即支付，刷新订单列表
      await loadOrders()
      return null
    } catch (error) {
      console.error('创建订单失败:', error)
      ElMessage.error('创建订单失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 设置当前订单
   * - 纯内存操作，无需调用 API
   */
  const setCurrentOrder = (order: Order | null): void => {
    currentOrder.value = order
  }

  /**
   * 清空订单列表
   * - 纯内存操作，无需调用 API
   */
  const clearOrders = (): void => {
    orders.value = []
    currentOrder.value = null
  }

  // ==================== 导出接口 ====================
  return {
    // 状态
    orders,
    currentOrder,
    loading,
    
    // 计算属性
    orderCount,
    ordersByStatus,
    pendingOrders,
    paidOrders,
    shippedOrders,
    completedOrders,
    canceledOrders,
    
    // 业务方法
    loadOrders,
    createNewOrder,
    setCurrentOrder,
    clearOrders,
  }
})