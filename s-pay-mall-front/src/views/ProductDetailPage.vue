<script setup>
/**
 * 商品详情页：展示商品信息、库存状态、加入购物车
 *
 * @author 傅崇睿
 */

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '../utils/api'
import { isSoldOut, normalizeProduct } from '../utils/product'

const route = useRoute()
const product = ref(null)
const pid = computed(() => Number(route.params.id))

onMounted(async () => {
  const list = await request('/mall-api/v1/products')
  const matched = list.find((e) => e.id === pid.value)
  product.value = matched ? normalizeProduct(matched) : null
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
