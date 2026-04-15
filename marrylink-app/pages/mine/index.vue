<template>
  <view class="mine-container">
    <scroll-view class="mine-scroll" scroll-y>
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-info">
          <image
            class="user-avatar"
            :src="avatarUrl"
            mode="aspectFill"
          ></image>
          <view class="user-details">
            <text class="user-name">{{ userInfo.realName || userInfo.name || '未登录' }}</text>
            <text class="user-phone">{{ userInfo.phone || '点击登录' }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
      
      <!-- 我的订单 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">我的预约</text>
          <text class="section-more" @click="goToOrders()">查看全部 ›</text>
        </view>
        
        <view class="order-status">
          <view class="status-item" @click="goToOrders(1)">
            <view class="status-icon">⏳</view>
            <text class="status-text">待确认</text>
            <view class="status-badge" v-if="orderCount.pending > 0">{{ orderCount.pending }}</view>
          </view>
          <view class="status-item" @click="goToOrders(3)">
            <view class="status-icon">✅</view>
            <text class="status-text">已确认</text>
            <view class="status-badge" v-if="orderCount.confirmed > 0">{{ orderCount.confirmed }}</view>
          </view>
          <view class="status-item" @click="goToOrders(4)">
            <view class="status-icon">🎉</view>
            <text class="status-text">已完成</text>
          </view>
          <view class="status-item" @click="goToOrders(5)">
            <view class="status-icon">❌</view>
            <text class="status-text">已取消</text>
          </view>
        </view>
      </view>
      
      <!-- 月度订单统计（仅主持人） -->
      <view class="section" v-if="userInfo.userType === 'HOST'">
        <view class="section-header">
          <text class="section-title">月度订单统计</text>
        </view>
        
        <scroll-view class="chart-scroll" scroll-x>
          <view class="chart-container">
            <view class="chart-bars">
              <view
                class="bar-item"
                v-for="(item, index) in monthlyOrders"
                :key="index"
                @click="toggleBarSelection(item.month)"
                @longpress="toggleBarSelection(item.month)"
              >
                <view
                  class="bar"
                  :class="{ 'bar-active': selectedMonth === item.month }"
                  :style="{ height: getBarHeight(item.count) + 'rpx' }"
                >
                  <text v-if="selectedMonth === item.month" class="bar-value">{{ item.count }}</text>
                </view>
                <text class="bar-label">{{ item.month }}月</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
      
      <!-- 功能菜单 -->
      <view class="section">
        <view class="menu-list">
          <view class="menu-item" @click="goToMyQuestionnaires">
            <view class="menu-left">
              <text class="menu-icon">📝</text>
              <text class="menu-text">我的问卷</text>
            </view>
            <text class="menu-arrow">›</text>
          </view>
          
          <view class="menu-item" @click="goToMessages">
            <view class="menu-left">
              <text class="menu-icon">🔔</text>
              <text class="menu-text">消息通知</text>
              <view v-if="unreadCount > 0" class="menu-badge">{{ unreadCount }}</view>
            </view>
            <text class="menu-arrow">›</text>
          </view>

          <view class="menu-item" @click="goToChatList">
            <view class="menu-left">
              <text class="menu-icon">💬</text>
              <text class="menu-text">实时沟通</text>
              <view v-if="chatUnreadCount > 0" class="menu-badge">{{ chatUnreadCount }}</view>
            </view>
            <text class="menu-arrow">›</text>
          </view>
          
          <view class="menu-item" @click="goToSettings">
            <view class="menu-left">
              <text class="menu-icon">⚙️</text>
              <text class="menu-text">设置</text>
            </view>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>
      
      <!-- 帮助与反馈 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">帮助与反馈</text>
        </view>
        
        <view class="menu-list">
          <view class="menu-item" @click="goToHelp">
            <view class="menu-left">
              <text class="menu-icon">❓</text>
              <text class="menu-text">帮助中心</text>
            </view>
            <text class="menu-arrow">›</text>
          </view>
          
          <view class="menu-item" @click="goToFeedback">
            <view class="menu-left">
              <text class="menu-icon">💬</text>
              <text class="menu-text">意见反馈</text>
            </view>
            <text class="menu-arrow">›</text>
          </view>
          
          <view class="menu-item" @click="goToAbout">
            <view class="menu-left">
              <text class="menu-icon">ℹ️</text>
              <text class="menu-text">关于我们</text>
            </view>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>
      
      <!-- 退出登录 -->
      <view class="logout-section" v-if="isLoggedIn">
        <button class="btn-logout" @click="handleLogout">退出登录</button>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { getMonthlyOrders } from '@/api/host'
import { getUnreadCount } from '@/api/message'
import { getChatUnreadCount } from '@/api/chat'
import { BASE_URL } from '@/utils/request'

export default {
  data() {
    return {
      orderCount: {
        pending: 0,
        confirmed: 0
      },
      monthlyOrders: [],
      selectedMonth: null,
      unreadCount: 0,
      chatUnreadCount: 0
    }
  },
  
  computed: {
    ...mapState('user', ['userInfo', 'token']),
    
    isLoggedIn() {
      return !!this.token
    },
    
    avatarUrl() {
      if (!this.userInfo.avatar) {
        return '/static/default-avatar.png'
      }
      if (this.userInfo.avatar.startsWith('http')) {
        return this.userInfo.avatar
      }
      return BASE_URL + this.userInfo.avatar
    }
  },
  
  onLoad() {
    if (this.isLoggedIn) {
      this.loadOrderCount()
    }
  },
  
  onShow() {
    if (this.isLoggedIn) {
      this.loadOrderCount()
      this.loadUnreadCount()
      this.loadChatUnreadCount()
    }
  },
  
  methods: {
    ...mapActions('user', ['logout']),
    
    // 加载订单数量
    async loadOrderCount() {
      // 如果是主持人，加载月度统计
      if (this.userInfo.userType == 'HOST') {
        this.loadMonthlyOrders()
      }
    },
    
    // 加载月度订单统计
    async loadMonthlyOrders() {
      try {
        const result = await getMonthlyOrders()
        if (result.code === 200 || result.code === '00000') {
          this.monthlyOrders = result.data
        }
      } catch (error) {
        console.error('加载月度订单统计失败', error)
      }
    },
    
    // 计算柱状图高度
    getBarHeight(count) {
      const maxCount = Math.max(...this.monthlyOrders.map(item => item.count))
      const maxHeight = 300
      return maxCount > 0 ? (count / maxCount) * maxHeight : 0
    },
    
    // 切换柱状图选中状态
    toggleBarSelection(month) {
      if (this.selectedMonth === month) {
        this.selectedMonth = null
      } else {
        this.selectedMonth = month
      }
    },
    
    // 跳转到订单列表
    goToOrders(status) {
      if (!this.isLoggedIn) {
        uni.navigateTo({
          url: '/pages/login/index'
        })
        return
      }
      
      const url = status ? `/pages/order/index?status=${status}` : '/pages/order/index'
      uni.navigateTo({
        url
      })
    },
    
    // 跳转到我的问卷
    goToMyQuestionnaires() {
      if (!this.isLoggedIn) {
        uni.navigateTo({
          url: '/pages/login/index'
        })
        return
      }
      
      uni.switchTab({
        url: '/pages/questionnaire/index'
      })
    },
    
    // 跳转到消息通知
    goToMessages() {
      if (!this.isLoggedIn) {
        uni.navigateTo({
          url: '/pages/login/index'
        })
        return
      }
      
      uni.navigateTo({
        url: '/pages/message/list'
      })
    },
    
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

    // 加载聊天未读数量
    async loadChatUnreadCount() {
      try {
        const res = await getChatUnreadCount()
        if (res.code === 200 || res.code === '00000') {
          this.chatUnreadCount = res.data.count || 0
        }
      } catch (error) {
        console.error('加载聊天未读数量失败:', error)
      }
    },

    // 跳转到实时沟通列表
    goToChatList() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/index' })
        return
      }
      uni.navigateTo({ url: '/pages/chat/index' })
    },
    
    // 跳转到设置
    goToSettings() {
      if (!this.isLoggedIn) {
        uni.navigateTo({
          url: '/pages/login/index'
        })
        return
      }
      
      uni.navigateTo({
        url: '/pages/settings/index'
      })
    },
    
    // 跳转到帮助中心
    goToHelp() {
      uni.showToast({
        title: '功能开发中',
        icon: 'none'
      })
    },
    
    // 跳转到意见反馈
    goToFeedback() {
      uni.showToast({
        title: '功能开发中',
        icon: 'none'
      })
    },
    
    // 跳转到关于我们
    goToAbout() {
      uni.showModal({
        title: '关于 MarryLink',
        content: 'MarryLink v1.0.0\n让每一场婚礼都完美呈现',
        showCancel: false
      })
    },
    
    // 退出登录
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.logout()
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.mine-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.mine-scroll {
  height: 100vh;
}

.user-card {
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  padding: 60rpx 32rpx 40rpx;
  margin-bottom: 20rpx;
  
  .user-info {
    display: flex;
    align-items: center;
    
    .user-avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      border: 4rpx solid rgba(255, 255, 255, 0.3);
      margin-right: 24rpx;
    }
    
    .user-details {
      flex: 1;
      
      .user-name {
        display: block;
        font-size: 36rpx;
        font-weight: bold;
        color: #ffffff;
        margin-bottom: 8rpx;
      }
      
      .user-phone {
        font-size: 26rpx;
        color: rgba(255, 255, 255, 0.8);
      }
    }
    
    .arrow {
      font-size: 48rpx;
      color: rgba(255, 255, 255, 0.6);
      font-weight: 300;
    }
  }
}

