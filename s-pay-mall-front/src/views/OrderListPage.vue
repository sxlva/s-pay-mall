<template>
  <div class="orders-container">
    <el-card class="header-card" shadow="hover">
      <template #header>
        <div class="header-content">
          <div class="icon-wrapper">
            <el-icon class="icon"><Box /></el-icon>
          </div>
          <div class="text-wrapper">
            <h2 class="title">我的订单</h2>
            <p class="subtitle">查看您的所有订单</p>
          </div>
        </div>
      </template>
    </el-card>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="4" animated />
    </div>

    <el-empty v-else-if="orders.length === 0" description="您还没有任何订单">
      <el-button type="primary" @click="$router.push('/products')">去购物</el-button>
    </el-empty>

    <el-card v-else v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
      <div class="order-header">
        <div class="order-info">
          <div class="order-no-row">
            <el-icon class="order-icon"><Ticket /></el-icon>
            <span class="order-no">订单号：{{ order.orderId }}</span>
          </div>
          <span class="create-time">{{ formatTime(order.createTime) }}</span>
        </div>
        <el-tag :type="getStatusType(order.status)" size="small" class="status-tag">
          {{ order.statusDesc }}
        </el-tag>
      </div>

      <el-divider class="divider" />

      <div class="order-body">
        <div class="summary-item">
          <span class="summary-label">收货地址</span>
          <span class="summary-value">{{ order.address || '未填写' }}</span>
        </div>
      </div>

      <div class="order-footer">
        <div class="footer-left">
          <span class="order-count">共 1 件商品</span>
        </div>
        <div class="footer-right">
          <span class="total-label">合计：</span>
          <span class="total-amount">¥{{ formatAmount(order.totalAmount) }}</span>
          <el-button
            v-if="isWaitPay(order.status)"
            type="danger"
            size="small"
            class="pay-btn"
            :loading="payingOrderId === order.id"
            @click="handleContinuePay(order)"
          >
            继续支付
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Box, Ticket } from '@element-plus/icons-vue';
import { useOrder } from '../hooks/useOrder';
import { usePayment } from '../hooks/usePayment';
import type { OrderStatus } from '../types/order';

const router = useRouter();
const { orders, loading, loadOrders } = useOrder();
const { initPayOrder, redirectToPay, startPolling } = usePayment();

const payingOrderId = ref<number | null>(null);

const isWaitPay = (status: OrderStatus) => {
  return status === 'CREATED' || status === 'INIT' || status === 'WAIT_PAY';
};

const getStatusType = (status: OrderStatus) => {
  const statusMap: Record<OrderStatus, string> = {
    'INIT': 'warning',
    'PAID': 'success',
    'SHIPPED': 'primary',
    'DONE': 'info',
    'CANCELED': 'danger',
    'CREATED': 'warning',
    'WAIT_PAY': 'warning'
  };
  return statusMap[status] || 'default';
};

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  return timeStr.replace('T', ' ').substring(0, 19);
};

const formatAmount = (amount: number) => {
  if (!amount) return '0.00';
  return amount.toFixed(2);
};

const handleContinuePay = async (order: any) => {
  if (payingOrderId.value) return;

  payingOrderId.value = order.id;

  try {
    const response = await fetch(`/mall-api/v1/orders/${order.orderId}/continue-pay`);
    const result = await response.json();

    if (result.code === 200 && result.data) {
      const payUrl = result.data.payUrl || result.data._html;

      if (payUrl && typeof payUrl === 'string' && payUrl.includes('<form')) {
        initPayOrder({
          orderNo: order.orderId,
          totalAmount: order.totalAmount,
          status: 'PAYING',
          payUrl: payUrl
        });

        ElMessage.success('正在跳转至支付宝安全支付页面...');
        redirectToPay(payUrl);
        startPolling(order.orderId, 180);
      } else {
        ElMessage.error('支付链接无效');
      }
    } else {
      ElMessage.error(result.message || '支付失败');
    }
  } catch (error) {
    console.error('继续支付失败:', error);
    ElMessage.error('支付跳转失败，请稍后重试');
  } finally {
    payingOrderId.value = null;
  }
};

onMounted(async () => {
  try {
    await loadOrders();
  } catch (error) {
    console.error('获取订单失败:', error);
    ElMessage.error('获取订单失败');
    router.push('/login');
  }
});
</script>

<style scoped>
.orders-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.header-card {
  border-radius: 16px;
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-wrapper {
  width: 40px;
  height: 40px;
  background-color: #E8F4FD;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  font-size: 20px;
  color: #409EFF;
}

.text-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.loading-container {
  padding: 64px;
  margin-bottom: 20px;
}

.order-card {
  border-radius: 12px;
  margin-bottom: 16px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-icon {
  font-size: 14px;
  color: #409EFF;
}

.order-no {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.create-time {
  font-size: 12px;
  color: #909399;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 4px;
}

.divider {
  margin: 0;
}

.order-body {
  padding: 0 16px 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  font-size: 13px;
  color: #606266;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fafafa;
}

.footer-left {
  display: flex;
  align-items: center;
}

.order-count {
  font-size: 13px;
  color: #909399;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-label {
  font-size: 14px;
  color: #606266;
}

.total-amount {
  font-size: 20px;
  font-weight: 700;
  color: #E6A23C;
}

.pay-btn {
  margin-left: 8px;
}

:deep(.el-empty) {
  padding: 60px 0;
}

:deep(.el-divider) {
  margin: 0;
}
</style>
