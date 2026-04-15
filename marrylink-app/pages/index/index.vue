<template>
  <view class="home-container">
    <!-- 主持人工作台 -->
    <view v-if="isHost" class="host-dashboard">
      <!-- 顶部问候卡片 -->
      <view class="greeting-card">
        <view class="greeting-content">
          <view class="greeting-text">
            <text class="greeting-time">{{ greetingTime }}，</text>
            <text class="greeting-name">{{ hostName }}</text>
            <text class="greeting-date">今天是 {{ currentDate }}，星期{{ weekDay }}</text>
          </view>
          <view class="greeting-right">
            <view class="message-bell" @click="showMessagePopup">
              <text class="bell-icon">🔔</text>
              <view v-if="unreadCount > 0" class="red-dot"></view>
            </view>
            <view class="voice-icon">🎙️</view>
          </view>
        </view>
      </view>

      <!-- 统计数据卡片 -->
      <view class="stats-grid">
        <!-- 本月订单 -->
        <view class="stat-item">
          <text class="stat-label">本月订单</text>
          <text class="stat-value">{{ stats.monthOrders || 0 }}</text>
          <text class="stat-trend trend-up">较上月 +{{ stats.orderTrend || 0 }}</text>
        </view>
        
        <!-- 待处理问卷 -->
        <view class="stat-item">
          <text class="stat-label">待查看问卷</text>
          <view class="stat-value-row">
            <text class="stat-value">{{ stats.pendingQuestionnaires || 0 }}</text>
            <!-- <text class="stat-badge">3 新</text> -->
          </view>
          <text class="stat-trend">需在24小时内查看</text>
        </view>
        
        <!-- 本月收入 -->
        <view class="stat-item">
          <text class="stat-label">本月收入</text>
          <text class="stat-value income">¥{{ (stats.monthIncome || 0).toLocaleString() }}</text>
          <text class="stat-trend trend-up">目标完成率 {{ stats.incomeRate || 0 }}%</text>
        </view>
        
        <!-- 满意度评分 -->
        <view class="stat-item">
          <text class="stat-label">满意度评分</text>
          <view class="stat-value-row">
            <text class="stat-value rating">{{ stats.rating || 5.0 }}</text>
            <text class="star-icon">⭐</text>
          </view>
          <text class="stat-trend">基于 {{ stats.ratingCount || 0 }} 条评价</text>
        </view>
      </view>

      <!-- 快捷功能 -->
      <view class="quick-actions">
        <text class="section-title-small">快捷功能</text>
        <view class="action-grid">
          <view class="action-item" @click="goToSchedule">
            <view class="action-icon blue">📅</view>
            <text class="action-text">档期管理</text>
          </view>
          <view class="action-item" @click="goToQuestionnaire">
            <view class="action-icon purple">📝</view>
            <text class="action-text">问卷管理</text>
          </view>
          <view class="action-item" @click="goToOrders">
            <view class="action-icon green">💎</view>
            <text class="action-text">我的订单</text>
          </view>
          <view class="action-item" @click="goToChatList">
            <view class="action-icon orange">💬</view>
            <text class="action-text">实时沟通</text>
          </view>
        </view>
      </view>

      <!-- 待处理事项 -->
      <view class="section" >
        <view class="section-header">
          <text class="section-title">待处理事项</text>
          <text class="section-more" @click="goToQuestionnaire" v-if="pendingItems.length > 0">查看全部</text>
        </view>
        <view class="pending-list">
          <view 
            class="pending-item" 
            v-for="item in pendingItems" 
            :key="item.id"
            @click="handlePendingItem(item)"
          >
            <view class="pending-icon" :style="{ background: item.iconBg }">
              {{ item.icon }}
            </view>
            <view class="pending-info">
              <text class="pending-title">{{ item.title }}</text>
              <text class="pending-desc">{{ item.desc }}</text>
              <text class="pending-time">📅 {{ item.time }}</text>
            </view>
            <view class="pending-badge" v-if="item.badge">{{ item.badge }}</view>
            <text class="pending-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 近期婚礼 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">近期婚礼</text>
          <text class="section-more" @click="goToSchedule">查看档期</text>
        </view>
        
        <!-- 本月日历 -->
        <view class="calendar-section">
          <view class="calendar-header">
            <text class="calendar-title">{{ currentYear }}年{{ currentMonth }}月</text>
            <view class="calendar-nav">
              <text class="nav-btn" @click="prevMonth">‹</text>
              <text class="nav-btn" @click="nextMonth">›</text>
            </view>
          </view>
          <view class="calendar-weekdays">
            <text class="weekday" v-for="day in weekdays" :key="day">{{ day }}</text>
          </view>
          <view class="calendar-days">
            <view
              class="day-item"
              v-for="(day, index) in calendarDays"
              :key="index"
              :class="{
                'other-month': day.isOtherMonth,
                'has-event': day.hasEvent,
                'occupied': day.status == 3,
                'locked': day.status == 1
              }"
              @click="handleDayClick(day)"
            >
              <text class="day-number">{{ day.day }}</text>
              <text class="day-status" v-if="day.status == 1">待确认</text>
              <text class="day-status" v-if="day.status == 3">定金已付，待主持</text>
              <view class="day-dot" v-if="day.hasEvent && !day.status"></view>
            </view>
          </view>
        </view>

        <!-- 即将到来的婚礼列表 -->
        <view class="upcoming-weddings" v-if="upcomingWeddings.length > 0">
          <view 
            class="wedding-item" 
            v-for="wedding in upcomingWeddings" 
            :key="wedding.id"
            @click="goToWeddingDetail(wedding.id)"
          >
            <view class="wedding-date">
              <text class="date-day">{{ wedding.day }}</text>
              <text class="date-month">{{ wedding.month }}</text>
            </view>
            <view class="wedding-info">
              <text class="wedding-couple">{{ wedding.coupleName }}</text>
              <!-- <text class="wedding-venue">📍 {{ wedding.venue }}</text> -->
            </view>
            <view class="wedding-status" :class="'status-' + wedding.status">
              {{ wedding.statusText }}
            </view>
          </view>
        </view>
        <view v-else class="empty-state">
          <text class="empty-icon">📅</text>
          <text class="empty-text">本月暂无婚礼安排</text>
        </view>
      </view>
    </view>

    <!-- 新人端首页 -->
    <view v-else class="couple-home">
      <!-- 顶部轮播 -->
      <view class="banner-section">
        <swiper class="banner-swiper" circular autoplay>
          <swiper-item v-for="(item, index) in banners" :key="index">
            <view class="banner-card" :class="'banner-bg-' + (index + 1)">
              <text class="banner-title">{{ item.title }}</text>
              <text class="banner-subtitle">{{ item.subtitle }}</text>
            </view>
          </swiper-item>
        </swiper>
      </view>
      
      <!-- 快捷入口 -->
      <view class="quick-entry">
        <view class="entry-item" @click="goToHost">
          <view class="entry-icon">🎤</view>
          <text class="entry-text">找主持人</text>
        </view>
        <view class="entry-item" @click="goToQuestionnaire">
          <view class="entry-icon">📝</view>
          <text class="entry-text">填写问卷</text>
        </view>
        <view class="entry-item" @click="goToMyOrders">
          <view class="entry-icon">📋</view>
          <text class="entry-text">我的预约</text>
        </view>
        <view class="entry-item" @click="goToChatList">
          <view class="entry-icon">💬</view>
          <text class="entry-text">实时沟通</text>
        </view>
      </view>
      
      <!-- 推荐主持人 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">推荐主持人</text>
          <text class="section-more" @click="goToHost">查看更多 ></text>
        </view>
        
        <scroll-view class="host-scroll" scroll-x>
          <view class="host-list">
            <view 
              class="host-card" 
              v-for="host in recommendHosts" 
              :key="host.id"
              @click="goToHostDetail(host.id)"
            >
              <image class="host-avatar" :src="host.avatar ? BASE_URL + host.avatar : '/static/default-avatar.png'" mode="aspectFill"></image>
              <view class="host-info">
                <text class="host-name">{{ host.name }}</text>
                <view class="host-tags">
                  <text class="tag" v-for="tag in host.tags" :key="tag">{{ getTagNameByCode(tag) }}</text>
                </view>
                <view class="host-stats">
                  <text class="stat">⭐ {{ host.rating || '5.0' }}</text>
                  <text class="stat">💼 {{ host.experience || '5' }}年经验</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
      
      <!-- 婚礼案例 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">精彩案例</text>
        </view>
        
        <view class="case-grid">
          <view 
            class="case-item" 
            v-for="caseItem in cases" 
            :key="caseItem.id"
            @click="viewCase(caseItem.id)"
          >
            <image class="case-image" :src="caseItem.image" mode="aspectFill"></image>
            <view class="case-info">
              <text class="case-title">{{ caseItem.title }}</text>
              <text class="case-date">{{ caseItem.date }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getHostList, getTagList } from '@/api/host'
