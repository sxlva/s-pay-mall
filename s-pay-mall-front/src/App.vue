<template>
  <router-view />
</template>

<script setup>
// App.vue 作为根组件，只负责渲染路由视图
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
