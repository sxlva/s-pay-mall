<template>
  <div class="orders-container">
    <!-- 支付表单提交器组件 -->
    <PaymentSubmitter
      v-if="showPaymentSubmitter"
      :pay-html="currentPayHtml"
      :auto-submit="true"
      :keep-visible="false"
      @submitted="handlePaymentSubmitted"
      @error="handlePaymentError"
    />

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
            <span class="order-no">订单号：{{ order.orderNo }}</span>
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
/**
 * 订单列表页：订单展示、状态筛选、继续支付
 *
 * @author 傅崇睿
 */

import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Box, Ticket } from '@element-plus/icons-vue';
import { useOrder } from '../hooks/useOrder';
import { usePayment } from '../hooks/usePayment';
import { orderRepository } from '../repositories/orderRepository';
import { checkStock, type StockCheckResult } from '../api/order';
import PaymentSubmitter from '../components/PaymentSubmitter.vue';
import type { OrderStatus } from '../types/domain/order';
import type { Order } from '../types/domain/order';

const router = useRouter();
const { orders, loading, loadOrders } = useOrder();
const { initPayOrder, startPolling, extractPaymentForm } = usePayment();

const payingOrderId = ref<number | null>(null);
const showPaymentSubmitter = ref(false);
const currentPayHtml = ref<string | null>(null);

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

/**
 * 处理支付表单提交完成事件
 */
const handlePaymentSubmitted = () => {
  ElMessage.success('正在跳转至支付宝安全支付页面...');
};

/**
 * 处理支付表单错误事件
 */
const handlePaymentError = (error: string) => {
  ElMessage.error(error || '支付处理失败');
};

/**
 * 继续支付处理函数
 * - 先调用 checkStock 接口进行后端库存同步校验
 * - 如果库存不足，弹窗提示用户
 * - 如果库存充足，继续执行支付流程
 */
const handleContinuePay = async (order: Order) => {
  if (payingOrderId.value) return;

  payingOrderId.value = order.id;

  try {
    console.log('【继续支付】order 对象:', order);
    console.log('【继续支付】orderNo:', order.orderNo);

    if (!order.orderNo) {
      ElMessage.error('订单号无效，无法继续支付');
      return;
    }

    ElMessage.info('正在校验库存...');

    const stockResult: StockCheckResult = await checkStock(order.orderNo);

    if (!stockResult.success) {
      await ElMessageBox.alert(
        stockResult.message || '库存不足，无法继续支付',
        '库存校验失败',
        {
          confirmButtonText: '确定',
          type: 'warning'
        }
      );
      return;
    }

    const result = await orderRepository.continuePay(order.orderNo);

    const payHtml = result.payUrl || result._html || result.html;

    if (payHtml && payHtml.includes('<form')) {
      initPayOrder({
        orderNo: order.orderNo,
        totalAmount: order.totalAmount,
        status: 'PAYING',
        payUrl: payHtml
      });

      currentPayHtml.value = payHtml;
      showPaymentSubmitter.value = true;

      startPolling(order.orderNo, 180);
    } else {
      ElMessage.error('支付链接无效');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('继续支付失败:', error);
      ElMessage.error(error instanceof Error ? error.message : '支付请求失败');
    }
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