import {
  getHostDashboardStats,
  getHostPendingQuestionnaires,
  getHostMonthSchedule,
  getUpcomingWeddings
} from '@/api/dashboard'
import { getUnreadMessages, getUnreadCount, markMessagesAsRead } from '@/api/message'
import { BASE_URL } from '@/utils/request'

export default {
  data() {
    return {
      BASE_URL: BASE_URL,
      // 新人端数据
      banners: [
        { title: '找到您的完美婚礼主持人', subtitle: '专业、贴心、难忘' },
        { title: '一站式婚礼服务', subtitle: '让您的婚礼更加完美' },
        { title: '精选优质主持人', subtitle: '为您的婚礼增添光彩' }
      ],
      recommendHosts: [],
      tagDict: [],
      cases: [
        {
          id: 1,
          title: '浪漫海边婚礼',
          image: '/static/case1.png',
          date: '2024-01-15'
        },
        {
          id: 2,
          title: '梦幻城堡婚礼',
          image: '/static/case2.png',
          date: '2026-02-20'
        },
        {
          id: 3,
          title: '温馨草坪婚礼',
          image: '/static/case3.png',
          date: '2025-12-10'
        },
        {
          id: 4,
          title: '星空露台婚礼',
          image: '/static/case4.png',
          date: '2025-01-16'
        }
      ],
      
      // 主持人工作台数据
      stats: {
        monthOrders: 0,
        orderTrend: 0,
        pendingQuestionnaires: 0,
        monthIncome: 0,
        incomeTrend: 0,
        incomeRate: 0,
        rating: 0,
        ratingCount: 0
      },
      pendingItems: [],
      upcomingWeddings: [],
      
      // 日历数据
      currentYear: new Date().getFullYear(),
      currentMonth: new Date().getMonth() + 1,
      calendarDays: [],
      weekdays: ['日', '一', '二', '三', '四', '五', '六'],
      scheduleData: [],
      
      // 消息数据
      unreadCount: 0,
      messages: [],
      showMessages: false
    }
  },
  
  computed: {
    ...mapState('user', ['userInfo']),
    
    // 判断是否为主持人
    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    },
    
    // 主持人姓名
    hostName() {
      return this.userInfo?.realName || '主持人'
    },
    
    // 问候语
    greetingTime() {
      const hour = new Date().getHours()
      if (hour < 6) return '凌晨好'
      if (hour < 9) return '早上好'
      if (hour < 12) return '上午好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      if (hour < 22) return '晚上好'
      return '夜深了'
    },
    
    // 当前日期
    currentDate() {
      const date = new Date()
      return `${date.getMonth() + 1}月${date.getDate()}日`
    },
    
    // 星期
    weekDay() {
      const days = ['日', '一', '二', '三', '四', '五', '六']
      return days[new Date().getDay()]
    }
  },
  
  onShow() {
    if (this.isHost) {
      // 加载主持人工作台数据
      this.loadHostDashboard()
      this.loadUnreadCount()
    } else {
      // 加载新人端数据
      this.loadTagList().finally(() => {
        this.loadRecommendHosts()
      })
    }
  },
  
  methods: {
    // ========== 主持人工作台方法 ==========
    
    // 加载主持人工作台数据
    async loadHostDashboard() {
      try {
        uni.showLoading({ title: '加载中...' })
        
        // 并行加载所有数据
        await Promise.all([
          this.loadDashboardStats(),
          this.loadPendingQuestionnaires(),
          this.loadMonthSchedule(),
          this.loadUpcomingWeddings()
        ])
        
        uni.hideLoading()
      } catch (error) {
        uni.hideLoading()
        console.error('加载工作台数据失败:', error)
      }
    },
    
    // 加载统计数据
    async loadDashboardStats() {
      try {
        const res = await getHostDashboardStats()
        if (res.code === 200 || res.code === '00000') {
          this.stats = {
            monthOrders: res.data.monthOrders || 0,
            orderTrend: res.data.orderTrend || 0,
            pendingQuestionnaires: res.data.pendingQuestionnaires || 0,
            monthIncome: res.data.monthIncome || 0,
            incomeTrend: res.data.incomeTrend || 0,
            incomeRate: res.data.incomeRate || 0,
            rating: res.data.rating || 5.0,
            ratingCount: res.data.ratingCount || 0
          }
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
      }
    },
    
    // 加载待处理问卷
    async loadPendingQuestionnaires() {
      try {
        const res = await getHostPendingQuestionnaires({ current: 1, size: 3 })
        if (res.code === 200 || res.code === '00000') {
          this.pendingItems = (res.data.records || []).map(item => ({
            id: item.id,
            icon: '📋',
            iconBg: '#fef3c7',
            title: item.submissionName + '的问卷',
            desc: item.weddingType || '中式婚礼',
            time: item.updateTime,
            badge: '今日内'
          }))
        }
      } catch (error) {
        console.error('加载待处理问卷失败:', error)
      }
    },
    
    // 加载本月档期
    async loadMonthSchedule() {
      try {
        const res = await getHostMonthSchedule({
          year: this.currentYear,
          month: this.currentMonth
        })
        if (res.code === 200 || res.code === '00000') {
          this.scheduleData = res.data || []
        }
      } catch (error) {
        console.error('加载档期失败:', error)
      }
      
      // 生成日历
      this.generateCalendar()
    },
    
    // 加载即将到来的婚礼
    async loadUpcomingWeddings() {
      try {
        const res = await getUpcomingWeddings({ current: 1, size: 3 })
        if (res.code === 200 || res.code === '00000') {
          this.upcomingWeddings = (res.data.records || []).map(item => {
            const date = new Date(item.weddingDate)
            return {
              id: item.id,
              day: date.getDate(),
              month: `${date.getMonth() + 1}月`,
              coupleName: item.coupleName,
              venue: item.venue,
              status: item.status,
              statusText: this.getStatusText(item.status)
            }
          })
        }
      } catch (error) {
        console.error('加载婚礼列表失败:', error)
      }
    },
    
    // 生成日历
    generateCalendar() {
      const year = this.currentYear
      const month = this.currentMonth
      const firstDay = new Date(year, month - 1, 1).getDay()
      const daysInMonth = new Date(year, month, 0).getDate()
      const daysInPrevMonth = new Date(year, month - 1, 0).getDate()
      
      const days = []
      const today = new Date()
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
      
      // 上个月的日期
      for (let i = firstDay - 1; i >= 0; i--) {
        days.push({
          day: daysInPrevMonth - i,
          isOtherMonth: true,
          isToday: false,
          hasEvent: false
        })
      }
      
      // 当前月的日期
      for (let i = 1; i <= daysInMonth; i++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(i).padStart(2, '0')}`
        const scheduleItem = this.scheduleData.find(s => s.date === dateStr)
        
        days.push({
          day: i,
          date: dateStr,
          isOtherMonth: false,
          isToday: dateStr === todayStr,
          hasEvent: !!scheduleItem,
          status: scheduleItem?.status || null
        })
      }
      
      // 下个月的日期（补齐6行）
      const remainingDays = 42 - days.length
      for (let i = 1; i <= remainingDays; i++) {
        days.push({
          day: i,
          isOtherMonth: true,
          isToday: false,
          hasEvent: false
        })
      }
      
      this.calendarDays = days
    },
    
    // 上一月
    prevMonth() {
      if (this.currentMonth === 1) {
        this.currentYear--
        this.currentMonth = 12
      } else {
        this.currentMonth--
      }
      this.loadMonthSchedule()
    },
    
    // 下一月
    nextMonth() {
      if (this.currentMonth === 12) {
        this.currentYear++
        this.currentMonth = 1
      } else {
        this.currentMonth++
      }
      this.loadMonthSchedule()
    },
    
    // 点击日期
    handleDayClick(day) {
      if (day.isOtherMonth) return
      
      if (day.hasEvent) {
        uni.showToast({
          title: `${day.date} 已有安排`,
          icon: 'none'
        })
      }
    },
    
    // 处理待办事项
    handlePendingItem(item) {
      uni.navigateTo({
        url: `/pages/questionnaire/detail?id=${item.id}`
      })
    },
    
    // 跳转到婚礼详情
    goToWeddingDetail(id) {
      uni.navigateTo({
        url: `/pages/order/detail?id=${id}`
      })
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        'confirmed': '已确认',
        'communicating': '沟通中',
        'pending': '待确认',
        'completed': '已完成'
      }
      return statusMap[status] || '未知'
    },
    
    // 跳转到档期管理
    goToSchedule() {
      const hostId = this.userInfo?.refId || this.userInfo?.userId
      if (hostId) {
        uni.navigateTo({
          url: `/pages/schedule/calendar?hostId=${hostId}`
        })
      } else {
        uni.showToast({
          title: '获取用户信息失败',
          icon: 'none'
        })
      }
    },
    
    // 跳转到订单
    goToOrders() {
      uni.switchTab({
        url: '/pages/mine/index'
      })
    },
    
    // 跳转到更多
    goToMore() {
      uni.switchTab({
        url: '/pages/mine/index'
      })
    },

    // 跳转到实时沟通列表
    goToChatList() {
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.showModal({
          title: '提示',
          content: '请先登录',
          success: (res) => {
            if (res.confirm) {
              uni.navigateTo({
                url: '/pages/login/index'
              })
            }
          }
        })
        return
      }
      uni.navigateTo({
        url: '/pages/chat/index'
      })
    },
    
    // ========== 消息相关方法 ==========
    
    // 加载未读消息数量
    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        if (res.code === 200 || res.code === '00000') {
          this.unreadCount = res.data.count || 0
        }
      } catch (error) {
        console.error('加载未读消息数量失败:', error)
      }
    },
    
    // 显示消息弹窗
    async showMessagePopup() {
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await getUnreadMessages()
        uni.hideLoading()
        
        if (res.code === 200 || res.code === '00000') {
          this.messages = res.data.messages || []
          
          if (this.messages.length === 0) {
            uni.showToast({
              title: '暂无未读消息',
              icon: 'none'
            })
            return
          }
          
          // 显示消息列表
          const content = this.messages.map((msg, index) =>
            `${index + 1}. ${msg.content}`
          ).join('\n\n')
          
          uni.showModal({
            title: `未读消息 (${this.messages.length})`,
            content: content,
            showCancel: false,
            confirmText: '知道了',
            success: async (modalRes) => {
              if (modalRes.confirm) {
                await this.markAllAsRead()
              }
            }
          })
        }
      } catch (error) {
        uni.hideLoading()
        console.error('加载消息失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },
    
    // 标记所有消息为已读
    async markAllAsRead() {
      try {
        const messageIds = this.messages.map(msg => msg.id)
        await markMessagesAsRead(messageIds)
        this.unreadCount = 0
        this.messages = []
      } catch (error) {
        console.error('标记已读失败:', error)
      }
    },
    
    // ========== 新人端方法 ==========
    
    // 加载标签列表
    async loadTagList() {
      try {
        const res = await getTagList("01")
        this.tagDict = res.data || []
      } catch (error) {
        console.error('加载标签列表失败:', error)
      }
    },
    
    // 根据标签code获取标签名称
    getTagNameByCode(tagCode) {
      const tag = this.tagDict.find(t => t.code === tagCode)
      return tag ? tag.name : tagCode
    },
    
    // 加载推荐主持人
    async loadRecommendHosts() {
      try {
        const res = await getHostList({ current: 1, size: 5 })
        this.recommendHosts = res.data.records || []
      } catch (error) {
        console.error('加载推荐主持人失败:', error)
      }
    },
    
    // 跳转到主持人列表
    goToHost() {
      uni.switchTab({
        url: '/pages/host/index'
      })
    },
    
    // 跳转到主持人详情
    goToHostDetail(id) {
      uni.navigateTo({
        url: `/pages/host/detail?id=${id}`
      })
    },
    
    // 跳转到问卷
    goToQuestionnaire() {
      uni.switchTab({
        url: '/pages/questionnaire/index'
      })
    },
    
    // 跳转到我的预约
    goToMyOrders() {
      uni.switchTab({
        url: '/pages/mine/index'
      })
    },
    
    // 客服咨询
    goToService() {
      uni.showModal({
        title: '客服咨询',
        content: '客服电话：400-123-4567\n工作时间：9:00-18:00',
        showCancel: false
      })
    },
    
    // 查看案例
    viewCase(id) {
      uni.showToast({
        title: '案例详情开发中',
        icon: 'none'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 100rpx;
}

// ========== 主持人工作台样式 ==========
.host-dashboard {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 100rpx;
}

.greeting-card {
  background: linear-gradient(135deg, #4169E1 0%, #5B8FF9 100%);
  padding: 40rpx 32rpx 50rpx;
  color: #ffffff;
  border-radius: 0 0 32rpx 32rpx;
  margin-bottom: 20rpx;
  
  .greeting-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .greeting-text {
      flex: 1;
      
      .greeting-time {
        font-size: 28rpx;
        opacity: 0.95;
      }
      
      .greeting-name {
        display: block;
        font-size: 44rpx;
        font-weight: bold;
        margin: 8rpx 0 12rpx;
      }
      
      .greeting-date {
        display: block;
        font-size: 24rpx;
        opacity: 0.85;
      }
    }
    
    .greeting-right {
      display: flex;
      align-items: center;
      gap: 24rpx;
      
      .message-bell {
        position: relative;
        width: 56rpx;
        height: 56rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        
        .bell-icon {
          font-size: 32rpx;
        }
        
        .red-dot {
          position: absolute;
          top: 8rpx;
          right: 8rpx;
          width: 16rpx;
          height: 16rpx;
          background: #ef4444;
          border-radius: 50%;
          border: 2rpx solid #ffffff;
        }
      }
      
      .voice-icon {
        font-size: 72rpx;
        opacity: 0.4;
      }
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  padding: 0 32rpx 24rpx;
  
  .stat-item {
    background: #ffffff;
    border-radius: 20rpx;
    padding: 28rpx 24rpx;
    box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
    
    .stat-label {
      display: block;
      font-size: 26rpx;
      color: #666666;
      margin-bottom: 16rpx;
    }
    
    .stat-value {
      display: block;
      font-size: 52rpx;
      font-weight: bold;
      color: #333333;
      line-height: 1.2;
      margin-bottom: 12rpx;
      
      &.income {
        font-size: 40rpx;
        color: #10b981;
      }
      
      &.rating {
        color: #f59e0b;
      }
    }
    
    .stat-value-row {
      display: flex;
      align-items: baseline;
      margin-bottom: 12rpx;
      
      .stat-value {
        margin-bottom: 0;
        margin-right: 8rpx;
      }
      
      .stat-badge {
        background: #ef4444;
        color: #ffffff;
        font-size: 20rpx;
        padding: 2rpx 8rpx;
        border-radius: 8rpx;
      }
      
      .star-icon {
        font-size: 32rpx;
        margin-left: 4rpx;
      }
    }
    
    .stat-trend {
      display: block;
      font-size: 22rpx;
      color: #999999;
      
      &.trend-up {
        color: #10b981;
      }
    }
  }
}

.quick-actions {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin: 0 32rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
  
  .section-title-small {
    display: block;
    font-size: 28rpx;
    font-weight: bold;
    color: #333333;
    margin-bottom: 24rpx;
  }
  
  .action-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20rpx;
    
    .action-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .action-icon {
        width: 88rpx;
        height: 88rpx;
        border-radius: 20rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 40rpx;
        margin-bottom: 12rpx;
        
        &.blue {
          background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
        }
        
        &.purple {
          background: linear-gradient(135deg, #F3E5F5 0%, #E1BEE7 100%);
        }
        
        &.green {
          background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%);
        }
        
        &.orange {
          background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%);
        }
      }
      
      .action-text {
        font-size: 24rpx;
        color: #333333;
      }
    }
  }
}

.section {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin: 0 32rpx 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.04);
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .section-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333333;
    }
    
    .section-more {
      font-size: 24rpx;
      color: #4169E1;
    }
  }
}

.pending-list {
  .pending-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    background: #f9fafb;
    border-radius: 16rpx;
    margin-bottom: 16rpx;
    position: relative;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .pending-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }
    
    .pending-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .pending-title {
        font-size: 28rpx;
        font-weight: bold;
        color: #333333;
        margin-bottom: 8rpx;
      }
      
      .pending-desc {
        font-size: 24rpx;
        color: #666666;
        margin-bottom: 8rpx;
      }
      
      .pending-time {
        font-size: 22rpx;
        color: #999999;
      }
    }
    
    .pending-badge {
      position: absolute;
      top: 16rpx;
      right: 48rpx;
      background: #ef4444;
      color: #ffffff;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 12rpx;
    }
    
    .pending-arrow {
      font-size: 40rpx;
      color: #d1d5db;
      margin-left: 12rpx;
    }
  }
}

.calendar-section {
  margin-bottom: 32rpx;
  
  .calendar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .calendar-title {
      font-size: 28rpx;
      font-weight: bold;
      color: #333333;
    }
    
    .calendar-nav {
      display: flex;
      gap: 16rpx;
      
      .nav-btn {
        width: 48rpx;
        height: 48rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f3f4f6;
        border-radius: 8rpx;
        font-size: 32rpx;
        color: #4169E1;
      }
    }
  }
  
  .calendar-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    margin-bottom: 12rpx;
    
    .weekday {
      text-align: center;
      font-size: 24rpx;
      color: #999999;
      padding: 12rpx 0;
    }
  }
  
  .calendar-days {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8rpx;
    
    .day-item {
      aspect-ratio: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border-radius: 12rpx;
      position: relative;
      
      .day-number {
        font-size: 26rpx;
        color: #333333;
      }
      
      .day-status {
        font-size: 18rpx;
        margin-top: 2rpx;
      }
      
      .day-dot {
        width: 6rpx;
        height: 6rpx;
        border-radius: 50%;
        background: #4169E1;
        margin-top: 4rpx;
      }
      
      &.other-month {
        .day-number {
          color: #d1d5db;
        }
      }
      
      &.today {
        background: #E3F2FD;
        
        .day-number {
          color: #4169E1;
          font-weight: bold;
        }
      }
      
      &.occupied {
        background: #4169E1;
        
        .day-number {
          color: #ffffff;
        }
        
        .day-status {
          color: #ffffff;
        }
        
        .day-dot {
          background: #ffffff;
        }
      }
      
      &.locked {
        background: #FFF3E0;
        
        .day-number {
          color: #f59e0b;
        }
        
        .day-status {
          color: #f59e0b;
        }
        
        .day-dot {
          background: #f59e0b;
        }
      }
    }
  }
}

.upcoming-weddings {
  .wedding-item {
    display: flex;
    align-items: center;
    padding: 24rpx;
    background: #f9fafb;
    border-radius: 16rpx;
    margin-bottom: 16rpx;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .wedding-date {
      width: 100rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #4169E1 0%, #5B8FF9 100%);
      border-radius: 16rpx;
      padding: 16rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
      
      .date-day {
        font-size: 48rpx;
        font-weight: bold;
        color: #ffffff;
        line-height: 1;
      }
      
      .date-month {
        font-size: 20rpx;
        color: #ffffff;
        opacity: 0.9;
        margin-top: 4rpx;
      }
    }
    
    .wedding-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .wedding-couple {
        font-size: 28rpx;
        font-weight: bold;
        color: #333333;
        margin-bottom: 8rpx;
      }
      
      .wedding-venue {
        font-size: 24rpx;
        color: #666666;
        margin-bottom: 6rpx;
      }
    }
    
    .wedding-status {
      padding: 8rpx 16rpx;
      border-radius: 12rpx;
      font-size: 22rpx;
      margin-left: 12rpx;
      
      &.status-confirmed {
        background: #d1fae5;
        color: #059669;
      }
      
      &.status-communicating {
        background: #E3F2FD;
        color: #4169E1;
      }
      
      &.status-pending {
        background: #FFF3E0;
        color: #f59e0b;
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
  
  .empty-icon {
    font-size: 120rpx;
    opacity: 0.3;
    margin-bottom: 24rpx;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

// ========== 新人端样式 ==========
.couple-home {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 100rpx;
}

.banner-section {
  width: 100%;
  height: 320rpx;
  
  .banner-swiper {
    width: 100%;
    height: 100%;
    
    .banner-card {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: 0 48rpx;
      box-sizing: border-box;
      
      .banner-title {
        font-size: 44rpx;
        font-weight: bold;
        color: #ffffff;
        margin-bottom: 16rpx;
      }
      
      .banner-subtitle {
        font-size: 28rpx;
        color: rgba(255, 255, 255, 0.9);
        font-style: italic;
      }
    }
    
    .banner-bg-1 {
      background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 50%, #60a5fa 100%);
    }
    
    .banner-bg-2 {
      background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 50%, #a78bfa 100%);
    }
    
    .banner-bg-3 {
      background: linear-gradient(135deg, #059669 0%, #10b981 50%, #34d399 100%);
    }
  }
}

.quick-entry {
  display: flex;
  justify-content: space-around;
  padding: 40rpx 32rpx;
  background-color: #ffffff;
  margin-bottom: 20rpx;
  
  .entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .entry-icon {
      width: 96rpx;
      height: 96rpx;
      background: linear-gradient(135deg, #4169E1 0%, #5B8FF9 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 48rpx;
      margin-bottom: 16rpx;
    }
    
    .entry-text {
      font-size: 24rpx;
      color: #333333;
    }
  }
}

.host-scroll {
  white-space: nowrap;
  
  .host-list {
    display: inline-flex;
    
    .host-card {
      display: inline-block;
      width: 280rpx;
      margin-right: 24rpx;
      background-color: #f5f7fa;
      border-radius: 16rpx;
      overflow: hidden;
      
      &:last-child {
        margin-right: 0;
      }
      
      .host-avatar {
        width: 100%;
        height: 280rpx;
      }
      
      .host-info {
        padding: 20rpx;
        
        .host-name {
          display: block;
          font-size: 28rpx;
          font-weight: bold;
          color: #333333;
          margin-bottom: 12rpx;
        }
        
        .host-tags {
          display: flex;
          flex-wrap: wrap;
          margin-bottom: 12rpx;
          
          .tag {
            font-size: 20rpx;
            color: #4169E1;
            background-color: rgba(65, 105, 225, 0.1);
            padding: 4rpx 12rpx;
            border-radius: 8rpx;
            margin-right: 8rpx;
            margin-bottom: 8rpx;
          }
        }
        
        .host-stats {
          display: flex;
          justify-content: space-between;
          
          .stat {
            font-size: 22rpx;
            color: #666666;
          }
        }
      }
    }
  }
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  
  .case-item {
    border-radius: 16rpx;
    overflow: hidden;
    background-color: #f5f7fa;
    
    .case-image {
      width: 100%;
      height: 240rpx;
    }
    
    .case-info {
      padding: 20rpx;
      
      .case-title {
        display: block;
        font-size: 26rpx;
        font-weight: bold;
        color: #333333;
        margin-bottom: 8rpx;
      }
      
      .case-date {
        font-size: 22rpx;
        color: #999999;
      }
    }
  }
}
</style>