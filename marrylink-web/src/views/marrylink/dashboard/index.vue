<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon :size="40"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">总注册主持人</div>
              <div class="stat-value">{{ stats.hostCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon :size="40"><UserFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">总注册用户</div>
              <div class="stat-value">{{ stats.userCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon :size="40"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">本月订单量</div>
              <div class="stat-value">{{ stats.orderCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon :size="40"><Money /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-title">平台交易额</div>
              <div class="stat-value">¥{{ stats.totalAmount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单趋势</span>
              <el-radio-group v-model="trendType" size="small" @change="handleTrendTypeChange">
                <el-radio-button label="monthly">月度</el-radio-button>
                <el-radio-button label="yearly">年度</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChart" style="height: 350px"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单状态分布</span>
            </div>
          </template>
          <div ref="statusChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近订单</span>
            </div>
          </template>
          <el-table :data="recentOrders" style="width: 100%">
            <el-table-column prop="id" label="订单ID" width="80" />
            <el-table-column prop="userName" label="用户名称" width="120" />
            <el-table-column prop="hostName" label="主持人" width="120" />
            <el-table-column prop="weddingDate" label="婚礼日期" width="120" />
            <el-table-column prop="weddingType" label="婚礼类型" width="120">
              <template #default="{ row }">
                {{ getWeddingTypeName(row.weddingType) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">
                ¥{{ row.amount }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { User, UserFilled, Document, Money } from '@element-plus/icons-vue'
import { getDashboardStats, getMonthlyOrderTrend, getYearlyOrderTrend, getOrderStatusDistribution, getRecentOrders, getTagList } from '@/api/marrylink-api'
import * as echarts from 'echarts'

const stats = ref({
  hostCount: 0,
  userCount: 0,
  orderCount: 0,
  totalAmount: 0
})

const trendType = ref('monthly')
const trendChart = ref(null)
const statusChart = ref(null)
const recentOrders = ref([])
const weddingTypeDict = ref([])
let trendChartInstance = null
let statusChartInstance = null

async function fetchStats() {
  const res = await getDashboardStats()
  stats.value = res
}

async function fetchTrendData() {
  let res
  if (trendType.value === 'monthly') {
    const currentYear = new Date().getFullYear()
    res = await getMonthlyOrderTrend(currentYear)
  } else {
    res = await getYearlyOrderTrend()
  }
  
  await nextTick()
  renderTrendChart(res)
}

async function fetchStatusData() {
  const res = await getOrderStatusDistribution()
  await nextTick()
  renderStatusChart(res)
}

async function fetchRecentOrders() {
  const res = await getRecentOrders()
  recentOrders.value = res
}

function getStatusText(status) {
  const statusMap = {
    1: '待确认',
    3: '已支付',
    4: '已完成',
    5: '已取消'
  }
  return statusMap[status] || '未知'
}

function getStatusType(status) {
  const typeMap = {
    1: 'warning',
    3: 'primary',
    4: 'success',
    5: 'info'
  }
  return typeMap[status] || 'info'
}

function getWeddingTypeName(code) {
  const tag = weddingTypeDict.value.find(t => t.code == code)
  return tag ? tag.name : code
}

function renderTrendChart(data) {
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value)
  }
  
  const xAxisData = data.map(item => item.month || item.year)
  const seriesData = data.map(item => item.count)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisTick: {
        alignWithLabel: true
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '订单量',
        type: 'bar',
        barWidth: '60%',
        data: seriesData,
        itemStyle: {
          color: '#1d4ed8'
        }
      }
    ]
  }
  
  trendChartInstance.setOption(option)
}

function renderStatusChart(data) {
  if (!statusChartInstance) {
    statusChartInstance = echarts.init(statusChart.value)
  }
  
  const chartData = data.map(item => ({
    name: item.status,
    value: item.count
  }))
  
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
        name: '订单状态',
        type: 'pie',
        radius: '50%',
        data: chartData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  statusChartInstance.setOption(option)
}

function handleTrendTypeChange() {
  fetchTrendData()
}

onMounted(async () => {
  // 加载婚礼类型标签字典 (02是婚礼类型分类代码)
  const tagRes = await getTagList("01")
  weddingTypeDict.value = tagRes
  
  await fetchStats()
  await fetchTrendData()
  await fetchStatusData()
  await fetchRecentOrders()
  
  window.addEventListener('resize', () => {
    trendChartInstance?.resize()
    statusChartInstance?.resize()
  })
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 20px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>