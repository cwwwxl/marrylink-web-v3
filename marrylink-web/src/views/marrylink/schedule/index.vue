<template>
  <div class="schedule-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Calendar /></el-icon>
        档期管理
      </h2>
      <p class="page-description">查看和管理您的婚礼档期安排</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card available-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.available }}</div>
              <div class="stat-label">可预约</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card pending-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待确认</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card occupied-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.occupied }}</div>
              <div class="stat-label">已占用</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日历卡片 -->
    <el-card class="calendar-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">档期日历</span>
          <div class="calendar-controls">
            <el-button :icon="ArrowLeft" circle @click="handlePrevMonth" />
            <span class="current-month">{{ currentYearMonth }}</span>
            <el-button :icon="ArrowRight" circle @click="handleNextMonth" />
            <el-button @click="handleToday">今天</el-button>
          </div>
        </div>
      </template>

      <!-- 图例 -->
      <div class="legend">
        <div class="legend-item">
          <span class="legend-dot available"></span>
          <span>可预约</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot pending"></span>
          <span>待确认（状态1-2）</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot occupied"></span>
          <span>已占用（已支付）</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot completed"></span>
          <span>已完成</span>
        </div>
      </div>

      <!-- 日历 -->
      <div v-loading="loading" class="calendar-wrapper">
        <el-calendar v-model="currentDate">
          <template #date-cell="{ data }">
            <div
              class="calendar-day"
              :class="getDayClass(data.day)"
              @click="handleDayClick(data.day)"
            >
              <div class="day-number">{{ data.day.split('-')[2] }}</div>
              <div v-if="getDayOrders(data.day).length > 0" class="day-orders">
                <div
                  v-for="order in getDayOrders(data.day)"
                  :key="order.id"
                  class="order-badge"
                  :class="`status-${order.status}`"
                >
                  <el-icon><User /></el-icon>
                  {{ getOrderStatusText(order.status) }}
                </div>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailDialog.visible"
      :title="`${selectedDate} 的档期详情`"
      width="700px"
    >
      <div v-if="selectedOrders.length === 0" class="empty-orders">
        <el-empty description="该日期暂无预约" />
      </div>
      <div v-else class="order-list">
        <div
          v-for="order in selectedOrders"
          :key="order.id"
          class="order-item"
        >
          <div class="order-header">
            <el-tag :type="getStatusTagType(order.status)" size="large">
              {{ getOrderStatusText(order.status) }}
            </el-tag>
            <span class="order-no">{{ order.orderNo }}</span>
          </div>
          <el-descriptions :column="2" border class="order-details">
            <el-descriptions-item label="婚礼日期">
              {{ order.weddingDate }}
            </el-descriptions-item>
            <el-descriptions-item label="婚礼类型">
              {{ order.weddingType || '未填写' }}
            </el-descriptions-item>
            <el-descriptions-item label="金额">
              ¥{{ order.amount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ order.createTime }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="order-actions">
            <el-button
              v-if="order.status === 1"
              type="success"
              size="small"
              @click="handleUpdateStatus(order.id, 2)"
            >
              确认预约
            </el-button>
            <el-button
              v-if="order.status === 2"
              type="primary"
              size="small"
              @click="handleUpdateStatus(order.id, 3)"
            >
              标记已付定金
            </el-button>
            <el-button
              v-if="order.status === 3"
              type="success"
              size="small"
              @click="handleUpdateStatus(order.id, 4)"
            >
              标记已完成
            </el-button>
            <el-button
              v-if="order.status < 4"
              type="danger"
              size="small"
              @click="handleUpdateStatus(order.id, 5)"
            >
              取消订单
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  CircleCheck,
  Clock,
  Lock,
  ArrowLeft,
  ArrowRight,
  User
} from '@element-plus/icons-vue'
import { getOrderSchedule, updateOrderStatus } from '@/api/marrylink-api'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const currentHostId = computed(() => userStore.user.hostId || userStore.user.userId)

const loading = ref(false)
const currentDate = ref(new Date())
const orders = ref([])

// 详情对话框
const detailDialog = reactive({
  visible: false
})

const selectedDate = ref('')
const selectedOrders = ref([])

// 当前年月显示
const currentYearMonth = computed(() => {
  const date = currentDate.value
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  return `${year}年${month}月`
})

// 统计数据
const stats = computed(() => {
  const result = { available: 0, pending: 0, occupied: 0 }
  
  // 获取当月天数
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  const daysInMonth = new Date(year, month, 0).getDate()
  
  // 统计已占用和待确认的天数
  const occupiedDays = new Set()
  const pendingDays = new Set()
  
  orders.value.forEach(order => {
    const dateStr = order.weddingDate
    if (order.status === 3 || order.status === 4) {
      // 定金已付或已完成
      occupiedDays.add(dateStr)
    } else if (order.status === 1 || order.status === 2) {
      // 待确认或已确认
      pendingDays.add(dateStr)
    }
  })
  
  result.occupied = occupiedDays.size
  result.pending = pendingDays.size
  result.available = daysInMonth - occupiedDays.size - pendingDays.size
  
  return result
})

// 获取指定日期的订单
function getDayOrders(dateStr) {
  return orders.value.filter(order => order.weddingDate === dateStr)
}

// 获取日期的样式类
function getDayClass(dateStr) {
  const dayOrders = getDayOrders(dateStr)
  if (dayOrders.length === 0) return 'day-available'
  
  // 检查是否有已占用的订单（定金已付或已完成）
  const hasOccupied = dayOrders.some(o => o.status === 3 || o.status === 4)
  if (hasOccupied) return 'day-occupied'
  
  // 检查是否有待确认的订单
  const hasPending = dayOrders.some(o => o.status === 1 || o.status === 2)
  if (hasPending) return 'day-pending'
  
  return 'day-available'
}

