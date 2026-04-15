<template>
  <view class="calendar-container">
    <view class="header">
      <view class="header-content">
        <text class="title" v-if="isHost">查看档期</text>
        <text class="title" v-else>选择档期</text>
        <text class="subtitle">{{ hostInfo.name }}</text>
      </view>
    </view>

    <view class="month-selector">
      <view class="month-btn" @click="handlePrevMonth">
        <text class="icon">◀</text>
      </view>
      <text class="current-month">{{ currentYearMonth }}</text>
      <view class="month-btn" @click="handleNextMonth">
        <text class="icon">▶</text>
      </view>
    </view>

    <view class="legend">
      <!-- <view class="legend-item">
        <view class="dot available"></view>
        <text>可预约</text>
      </view> -->
      <view class="legend-item">
        <view class="dot pending"></view>
        <text>待确认</text>
      </view>
      <view class="legend-item">
        <view class="dot occupied"></view>
        <text>已占用</text>
      </view>
    </view>

    <view class="calendar">
      <view class="weekdays">
        <text class="weekday" v-for="day in weekdays" :key="day">{{ day }}</text>
      </view>
      <view class="days">
        <view
          v-for="(day, index) in calendarDays"
          :key="index"
          class="day-cell"
          :class="getDayClass(day)"
          @click="handleDayClick(day)"
        >
          <text class="day-number">{{ day.day }}</text>
          <text v-if="day.status" class="status-text" :class="day.status">
            {{ getStatusText(day.status) }}
          </text>
        </view>
      </view>
    </view>

    <!-- 预约弹框 -->
    <view v-if="showBookModal" class="modal-overlay" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">确认订单</text>
          <text class="modal-close" @click="closeModal">✕</text>
        </view>
        <view class="modal-body">
          <text class="modal-date">婚礼日期：{{ selectedDate }}</text>
          <view class="wedding-type-section">
            <text class="section-label">婚礼类型：</text>
            <radio-group @change="handleWeddingTypeChange">
              <label class="radio-item" v-for="type in weddingTypes" :key="type.value">
                <radio :value="type.value" :checked="selectedWeddingType === type.value" />
                <text>{{ type.label }}</text>
              </label>
            </radio-group>
          </view>
          <view class="amount-section">
            <text class="amount-label">支付金额：</text>
            <text class="amount-value">¥{{ hostInfo.price || '3000' }}</text>
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @click="closeModal">取消</view>
          <view class="modal-btn confirm" @click="goToPay">去支付</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getHostDetail, getHostSchedule, bookHost } from '@/api/host'

