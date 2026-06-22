<template>
  <router-view />
</template>

<script setup>
/**
 * 根组件：渲染路由视图，全局样式重置
 *
 * @author 傅崇睿
 */

// 导航栏由 Layout.vue 组件负责，登录页独立显示
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
}
</style>
