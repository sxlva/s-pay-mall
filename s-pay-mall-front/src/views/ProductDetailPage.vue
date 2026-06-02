<!-- 页面说明：商品详情页 -->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { request } from '../utils/api'

const route = useRoute()
const product = ref(null)
const pid = computed(() => Number(route.params.id))

onMounted(async () => {
  const list = await request('/mall-api/v1/products')
  product.value = list.find((e) => e.id === pid.value)
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
    </div>
  </div>
</template>
