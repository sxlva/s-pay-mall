<script setup lang="ts">
/**
 * 结算页：地址填写 + 订单确认 + 支付跳转
 *
 * @author 傅崇睿
 */

import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MapLocation, CreditCard, ArrowLeft } from '@element-plus/icons-vue';
import { useCart } from '../hooks/useCart';
import { useOrder } from '../hooks/useOrder';
import { usePayment } from '../hooks/usePayment';

const router = useRouter();
const address = ref('');
const addressError = ref('');
const isSubmitting = ref(false);

const { items: cartItems, totalAmount, totalCount, loadCart } = useCart();
const { createNewOrder } = useOrder();
const {
  payUrl,
  pollingState,
  countdown,
  error,
  isPolling,
  isPaid,
  isFailed,
  startPolling,
  stopPolling,
  initPayOrder,
  resetPayment,
  redirectToPay
} = usePayment();

const shippingFee = '0.00';
const discount = '0.00';

const paymentStatusText = computed(() => {
  switch (pollingState.value) {
    case 'idle':
      return '准备支付';
    case 'polling':
      return `支付中... ${countdown.value}s`;
    case 'success':
      return '支付成功';
    case 'failed':
      return '支付失败';
    case 'timeout':
      return '支付超时';
    default:
      return '';
  }
});

const showQRCode = computed(() => isPolling.value && payUrl.value);
const showSuccess = computed(() => isPaid.value);
const showFailed = computed(() => isFailed.value);

const submitOrder = async () => {
  if (!address.value.trim()) {
    addressError.value = '请输入收货地址';
    return;
  }

  addressError.value = '';
  isSubmitting.value = true;
  resetPayment();

  try {
    const payUrlResult = await createNewOrder(address.value);

    if (payUrlResult) {
      initPayOrder({
        orderNo: '',
        totalAmount: totalAmount.value,
        status: 'PAYING',
        payUrl: payUrlResult
      });

      ElMessage.success('正在跳转至支付宝沙箱安全支付页面...');
      redirectToPay(payUrlResult);
      startPolling('', 180);
    } else {
      ElMessage.success('订单提交成功');
      router.push('/orders');
    }
  } catch (err) {
    console.error('订单支付通信失败', err);
    addressError.value = '支付跳转失败，请确保后端服务和数据库连接正常';
  } finally {
    isSubmitting.value = false;
  }
};

const handleRetry = () => {
  resetPayment();
  address.value = '';
};

const handleBackToCart = () => {
  stopPolling();
  router.push('/cart');
};

onMounted(() => {
  loadCart();
});

onUnmounted(() => {
  stopPolling();
});
</script>

<template>
  <div class="checkout-page">
    <div class="page-header">
      <div class="header-content">
        <button @click="handleBackToCart" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div>
          <h1 class="page-title">确认订单</h1>
          <p class="page-subtitle">请确认您的订单信息</p>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="main-content">
      <el-col :span="16" class="left-section">
        <el-card class="address-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><MapLocation /></el-icon>
              <span class="header-title">收货人信息</span>
            </div>
          </template>
          <div class="address-form">
            <textarea
              v-model="address"
              :disabled="isPolling"
              placeholder="请输入详细收货地址（包括省市区街道门牌号）"
              rows="3"
              class="address-input"
            ></textarea>
            <span v-if="addressError" class="error-text">{{ addressError }}</span>
          </div>
        </el-card>

        <el-card class="products-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">商品清单</span>
              <span class="header-count">共 {{ totalCount }} 件商品</span>
            </div>
          </template>
          <el-table :data="cartItems" border stripe class="products-table">
            <el-table-column prop="productName" label="商品名称" min-width="200">
              <template #default="scope">
                <div class="product-cell">
                  <img
                    :src="`https://picsum.photos/60?random=${scope.row.productId}`"
                    :alt="scope.row.productName"
                    class="product-image"
                  />
                  <span class="product-name">{{ scope.row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="productPrice" label="单价" width="100">
              <template #default="scope">
                <span class="price-text">¥{{ scope.row.productPrice.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column label="小计" width="100">
              <template #default="scope">
                <span class="price-text">¥{{ scope.row.itemAmount.toFixed(2) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8" class="right-section">
        <el-card class="summary-card" shadow="hover" body-style="padding: 0">
          <div class="summary-header">
            <span class="summary-title">订单总计</span>
          </div>
          <div class="summary-content">
            <div class="summary-row">
              <span class="row-label">商品总额</span>
              <span class="row-value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>
            <div class="summary-row">
              <span class="row-label">运费</span>
              <span class="row-value">¥{{ shippingFee }}</span>
            </div>
            <div class="summary-row discount">
              <span class="row-label">优惠金额</span>
              <span class="row-value discount-value">-¥{{ discount }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-total">
              <span class="total-label">实付金额</span>
              <span class="total-value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>

            <div v-if="isPolling || isPaid || isFailed" class="payment-status">
              <el-divider />
              <div class="status-info">
                <span class="status-label">支付状态：</span>
                <el-tag :type="isPaid ? 'success' : isFailed ? 'danger' : 'warning'">
                  {{ paymentStatusText }}
                </el-tag>
              </div>
              <div v-if="isFailed && error" class="error-info">
                <span class="error-message">{{ error }}</span>
              </div>
            </div>
          </div>
          <div class="summary-footer">
            <el-button
              v-if="!isPolling && !isPaid"
              type="danger"
              size="large"
              :loading="isSubmitting"
              class="submit-btn"
              @click="submitOrder"
            >
              <CreditCard class="btn-icon" />
              提交订单并使用支付宝支付
            </el-button>
            <el-button
              v-if="isFailed"
              type="primary"
              size="large"
              class="submit-btn"
              @click="handleRetry"
            >
              重新下单
            </el-button>
            <el-button
              v-if="isPaid"
              type="success"
              size="large"
              class="submit-btn"
              @click="router.push('/orders')"
            >
              查看订单
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40px;
}

.page-header {
  background: white;
  padding: 16px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  transition: all 0.3s;
}

.back-btn:hover {
  background: #e4e8eb;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 4px 0 0;
}

.main-content {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.left-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.address-card,
.products-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-icon {
  font-size: 18px;
  margin-right: 8px;
  color: #409EFF;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-count {
  font-size: 14px;
  color: #909399;
}

.address-form {
  padding-top: 8px;
}

.address-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

.address-input:focus {
  outline: none;
  border-color: #409EFF;
}

.address-input:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.error-text {
  display: block;
  font-size: 13px;
  color: #f56c6c;
  margin-top: 8px;
}

.products-table {
  --el-table-header-text-color: #606266;
  --el-table-row-hover-bg-color: #fafafa;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-image {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-name {
  font-weight: 500;
  color: #303133;
}

.price-text {
  color: #f56c6c;
  font-weight: 500;
}

.right-section {
  position: sticky;
  top: 20px;
  height: fit-content;
}

.summary-card {
  border-radius: 12px;
  overflow: hidden;
}

.summary-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.summary-content {
  padding: 16px 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.row-label {
  font-size: 14px;
  color: #606266;
}

.row-value {
  font-size: 14px;
  color: #303133;
}

.discount-value {
  color: #67c23a;
}

.summary-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 16px 0;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-label {
  font-size: 14px;
  color: #606266;
}

.total-value {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

.payment-status {
  margin-top: 16px;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.status-label {
  font-size: 14px;
  color: #606266;
}

.error-info {
  margin-top: 8px;
}

.error-message {
  font-size: 13px;
  color: #f56c6c;
}

.summary-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
}

.btn-icon {
  margin-right: 8px;
}
</style>