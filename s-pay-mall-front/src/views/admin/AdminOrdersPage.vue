<template>
  <div class="admin-content">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>订单管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-card class="main-card" shadow="hover">
      <!-- 添加加载状态 -->
      <el-table :data="orders" stripe border v-loading="loading">
        <el-table-column prop="order_no" label="订单编号" min-width="180">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.order_no || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column prop="total_amount" label="订单金额" width="120">
          <template #default="scope">
            <span class="amount">¥{{ (scope.row.total_amount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="收货地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === 'PAID'">
              <el-button
                size="small"
                type="success"
                link
                @click="handleShip(scope.row)"
              >
                一键发货
              </el-button>
            </template>
            <template v-else-if="scope.row.status === 'CREATED' || scope.row.status === 'PENDING'">
              <el-button
                size="small"
                type="warning"
                link
                @click="handleCancel(scope.row)"
              >
                取消订单
              </el-button>
            </template>
            <template v-else-if="scope.row.status === 'SHIPPED'">
              <el-tag type="info">已发货</el-tag>
            </template>
            <template v-else-if="scope.row.status === 'DONE'">
              <el-tag type="success">已完成</el-tag>
            </template>
            <template v-else-if="scope.row.status === 'CANCELLED' || scope.row.status === 'CLOSE'">
              <el-tag type="danger">已取消</el-tag>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 显式的空状态兜底展示 -->
      <div v-if="!loading && orders.length === 0" class="empty-state">
        <el-empty description="暂无订单数据">
          <el-button type="primary" @click="loadOrders">刷新数据</el-button>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 管理后台订单管理：订单列表、一键发货、取消订单
 *
 * @author 傅崇睿
 */

import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrders, deliverOrder, cancelOrder } from '../../api/admin'

const orders = ref([])
const loading = ref(false)

const getStatusInfo = (status) => {
  const statusMap = {
    'CREATED': { label: '待付款', type: 'warning' },
    'INIT': { label: '待付款', type: 'warning' },
    'PAID': { label: '已付款', type: 'primary' },
    'SHIPPED': { label: '已发货', type: 'info' },
    'COMPLETED': { label: '已完成', type: 'success' },
    'DONE': { label: '已完成', type: 'success' },
    'CANCELED': { label: '已取消', type: 'danger' },
    'CLOSE': { label: '已取消', type: 'danger' },
    'CLOSED': { label: '已取消', type: 'danger' }
  }
  return statusMap[status] || { label: status, type: 'default' }
}

const getStatusLabel = (status) => getStatusInfo(status).label
const getStatusType = (status) => getStatusInfo(status).type

const loadOrders = async () => {
  try {
    loading.value = true
    const data = await getAdminOrders()
    // 防御性处理：确保返回数组
    orders.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取订单列表失败:', error)
    orders.value = [] // 异常时兜底为空数组
  } finally {
    loading.value = false
  }
}

const handleShip = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要一键发货吗？',
      '提示',
      { type: 'info', confirmButtonText: '确认发货', cancelButtonText: '取消' }
    )

    await deliverOrder(row.id)
    ElMessage.success('发货成功')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发货失败')
    }
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？',
      '警告',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' }
    )

    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.admin-content {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.main-card {
  border-radius: 8px;
}

.amount {
  color: #E6A23C;
  font-weight: 600;
  font-size: 16px;
}

.empty-state {
  padding: 40px;
}
</style>