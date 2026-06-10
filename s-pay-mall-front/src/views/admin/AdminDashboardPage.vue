<template>
  <div class="dashboard-content">
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="用户总数" :value="stats.users">
            <template #prefix>
              <el-icon class="stat-icon" color="#409EFF"><User /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="商品数量" :value="stats.products">
            <template #prefix>
              <el-icon class="stat-icon" color="#67C23A"><Goods /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="订单总数" :value="stats.orders">
            <template #prefix>
              <el-icon class="stat-icon" color="#E6A23C"><ShoppingCart /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="分类数量" :value="stats.categories">
            <template #prefix>
              <el-icon class="stat-icon" color="#909399"><Folder /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>最近7天销售额走势</span>
            </div>
          </template>
          <div ref="salesChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="chart-header">
              <span>商品分类占比</span>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  User, 
  Goods, 
  ShoppingCart, 
  Folder
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { 
  getAdminUsers, 
  getAdminProducts, 
  getAdminOrders, 
  getAdminCategories,
  getSalesTrend,
  getCategoryRatio
} from '../../api/admin'

const stats = reactive({
  users: 0,
  products: 0,
  orders: 0,
  categories: 0
})

const salesChartRef = ref(null)
const categoryChartRef = ref(null)
let salesChart = null
let categoryChart = null

const loadStats = async () => {
  try {
    const [usersData, productsData, ordersData, categoriesData] = await Promise.all([
      getAdminUsers(),
      getAdminProducts(),
      getAdminOrders(),
      getAdminCategories()
    ])
    
    stats.users = usersData?.length || 0
    stats.products = productsData?.length || 0
    stats.orders = ordersData?.length || 0
    stats.categories = categoriesData?.length || 0
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const loadSalesTrend = async () => {
  try {
    const data = await getSalesTrend()
    initSalesChart(data || [])
  } catch (error) {
    console.error('Failed to load sales trend:', error)
    initSalesChart([])
  }
}

const loadCategoryRatio = async () => {
  try {
    const data = await getCategoryRatio()
    initCategoryChart(data || [])
  } catch (error) {
    console.error('Failed to load category ratio:', error)
    initCategoryChart([])
  }
}

const initSalesChart = (data) => {
  if (!salesChartRef.value) return

  salesChart = echarts.init(salesChartRef.value)

  const dates = data.map(item => item.date || item.dayOfWeek)
  const salesAmounts = data.map(item => item.sales_amount || 0)
  const orderCounts = data.map(item => item.order_count || 0)

  const maxSales = Math.max(...salesAmounts, 1000)
  const maxOrders = Math.max(...orderCounts, 10)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      }
    },
    toolbox: {
      feature: {
        dataView: { show: true, readOnly: false },
        magicType: { show: true, type: ['line', 'bar'] },
        restore: { show: true },
        saveAsImage: { show: true }
      }
    },
    legend: {
      data: ['销售额', '订单量']
    },
    xAxis: {
      type: 'category',
      data: dates.length > 0 ? dates : ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisPointer: {
        type: 'shadow'
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        min: 0,
        max: Math.ceil(maxSales / 5000) * 5000 || 25000,
        interval: Math.ceil(maxSales / 5) || 5000,
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      {
        type: 'value',
        name: '订单量',
        min: 0,
        max: Math.ceil(maxOrders / 20) * 20 || 100,
        interval: Math.ceil(maxOrders / 5) || 20
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        data: salesAmounts.length > 0 ? salesAmounts : [0, 0, 0, 0, 0, 0, 0],
        itemStyle: {
          color: '#409EFF'
        }
      },
      {
        name: '订单量',
        type: 'bar',
        yAxisIndex: 1,
        data: orderCounts.length > 0 ? orderCounts : [0, 0, 0, 0, 0, 0, 0],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#67C23A' },
            { offset: 1, color: '#95d475' }
          ])
        }
      }
    ]
  }

  salesChart.setOption(option)
}

const initCategoryChart = (data) => {
  if (!categoryChartRef.value) return

  categoryChart = echarts.init(categoryChartRef.value)

  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#909399', '#F56C6C', '#B37FEB', '#EC8469']
  
  const chartData = data.length > 0 
    ? data.map((item, index) => ({
        value: item.product_count || 0,
        name: item.category_name || '未知',
        itemStyle: { color: colors[index % colors.length] }
      }))
    : [
        { value: 1, name: '暂无数据', itemStyle: { color: '#909399' } }
      ]

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '商品分类',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: chartData
      }
    ]
  }

  categoryChart.setOption(option)
}

const handleResize = () => {
  salesChart?.resize()
  categoryChart?.resize()
}

onMounted(async () => {
  await loadStats()
  await loadSalesTrend()
  await loadCategoryRatio()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped>
.dashboard-content {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-icon {
  font-size: 24px;
  margin-right: 8px;
}

.stat-card :deep(.el-statistic__head) {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-card :deep(.el-statistic__content) {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 300px;
}
</style>