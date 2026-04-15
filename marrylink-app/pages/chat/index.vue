<template>
  <view class="chat-container">
    <!-- 会话列表 -->
    <scroll-view
      class="conversation-list"
      scroll-y
      @scrolltolower="loadMore"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="conversations.length === 0 && !loading" class="empty-state">
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无聊天记录</text>
        <text class="empty-tip">{{ isHost ? '等待新人发起咨询' : '去主持人详情页发起咨询吧' }}</text>
      </view>

      <view
        v-else
        class="conversation-item"
        v-for="item in conversations"
        :key="item.id"
        @click="goToChat(item)"
      >
        <view class="avatar-wrapper">
          <image
            class="conv-avatar"
            :src="getAvatarUrl(item.targetAvatar)"
            mode="aspectFill"
          ></image>
          <view v-if="item.unreadCount > 0" class="unread-badge">
            {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
          </view>
        </view>
        <view class="conv-content">
          <view class="conv-top">
            <text class="conv-name">{{ item.targetName || '未知用户' }}</text>
            <text class="conv-time">{{ formatTime(item.lastMsgTime) }}</text>
          </view>
          <view class="conv-bottom">
            <text class="conv-last-msg" :class="{ 'has-unread': item.unreadCount > 0 }">
              {{ item.lastMsgContent || '暂无消息' }}
            </text>
            <view v-if="item.targetUserType === 'HOST'" class="role-tag host-tag">主持人</view>
            <view v-else-if="item.targetUserType === 'CUSTOMER'" class="role-tag customer-tag">新人</view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-more">
        <text>加载中...</text>
      </view>

      <view v-if="!hasMore && conversations.length > 0" class="no-more">
        <text>没有更多了</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getConversationList, getChatUnreadCount } from '@/api/chat'
import { BASE_URL } from '@/utils/request'
import wsManager from '@/utils/websocket'

export default {
  data() {
    return {
      conversations: [],
      current: 1,
      size: 20,
      loading: false,
      refreshing: false,
      hasMore: true,
      totalUnread: 0
    }
  },

  computed: {
    ...mapState('user', ['userInfo']),

    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    }
  },

  onLoad() {
    this.initWebSocket()
    this.loadConversations()
  },

  onShow() {
    // 每次进入页面时刷新列表
    this.refreshList()
  },

  onHide() {
    // 离开页面时移除消息监听
    wsManager.off('CHAT', this.onNewMessage)
  },

  onUnload() {
    wsManager.off('CHAT', this.onNewMessage)
  },

  methods: {
    // 初始化 WebSocket
    initWebSocket() {
      wsManager.connect()
      wsManager.on('CHAT', this.onNewMessage)
    },

    // 收到新消息时更新会话列表
    onNewMessage(data) {
      const convIndex = this.conversations.findIndex(
        c => c.id === data.conversationId
      )

      if (convIndex > -1) {
        // 更新已有会话
        const conv = { ...this.conversations[convIndex] }
        conv.lastMsgContent = data.msgType === 'image' ? '[图片]' : data.content
        conv.lastMsgTime = data.createTime || new Date().toISOString()
        conv.unreadCount = (conv.unreadCount || 0) + 1

        // 移到顶部
        this.conversations.splice(convIndex, 1)
        this.conversations.unshift(conv)
      } else {
        // 新会话，刷新整个列表
        this.refreshList()
      }
    },

    // 刷新列表
    refreshList() {
      this.current = 1
      this.conversations = []
      this.hasMore = true
      this.loadConversations()
    },

    // 加载会话列表
    async loadConversations() {
      if (this.loading || !this.hasMore) return

      try {
        this.loading = true
        const res = await getConversationList({
          current: this.current,
          size: this.size
        })

        if (res.code === 200 || res.code === '00000') {
          const newList = res.data.records || []
          this.conversations = this.current === 1 ? newList : [...this.conversations, ...newList]
          this.hasMore = this.conversations.length < (res.data.total || 0)
          this.current++
        }
      } catch (error) {
        console.error('加载会话列表失败:', error)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.refreshList()
    },

    // 加载更多
    loadMore() {
      if (!this.loading && this.hasMore) {
        this.loadConversations()
      }
    },

    // 跳转到聊天页
    goToChat(item) {
      uni.navigateTo({
        url: `/pages/chat/room?conversationId=${item.id}&targetUserId=${item.targetUserId}&targetName=${encodeURIComponent(item.targetName || '未知用户')}&targetAvatar=${encodeURIComponent(item.targetAvatar || '')}`
      })
    },

    // 获取头像完整URL
    getAvatarUrl(avatar) {
      if (!avatar) return '/static/default-avatar.png'
      if (avatar.startsWith('http')) return avatar
      return BASE_URL + avatar
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now.getTime() - date.getTime()
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)

      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 1) return '昨天'
      if (days < 7) return `${days}天前`

      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}-${day}`
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.conversation-list {
  height: 100vh;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  background-color: #ffffff;
  border-bottom: 1rpx solid #f0f0f0;

  &:active {
    background-color: #f5f7fa;
  }
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
  margin-right: 24rpx;

  .conv-avatar {
    width: 96rpx;
    height: 96rpx;
    border-radius: 50%;
    background-color: #e5e7eb;
  }

  .unread-badge {
    position: absolute;
    top: -8rpx;
    right: -8rpx;
    min-width: 36rpx;
    height: 36rpx;
    line-height: 36rpx;
    padding: 0 10rpx;
    background-color: #ef4444;
    color: #ffffff;
    border-radius: 18rpx;
    font-size: 22rpx;
    text-align: center;
  }
}

.conv-content {
  flex: 1;
  overflow: hidden;

  .conv-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;

    .conv-name {
      font-size: 30rpx;
      font-weight: 600;
      color: #333333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 400rpx;
    }

    .conv-time {
      font-size: 22rpx;
      color: #999999;
      flex-shrink: 0;
    }
  }

  .conv-bottom {
    display: flex;
    align-items: center;

    .conv-last-msg {
      flex: 1;
      font-size: 26rpx;
      color: #999999;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      &.has-unread {
        color: #666666;
        font-weight: 500;
      }
    }

    .role-tag {
      flex-shrink: 0;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
      margin-left: 12rpx;

      &.host-tag {
        color: #1d4ed8;
        background-color: rgba(29, 78, 216, 0.1);
      }

      &.customer-tag {
        color: #059669;
        background-color: rgba(5, 150, 105, 0.1);
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;

  .empty-icon {
    font-size: 120rpx;
    opacity: 0.3;
    margin-bottom: 24rpx;
  }

  .empty-text {
    font-size: 32rpx;
    color: #999999;
    margin-bottom: 12rpx;
  }

  .empty-tip {
    font-size: 24rpx;
    color: #cccccc;
  }
}

.loading-more,
.no-more {
  text-align: center;
  padding: 32rpx 0;
  font-size: 24rpx;
  color: #999999;
}
</style>