.section {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 20rpx;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;
    
    .section-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333333;
      position: relative;
      padding-left: 20rpx;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 8rpx;
        height: 32rpx;
        background-color: #1d4ed8;
        border-radius: 4rpx;
      }
    }
    
    .section-more {
      font-size: 24rpx;
      color: #1d4ed8;
    }
  }
  
  .order-status {
    display: flex;
    justify-content: space-around;
    
    .status-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;
      
      .status-icon {
        width: 96rpx;
        height: 96rpx;
        background-color: #f5f7fa;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 48rpx;
        margin-bottom: 12rpx;
      }
      
      .status-text {
        font-size: 24rpx;
        color: #666666;
      }
      
      .status-badge {
        position: absolute;
        top: 0;
        right: 0;
        min-width: 32rpx;
        height: 32rpx;
        line-height: 32rpx;
        padding: 0 8rpx;
        background-color: #ef4444;
        color: #ffffff;
        border-radius: 16rpx;
        font-size: 20rpx;
        text-align: center;
      }
    }
  }
  
  .menu-list {
    .menu-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 28rpx 0;
      border-bottom: 1rpx solid #f5f7fa;
      
      &:last-child {
        border-bottom: none;
      }
      
      .menu-left {
        display: flex;
        align-items: center;
        position: relative;
        
        .menu-icon {
          font-size: 40rpx;
          margin-right: 20rpx;
        }
        
        .menu-text {
          font-size: 28rpx;
          color: #333333;
        }
        
        .menu-badge {
          position: absolute;
          top: -8rpx;
          left: 48rpx;
          min-width: 32rpx;
          height: 32rpx;
          line-height: 32rpx;
          padding: 0 8rpx;
          background-color: #ef4444;
          color: #ffffff;
          border-radius: 16rpx;
          font-size: 20rpx;
          text-align: center;
        }
      }
      
      .menu-arrow {
        font-size: 48rpx;
        color: #cccccc;
        font-weight: 300;
      }
    }
  }
}

.chart-scroll {
  width: 100%;
  white-space: nowrap;
}

.chart-container {
  display: inline-block;
  padding: 20rpx 0;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  height: 400rpx;
  padding: 0 20rpx;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  margin: 0 20rpx;
  
  .bar {
    width: 60rpx;
    background: linear-gradient(180deg, #93c5fd 0%, #60a5fa 100%);
    border-radius: 30rpx 30rpx 0 0;
    min-height: 40rpx;
    margin-bottom: 16rpx;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding-top: 12rpx;
    transition: all 0.3s ease;
    
    &.bar-active {
      background: linear-gradient(180deg, #1d4ed8 0%, #3b82f6 100%);
    }
    
    .bar-value {
      font-size: 24rpx;
      color: #ffffff;
      font-weight: bold;
    }
  }
  
  .bar-label {
    font-size: 26rpx;
    color: #666666;
  }
}

.logout-section {
  padding: 32rpx;
  
  .btn-logout {
    width: 100%;
    height: 80rpx;
    background-color: #ffffff;
    color: #ef4444;
    border: 2rpx solid #ef4444;
    border-radius: 40rpx;
    font-size: 28rpx;
    line-height: 80rpx;
  }
}
</style>