// 获取订单状态文本
function getOrderStatusText(status) {
  const texts = {
    1: '待确认',
    2: '已确认',
    3: '已付定金',
    4: '已完成',
    5: '已取消'
  }
  return texts[status] || '未知'
}

// 获取状态标签类型
function getStatusTagType(status) {
  const types = {
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'info',
    5: 'danger'
  }
  return types[status] || 'info'
}

// 加载档期数据
async function loadSchedule() {
  loading.value = true
  try {
    const year = currentDate.value.getFullYear()
    const month = currentDate.value.getMonth() + 1
    console.log(userStore.user)
    
    const res = await getOrderSchedule({
      hostId: currentHostId.value,
      year,
      month
    })
    orders.value = res
  } catch (error) {
    ElMessage.error('加载档期数据失败')
  } finally {
    loading.value = false
  }
}

// 上一月
function handlePrevMonth() {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() - 1)
  currentDate.value = date
}

// 下一月
function handleNextMonth() {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() + 1)
  currentDate.value = date
}

// 回到今天
function handleToday() {
  currentDate.value = new Date()
}

// 点击日期
function handleDayClick(dateStr) {
  selectedDate.value = dateStr
  selectedOrders.value = getDayOrders(dateStr)
  detailDialog.visible = true
}

// 更新订单状态
async function handleUpdateStatus(orderId, status) {
  try {
    await updateOrderStatus(orderId, status)
    ElMessage.success('状态更新成功')
    loadSchedule()
    // 更新选中的订单列表
    selectedOrders.value = getDayOrders(selectedDate.value)
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

// 监听日期变化
watch(currentDate, () => {
  loadSchedule()
})

onMounted(() => {
  loadSchedule()
})
</script>

<style scoped lang="scss">
.schedule-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.15);

  .page-title {
    display: flex;
    align-items: center;
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;

    .title-icon {
      margin-right: 12px;
      font-size: 28px;
    }
  }

  .page-description {
    margin: 0;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.9);
  }
}

.stats-row {
  margin-bottom: 20px;

  .stat-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: 700;
          line-height: 1.2;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }

    &.available-card {
      .stat-icon {
        background-color: #f0f9ff;
        color: #67c23a;
      }
      .stat-value {
        color: #67c23a;
      }
    }

    &.pending-card {
      .stat-icon {
        background-color: #fef0f0;
        color: #e6a23c;
      }
      .stat-value {
        color: #e6a23c;
      }
    }

    &.occupied-card {
      .stat-icon {
        background-color: #ecf5ff;
        color: #1d4ed8;
      }
      .stat-value {
        color: #1d4ed8;
      }
    }
  }
}

.calendar-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;

  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #e4e7ed;
    background-color: #fafafa;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .calendar-controls {
      display: flex;
      align-items: center;
      gap: 12px;

      .current-month {
        font-size: 16px;
        font-weight: 600;
        color: #1d4ed8;
        min-width: 100px;
        text-align: center;
      }
    }
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.legend {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #606266;

    .legend-dot {
      width: 16px;
      height: 16px;
      border-radius: 4px;

      &.available {
        background-color: #67c23a;
      }

      &.pending {
        background-color: #e6a23c;
      }

      &.occupied {
        background-color: #1d4ed8;
      }

      &.completed {
        background-color: #909399;
      }
    }
  }
}

.calendar-wrapper {
  :deep(.el-calendar) {
    .el-calendar__header {
      display: none;
    }

    .el-calendar__body {
      padding: 0;
    }

    .el-calendar-table {
      .el-calendar-day {
        padding: 0;
        height: 100px;
      }
    }
  }

  .calendar-day {
    height: 100%;
    padding: 8px;
    cursor: pointer;
    transition: all 0.3s;
    border-radius: 4px;

    &:hover {
      background-color: #f5f9ff;
    }

    .day-number {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    .day-orders {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .order-badge {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
        color: #ffffff;

        &.status-1,
        &.status-2 {
          background-color: #e6a23c;
        }

        &.status-3 {
          background-color: #1d4ed8;
        }

        &.status-4 {
          background-color: #909399;
        }

        &.status-5 {
          background-color: #f56c6c;
        }
      }
    }

    &.day-available {
      .day-number {
        color: #8c8c8c;
      }
    }

    &.day-pending {
      background-color: #fef5e7;

      .day-number {
        color: #e6a23c;
      }
    }

    &.day-occupied {
      background-color: #ecf5ff;

      .day-number {
        color: #1d4ed8;
      }
    }
  }
}

.empty-orders {
  padding: 40px 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .order-item {
    padding: 16px;
    background-color: #f5f7fa;
    border-radius: 8px;

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .order-no {
        font-size: 14px;
        color: #606266;
        font-family: 'Courier New', monospace;
      }
    }

    .order-details {
      margin-bottom: 12px;
    }

    .order-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
    }
  }
}

@media (max-width: 768px) {
  .schedule-container {
    padding: 12px;
  }

  .page-header {
    padding: 16px;

    .page-title {
      font-size: 20px;

      .title-icon {
        font-size: 24px;
      }
    }
  }

  .stats-row {
    .el-col {
      margin-bottom: 12px;
    }
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start !important;

    .calendar-controls {
      width: 100%;
      justify-content: space-between;
    }
  }

  .legend {
    flex-wrap: wrap;
    gap: 12px;
  }

  .calendar-wrapper {
    :deep(.el-calendar-table) {
      .el-calendar-day {
        height: 80px;
      }
    }

    .calendar-day {
      padding: 4px;

      .day-number {
        font-size: 14px;
      }

      .order-badge {
        font-size: 10px;
        padding: 1px 4px;
      }
    }
  }
}
</style>