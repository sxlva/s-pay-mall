<template>
  <!-- 支付表单提交器组件
       职责：接收支付表单 HTML 字符串，自动渲染并提交表单
       架构原则：将 DOM 操作从 Hook 中剥离，使 Hook 保持纯粹的业务逻辑
  -->
  <div
    ref="containerRef"
    :style="{ display: visible ? 'block' : 'none' }"
  ></div>
</template>

<script setup lang="ts">
/**
 * PaymentSubmitter 组件
 * 
 * 用途：
 * - 接收后端返回的支付表单 HTML 字符串
 * - 自动将 HTML 注入到页面并提交表单
 * - 完成后自动隐藏
 * 
 * 使用场景：
 * - 支付宝/微信等第三方支付的表单提交
 * - 需要自动提交的支付场景
 */
import { ref, watch, onMounted, nextTick } from 'vue'

// ==================== Props 定义 ====================
interface Props {
  /** 支付表单 HTML 字符串（包含<form>标签） */
  payHtml?: string | null
  /** 是否自动提交（默认 true） */
  autoSubmit?: boolean
  /** 提交后是否保持可见（默认 false，提交后自动隐藏） */
  keepVisible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  payHtml: null,
  autoSubmit: true,
  keepVisible: false
})

// ==================== Emits 定义 ====================
interface Emits {
  /** 表单提交完成 */
  (e: 'submitted'): void
  /** 发生错误 */
  (e: 'error', error: string): void
}

const emit = defineEmits<Emits>()

// ==================== 状态定义 ====================
const visible = ref(false)
const containerRef = ref<HTMLElement | null>(null)

// ==================== 业务方法 ====================

/**
 * 渲染并提交支付表单
 * @param html 支付表单 HTML 字符串
 */
const submitPaymentForm = (html: string): void => {
  if (!html || html.trim() === '') {
    emit('error', '支付表单 HTML 为空')
    return
  }

  if (!containerRef.value) {
    emit('error', '容器元素未准备好')
    return
  }

  try {
    // 清空容器
    containerRef.value.innerHTML = ''
    
    // 创建临时容器解析 HTML
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = html
    
    // 查找表单元素
    const form = tempDiv.querySelector('form') as HTMLFormElement
    if (!form) {
      emit('error', '支付表单中未找到<form>元素')
      return
    }

    // 将表单移动到容器中
    containerRef.value.appendChild(form)
    
    // 显示容器（虽然表单会立即提交，但确保 DOM 已准备好）
    visible.value = true
    
    // 自动提交表单
    if (props.autoSubmit) {
      // 使用 nextTick 确保 DOM 已完全渲染
      setTimeout(() => {
        form.submit()
        emit('submitted')
        
        // 如果不需保持可见，隐藏组件
        if (!props.keepVisible) {
          visible.value = false
        }
      }, 100)
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '表单提交失败'
    emit('error', errorMessage)
  }
}

// ==================== 监听器 ====================

/**
 * 监听 payHtml 变化
 * 当 payHtml 有值时，自动渲染并提交表单
 */
watch(
  () => props.payHtml,
  (newHtml) => {
    if (newHtml && newHtml.includes('<form')) {
      nextTick(() => {
        submitPaymentForm(newHtml)
      })
    } else if (newHtml && !newHtml.includes('<form')) {
      emit('error', '支付链接格式不正确，未包含表单元素')
    }
  }
)

// ==================== 生命周期 ====================

onMounted(() => {
  // 组件挂载时检查是否需要立即提交
  // 使用 nextTick 确保 DOM 已经完全渲染
  if (props.payHtml) {
    nextTick(() => {
      submitPaymentForm(props.payHtml)
    })
  }
})

// ==================== 导出接口 ====================
defineExpose({
  /** 手动触发表单提交 */
  submitPaymentForm
})
</script>

<style scoped>
/* 组件完全隐藏，不影响页面布局 */
div[style*="display: none"] {
  display: none !important;
}
</style>