export default {
  data() {
    return {
      hostId: '',
      hostInfo: {},
      currentDate: new Date(),
      scheduleData: [],
      weekdays: ['日', '一', '二', '三', '四', '五', '六'],
      showBookModal: false,
      selectedDate: '',
      selectedWeddingType: '01',
      weddingTypes: [
        { label: '中式婚礼', value: '01' },
        { label: '西式婚礼', value: '02' },
        { label: '主题婚礼', value: '03' },
        { label: '户外婚礼', value: '04' }
      ]
    }
  },

  computed: {
    ...mapState('user', ['userInfo']),

    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    },

    currentYearMonth() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth() + 1
      return `${year}年${month}月`
    },

    calendarDays() {
      const year = this.currentDate.getFullYear()
      const month = this.currentDate.getMonth()
      const firstDay = new Date(year, month, 1).getDay()
      const daysInMonth = new Date(year, month + 1, 0).getDate()
      const days = []

      for (let i = 0; i < firstDay; i++) {
        days.push({ day: '', date: '', disabled: true })
      }

      for (let i = 1; i <= daysInMonth; i++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
        const schedule = this.scheduleData.find(s => s.weddingDate === dateStr)
        const isPast = new Date(dateStr) < new Date(new Date().toDateString())
        
        days.push({
          day: i,
          date: dateStr,
          status: schedule ? this.getScheduleStatus(schedule.status) : null,
          disabled: isPast,
          schedule
        })
      }

      return days
    }
  },

  onLoad(options) {
    if (options.hostId) {
      this.hostId = options.hostId
      this.loadHostInfo()
      this.loadSchedule()
    }
  },

  methods: {
    async loadHostInfo() {
      try {
        const res = await getHostDetail(this.hostId)
        this.hostInfo = res.data || {}
      } catch (error) {
        console.error('加载主持人信息失败:', error)
      }
    },

    async loadSchedule() {
      try {
        const year = this.currentDate.getFullYear()
        const month = this.currentDate.getMonth() + 1
        const hostId = this.hostId;
        const res = await getHostSchedule({ hostId,year, month })
        this.scheduleData = res.data || []
      } catch (error) {
        console.error('加载档期失败:', error)
      }
    },

    getScheduleStatus(status) {
      if (status === 3) return 'occupied'
      if (status === 1) return 'pending'
      return null
    },

    getStatusText(status) {
      if (status === 'occupied') return '已占用'
      if (status === 'pending') return '待确认'
      // return '可预约'
    },

    getDayClass(day) {
      if (!day.day) return 'empty'
      if (day.disabled) return 'disabled'
      if (day.status === 'occupied') return 'occupied'
      if (day.status === 'pending') return 'pending'
      return 'available'
    },

    handlePrevMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() - 1)
      this.currentDate = date
      this.loadSchedule()
    },

    handleNextMonth() {
      const date = new Date(this.currentDate)
      date.setMonth(date.getMonth() + 1)
      this.currentDate = date
      this.loadSchedule()
    },

    handleDayClick(day) {
      if (!day.day || day.disabled) return

      // 主持人模式下禁用点击
      if (this.isHost) {
        return
      }

      if (day.status === 'occupied') {
        uni.showToast({
          title: '该日期已被预约',
          icon: 'none'
        })
        return
      }

      if (day.status === 'pending') {
        uni.showToast({
          title: '该日期待确认中',
          icon: 'none'
        })
        return
      }

      this.selectedDate = day.date
      this.selectedWeddingType = '01'
      this.showBookModal = true
    },

    handleWeddingTypeChange(e) {
      this.selectedWeddingType = e.detail.value
    },

    closeModal() {
      this.showBookModal = false
    },

    async confirmBook() {
      try {
        await bookHost({
          hostId: this.hostId,
          weddingDate: this.selectedDate,
          weddingType: this.selectedWeddingType
        })
        this.showBookModal = false
        uni.showToast({
          title: '预约成功',
          icon: 'success'
        })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        uni.showToast({
          title: error.message || '预约失败',
          icon: 'none'
        })
      }
    },

    // 跳转到支付页面
    goToPay() {
      this.showBookModal = false
      const params = {
        hostId: this.hostId,
        hostName: encodeURIComponent(this.hostInfo.name || '主持人'),
        weddingDate: this.selectedDate,
        weddingType: this.selectedWeddingType,
        amount: this.hostInfo.price || '3000'
      }
      const query = Object.keys(params).map(k => `${k}=${params[k]}`).join('&')
      uni.navigateTo({
        url: `/pages/payment/index?${query}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.calendar-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  padding: 32rpx;
  color: #ffffff;

  .header-content {
    .title {
      display: block;
      font-size: 40rpx;
      font-weight: bold;
      margin-bottom: 8rpx;
    }

    .subtitle {
      font-size: 28rpx;
      opacity: 0.9;
    }
  }
}

.month-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;

  .month-btn {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f5f7fa;
    border-radius: 50%;

    .icon {
      font-size: 24rpx;
      color: #1d4ed8;
    }
  }

  .current-month {
    font-size: 32rpx;
    font-weight: bold;
    color: #333333;
  }
}

.legend {
  display: flex;
  justify-content: center;
  gap: 40rpx;
  padding: 24rpx 32rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .dot {
      width: 24rpx;
      height: 24rpx;
      border-radius: 50%;

      &.available {
        background-color: #67c23a;
      }

      &.pending {
        background-color: #e6a23c;
      }

      &.occupied {
        background-color: #1d4ed8;
      }
    }

    text {
      font-size: 24rpx;
      color: #666666;
    }
  }
}

.calendar {
  background-color: #ffffff;
  padding: 32rpx;

  .weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    margin-bottom: 20rpx;

    .weekday {
      text-align: center;
      font-size: 26rpx;
      color: #999999;
      padding: 16rpx 0;
    }
  }

  .days {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 16rpx;

    .day-cell {
      aspect-ratio: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border-radius: 12rpx;
      position: relative;

      .day-number {
        font-size: 28rpx;
        font-weight: 500;
        margin-bottom: 4rpx;
      }

      .status-text {
        font-size: 20rpx;
        font-weight: 400;

        &.available {
          color: #67c23a;
        }

        &.pending {
          color: #e6a23c;
        }

        &.occupied {
          color: #1d4ed8;
        }
      }

      &.empty {
        visibility: hidden;
      }

      &.disabled {
        .day-number {
          color: #cccccc;
        }
      }

      &.available {
        background-color: #f0f9ff;

        .day-number {
          color: #333333;
        }
      }

      &.pending {
        background-color: #fef5e7;

        .day-number {
          color: #e6a23c;
        }
      }

      &.occupied {
        background-color: #ecf5ff;

        .day-number {
          color: #1d4ed8;
        }
      }
    }
  }
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 600rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .modal-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333333;
  }

  .modal-close {
    font-size: 40rpx;
    color: #999999;
    line-height: 1;
  }
}

.modal-body {
  padding: 32rpx;

  .modal-date {
    display: block;
    font-size: 28rpx;
    color: #333333;
    margin-bottom: 24rpx;
  }

  .wedding-type-section {
    .section-label {
      display: block;
      font-size: 28rpx;
      color: #333333;
      margin-bottom: 16rpx;
    }

    .radio-item {
      display: flex;
      align-items: center;
      padding: 16rpx 0;

      radio {
        margin-right: 12rpx;
      }

      text {
        font-size: 28rpx;
        color: #666666;
      }
    }
  }

  .amount-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 24rpx;
    padding-top: 24rpx;
    border-top: 1rpx solid #f0f0f0;

    .amount-label {
      font-size: 28rpx;
      color: #333333;
    }

    .amount-value {
      font-size: 40rpx;
      color: #ef4444;
      font-weight: bold;
    }
  }
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid #f0f0f0;

  .modal-btn {
    flex: 1;
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;

    &.cancel {
      color: #666666;
      border-right: 1rpx solid #f0f0f0;
    }

    &.confirm {
      color: #1d4ed8;
      font-weight: bold;
    }
  }
}
</style>