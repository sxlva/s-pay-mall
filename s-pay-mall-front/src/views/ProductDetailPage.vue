<script setup lang="ts">
/**
 * 商品详情页：展示商品信息、库存状态、加入购物车
 *
 * @author 傅崇睿
 */

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchProduct } from '../api/product'
import { isSoldOut } from '../utils/product'
import type { ProductVO } from '../types/domain/product'

const route = useRoute()
const product = ref<ProductVO | null>(null)
const pid = computed(() => Number(route.params.id))

onMounted(async () => {
  product.value = await fetchProduct(pid.value)
})
</script>

<template>
  <div>
    <h3>商品详情</h3>
    <p v-if="!product">加载中...</p>
    <div v-else>
      <p>名称：{{ product.name }}</p>
      <p>描述：{{ product.description }}</p>
      <p>价格：￥{{ product.price }}</p>
      <p>库存：{{ product.stock }}</p>
      
      <el-tag v-if="isSoldOut(product.stock)" type="danger" size="large">售罄</el-tag>
      
      <div style="margin-top: 20px;">
        <el-button 
          type="primary" 
          :disabled="isSoldOut(product.stock)"
          @click="handleAddToCart"
        >
          {{ isSoldOut(product.stock) ? '已售罄' : '加入购物车' }}
        </el-button>
        <el-button 
          type="success" 
          :disabled="isSoldOut(product.stock)"
          @click="handleBuyNow"
        >
          {{ isSoldOut(product.stock) ? '已售罄' : '立即购买' }}
        </el-button>
      </div>
    </div>
  </div>
</template>
