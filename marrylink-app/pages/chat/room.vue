<template>
  <view class="chat-room">
    <!-- 聊天记录 -->
    <scroll-view
      class="message-area"
      scroll-y
      :scroll-into-view="scrollToId"
      :scroll-with-animation="true"
      @scrolltoupper="loadMoreHistory"
    >
      <view v-if="loadingHistory" class="loading-history">
        <text>加载更多...</text>
      </view>
      <view v-if="!hasMoreHistory && messages.length > 0" class="history-tip">
        <text>以上是全部聊天记录</text>
      </view>

      <view
        v-for="(msg, index) in messages"
        :key="msg.id || index"
        :id="'msg-' + (msg.id || index)"
        class="message-row"
        :class="{ 'message-self': msg.isSelf }"
      >
        <!-- 时间分割线 -->
        <view v-if="showTimeDivider(index)" class="time-divider">
          <text class="time-text">{{ formatMsgTime(msg.createTime) }}</text>
        </view>

        <view class="message-wrapper" :class="{ 'self': msg.isSelf }">
          <!-- 对方头像（左侧） -->
          <image
            v-if="!msg.isSelf"
            class="msg-avatar"
            :src="getAvatarUrl(targetAvatar)"
            mode="aspectFill"
          ></image>

          <view class="msg-body" :class="{ 'self': msg.isSelf }">
            <!-- 发送者名称 -->
            <text v-if="!msg.isSelf" class="msg-sender">{{ targetName }}</text>

            <!-- 消息内容 -->
            <view class="msg-bubble" :class="{ 'self-bubble': msg.isSelf }">
              <image
                v-if="msg.msgType === 'image'"
                class="msg-image"
                :src="getImageUrl(msg.content)"
                mode="widthFix"
                @click="previewImage(getImageUrl(msg.content))"
              ></image>
              <text v-else class="msg-text" :class="{ 'self-text': msg.isSelf }">{{ msg.content }}</text>
            </view>

            <!-- 消息状态 -->
            <view v-if="msg.isSelf" class="msg-status">
              <text v-if="msg.sending" class="status-sending">发送中</text>
              <text v-else-if="msg.sendFailed" class="status-failed" @click="resendMessage(msg)">发送失败，点击重试</text>
            </view>
          </view>

          <!-- 自己头像（右侧） -->
          <image
            v-if="msg.isSelf"
            class="msg-avatar"
            :src="getAvatarUrl(myAvatar)"
            mode="aspectFill"
          ></image>
        </view>
      </view>

      <!-- 对方正在输入 -->
      <view v-if="peerTyping" class="typing-indicator">
        <text class="typing-text">对方正在输入...</text>
      </view>

      <view id="msg-bottom" style="height: 20rpx;"></view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area" :style="{ paddingBottom: keyboardHeight + 'px' }">
      <view class="input-row">
        <view class="input-wrapper">
          <input
            class="msg-input"
            v-model="inputText"
            :placeholder="inputPlaceholder"
            :adjust-position="false"
            confirm-type="send"
            @confirm="handleSend"
            @focus="onInputFocus"
            @blur="onInputBlur"
            @input="onInputChange"
          />
        </view>
        <view class="extra-btns">
          <view class="extra-btn" @click="chooseImage">
            <text class="btn-icon">📷</text>
          </view>
        </view>
        <button
          class="send-btn"
          :class="{ 'active': inputText.trim() }"
          :disabled="!inputText.trim()"
          @click="handleSend"
        >
          发送
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getChatHistory, sendMessage, markConversationRead, getOrCreateConversation } from '@/api/chat'
import { BASE_URL } from '@/utils/request'
import wsManager from '@/utils/websocket'

export default {
  data() {
    return {
      conversationId: '',
      targetUserId: '',
      targetName: '',
      targetAvatar: '',
      myAvatar: '',
      messages: [],
      inputText: '',
      scrollToId: '',
      loadingHistory: false,
      hasMoreHistory: true,
      lastMsgId: '',
      peerTyping: false,
      typingTimer: null,
      keyboardHeight: 0,
      msgIdCounter: 0
    }
  },

  computed: {
    ...mapState('user', ['userInfo']),

    inputPlaceholder() {
      return `发消息给${this.targetName || '对方'}...`
    }
  },

  onLoad(options) {
    this.conversationId = options.conversationId || ''
    this.targetUserId = options.targetUserId || ''
    this.targetName = decodeURIComponent(options.targetName || '')
    this.targetAvatar = decodeURIComponent(options.targetAvatar || '')
    this.myAvatar = this.userInfo?.avatar || ''

    // 设置导航栏标题
    uni.setNavigationBarTitle({
      title: this.targetName || '聊天'
    })

    this.initChat()
  },

  onShow() {
    // 监听消息
    wsManager.on('CHAT', this.onReceiveMessage)
    wsManager.on('TYPING', this.onPeerTyping)
    wsManager.on('READ', this.onReadReceipt)

    // 确保 WebSocket 已连接
    if (!wsManager.isConnected) {
      wsManager.connect()
    }
  },

  onHide() {
    this.removeListeners()
  },

  onUnload() {
    this.removeListeners()
    if (this.typingTimer) {
      clearTimeout(this.typingTimer)
    }
  },

  methods: {
    // 移除监听器
    removeListeners() {
      wsManager.off('CHAT', this.onReceiveMessage)
      wsManager.off('TYPING', this.onPeerTyping)
      wsManager.off('READ', this.onReadReceipt)
    },

    // 初始化聊天
    async initChat() {
      // 如果没有 conversationId，需要先创建或获取
      if (!this.conversationId && this.targetUserId) {
        try {
          const res = await getOrCreateConversation(this.targetUserId)
          if (res.code === 200 || res.code === '00000') {
            this.conversationId = res.data.id
          }
        } catch (error) {
          console.error('创建会话失败:', error)
          uni.showToast({ title: '连接失败', icon: 'none' })
          return
        }
      }

      // 加载聊天记录
      await this.loadHistory()

      // 标记已读
      this.markRead()

      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },

    // 加载聊天记录
    async loadHistory() {
      if (this.loadingHistory || !this.conversationId) return

      try {
        this.loadingHistory = true
        const params = {
          current: 1,
          size: 30
        }
        if (this.lastMsgId) {
          params.lastMsgId = this.lastMsgId
        }

        const res = await getChatHistory(this.conversationId, params)
        if (res.code === 200 || res.code === '00000') {
          const records = (res.data.records || []).map(msg => ({
            ...msg,
            isSelf: msg.senderId === this.userInfo.accountId || msg.senderId === this.userInfo.refId
          }))

          // 历史记录是倒序的，需要反转
          records.reverse()

          if (this.lastMsgId) {
            // 加载更多历史记录，添加到前面
            this.messages = [...records, ...this.messages]
          } else {
            this.messages = records
          }

          this.hasMoreHistory = records.length >= 30
          if (records.length > 0) {
            this.lastMsgId = records[0].id
          }
        }
      } catch (error) {
        console.error('加载聊天记录失败:', error)
      } finally {
        this.loadingHistory = false
      }
    },

    // 加载更多历史记录
    loadMoreHistory() {
      if (this.hasMoreHistory && !this.loadingHistory) {
        this.loadHistory()
      }
    },

    // 发送消息
    async handleSend() {
      const content = this.inputText.trim()
      if (!content) return

      this.inputText = ''

      const tempId = 'temp_' + Date.now() + '_' + (++this.msgIdCounter)
      const newMsg = {
        id: tempId,
        conversationId: this.conversationId,
        senderId: this.userInfo.accountId || this.userInfo.refId,
        receiverId: this.targetUserId,
        content: content,
        msgType: 'text',
        createTime: new Date().toISOString(),
        isSelf: true,
        sending: true,
        sendFailed: false
      }

      this.messages.push(newMsg)
      this.scrollToBottom()

      // 优先通过 WebSocket 发送
      const sent = wsManager.sendChatMessage(
        this.conversationId,
        this.targetUserId,
        content,
        'text'
      )

      if (sent) {
        // WebSocket 发送成功
        const msgIndex = this.messages.findIndex(m => m.id === tempId)
        if (msgIndex > -1) {
          this.$set(this.messages, msgIndex, {
            ...this.messages[msgIndex],
            sending: false
          })
        }
      } else {
        // WebSocket 不可用，走 HTTP 备用通道
        try {
          const res = await sendMessage({
            conversationId: this.conversationId,
            receiverId: this.targetUserId,
            content: content,
            msgType: 'text'
          })

          if (res.code === 200 || res.code === '00000') {
            const msgIndex = this.messages.findIndex(m => m.id === tempId)
            if (msgIndex > -1) {
              this.$set(this.messages, msgIndex, {
                ...this.messages[msgIndex],
                id: res.data.id || tempId,
                sending: false
              })
            }
          }
        } catch (error) {
          console.error('发送消息失败:', error)
          const msgIndex = this.messages.findIndex(m => m.id === tempId)
          if (msgIndex > -1) {
            this.$set(this.messages, msgIndex, {
              ...this.messages[msgIndex],
              sending: false,
              sendFailed: true
            })
          }
        }
      }
    },

    // 重发消息
    async resendMessage(msg) {
      const msgIndex = this.messages.findIndex(m => m.id === msg.id)
      if (msgIndex === -1) return

      this.$set(this.messages, msgIndex, {
        ...this.messages[msgIndex],
        sending: true,
        sendFailed: false
      })

      try {
        const res = await sendMessage({
          conversationId: this.conversationId,
          receiverId: this.targetUserId,
          content: msg.content,
          msgType: msg.msgType || 'text'
        })

        if (res.code === 200 || res.code === '00000') {
          this.$set(this.messages, msgIndex, {
            ...this.messages[msgIndex],
            id: res.data.id || msg.id,
            sending: false,
            sendFailed: false
          })
        }
      } catch (error) {
        this.$set(this.messages, msgIndex, {
          ...this.messages[msgIndex],
          sending: false,
          sendFailed: true
        })
      }
    },

    // 选择图片发送
    chooseImage() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const tempFile = res.tempFilePaths[0]
          this.uploadAndSendImage(tempFile)
        }
      })
    },

    // 上传并发送图片
    async uploadAndSendImage(filePath) {
      const tempId = 'temp_img_' + Date.now()
      const newMsg = {
        id: tempId,
        conversationId: this.conversationId,
        senderId: this.userInfo.accountId || this.userInfo.refId,
        receiverId: this.targetUserId,
        content: filePath,
        msgType: 'image',
        createTime: new Date().toISOString(),
        isSelf: true,
        sending: true,
        sendFailed: false
      }

      this.messages.push(newMsg)
      this.scrollToBottom()

      const token = uni.getStorageSync('token')

      uni.uploadFile({
        url: BASE_URL + '/chat/upload',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': token ? `Bearer ${token}` : ''
        },
        success: (uploadRes) => {
          try {
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200 || data.code === '00000') {
              const imageUrl = data.data.url

              // 通过 WebSocket 或 HTTP 发送图片消息
              const sent = wsManager.sendChatMessage(
                this.conversationId,
                this.targetUserId,
                imageUrl,
                'image'
              )

              const msgIndex = this.messages.findIndex(m => m.id === tempId)
              if (msgIndex > -1) {
                this.$set(this.messages, msgIndex, {
                  ...this.messages[msgIndex],
                  content: imageUrl,
                  sending: false
                })
              }

              if (!sent) {
                // HTTP 备用
                sendMessage({
                  conversationId: this.conversationId,
                  receiverId: this.targetUserId,
                  content: imageUrl,
                  msgType: 'image'
                })
              }
            }
          } catch (e) {
            const msgIndex = this.messages.findIndex(m => m.id === tempId)
            if (msgIndex > -1) {
              this.$set(this.messages, msgIndex, {
                ...this.messages[msgIndex],
                sending: false,
                sendFailed: true
              })
            }
          }
        },
        fail: () => {
          const msgIndex = this.messages.findIndex(m => m.id === tempId)
          if (msgIndex > -1) {
            this.$set(this.messages, msgIndex, {
              ...this.messages[msgIndex],
              sending: false,
              sendFailed: true
            })
          }
          uni.showToast({ title: '图片上传失败', icon: 'none' })
        }
      })
    },

    // 收到新消息
    onReceiveMessage(data) {
      // 只处理当前会话的消息
      if (data.conversationId !== this.conversationId) return

      // 避免重复
      if (this.messages.some(m => m.id === data.id)) return

      this.messages.push({
        ...data,
        isSelf: false
      })

      this.scrollToBottom()
      this.markRead()
    },

    // 对方正在输入
    onPeerTyping(data) {
      if (data.conversationId !== this.conversationId) return

      this.peerTyping = true
      if (this.typingTimer) {
        clearTimeout(this.typingTimer)
      }
      this.typingTimer = setTimeout(() => {
        this.peerTyping = false
      }, 3000)
    },

    // 已读回执
    onReadReceipt(data) {
      if (data.conversationId !== this.conversationId) return
      // 可以更新消息的已读状态
    },

    // 标记会话已读
    markRead() {
      if (this.conversationId) {
        markConversationRead(this.conversationId).catch(() => {})
        wsManager.sendReadReceipt(this.conversationId)
      }
    },

    // 输入框获得焦点
    onInputFocus(e) {
      this.keyboardHeight = e.detail.height || 0
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },

    // 输入框失去焦点
    onInputBlur() {
      this.keyboardHeight = 0
    },

    // 输入内容变化
    onInputChange() {
      // 发送正在输入状态（节流）
      if (!this._lastTypingTime || Date.now() - this._lastTypingTime > 2000) {
        wsManager.sendTyping(this.conversationId)
        this._lastTypingTime = Date.now()
      }
    },

    // 获取图片完整URL
    getImageUrl(url) {
      if (!url) return ''
      if (url.startsWith('http')) return url
      // 本地临时文件路径（上传中的预览）
      if (url.startsWith('blob:') || url.startsWith('/tmp') || url.startsWith('file:') || url.indexOf('_doc/') > -1) return url
      return BASE_URL + url
    },

    // 预览图片
    previewImage(url) {
      uni.previewImage({
        current: url,
        urls: this.messages
          .filter(m => m.msgType === 'image')
          .map(m => this.getImageUrl(m.content))
      })
    },

    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        this.scrollToId = ''
        this.$nextTick(() => {
          this.scrollToId = 'msg-bottom'
        })
      })
    },

    // 获取头像完整URL
    getAvatarUrl(avatar) {
      if (!avatar) return '/static/default-avatar.png'
      if (avatar.startsWith('http')) return avatar
      return BASE_URL + avatar
    },

    // 是否显示时间分割线
    showTimeDivider(index) {
      if (index === 0) return true
      const current = new Date(this.messages[index].createTime).getTime()
      const prev = new Date(this.messages[index - 1].createTime).getTime()
      // 间隔超过 5 分钟显示时间
      return current - prev > 5 * 60 * 1000
    },

    // 格式化消息时间
    formatMsgTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const isToday = date.toDateString() === now.toDateString()

      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const timeStr = `${hours}:${minutes}`

      if (isToday) return timeStr

      const yesterday = new Date(now)
      yesterday.setDate(yesterday.getDate() - 1)
      if (date.toDateString() === yesterday.toDateString()) {
        return `昨天 ${timeStr}`
      }

      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}-${day} ${timeStr}`
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0f2f5;
}

.message-area {
  flex: 1;
  padding: 20rpx 0;
}

.loading-history,
.history-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #999999;
}

.time-divider {
  text-align: center;
  padding: 20rpx 0;

  .time-text {
    font-size: 22rpx;
    color: #b0b0b0;
    background-color: rgba(0, 0, 0, 0.05);
    padding: 6rpx 20rpx;
    border-radius: 12rpx;
  }
}

.message-row {
  padding: 0 24rpx;

  &.message-self {
    // 自己的消息靠右
  }
}

.message-wrapper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24rpx;

  &.self {
    flex-direction: row-reverse;
  }
}

.msg-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background-color: #e5e7eb;
}

.msg-body {
  max-width: 65%;
  margin: 0 16rpx;

  &.self {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  .msg-sender {
    font-size: 22rpx;
    color: #999999;
    margin-bottom: 8rpx;
    display: block;
  }
}

.msg-bubble {
  display: inline-block;
  padding: 20rpx 28rpx;
  background-color: #ffffff;
  border-radius: 4rpx 20rpx 20rpx 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  word-break: break-all;

  &.self-bubble {
    background-color: #1d4ed8;
    border-radius: 20rpx 4rpx 20rpx 20rpx;
  }
}

.msg-text {
  font-size: 30rpx;
  color: #333333;
  line-height: 1.6;

  &.self-text {
    color: #ffffff;
  }
}

.msg-image {
  max-width: 400rpx;
  min-width: 200rpx;
  border-radius: 16rpx;
}

.msg-status {
  margin-top: 6rpx;

  .status-sending {
    font-size: 20rpx;
    color: #999999;
  }

  .status-failed {
    font-size: 20rpx;
    color: #ef4444;
  }
}

.typing-indicator {
  padding: 16rpx 24rpx;

  .typing-text {
    font-size: 24rpx;
    color: #999999;
    font-style: italic;
  }
}

// 输入区域
.input-area {
  background-color: #ffffff;
  padding: 16rpx 24rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  transition: padding-bottom 0.2s;
}

.input-row {
  display: flex;
  align-items: center;
}

.input-wrapper {
  flex: 1;
  background-color: #f5f7fa;
  border-radius: 36rpx;
  padding: 0 28rpx;
  height: 72rpx;
  display: flex;
  align-items: center;

  .msg-input {
    width: 100%;
    height: 72rpx;
    font-size: 28rpx;
    color: #333333;
  }
}

.extra-btns {
  display: flex;
  margin: 0 12rpx;

  .extra-btn {
    width: 64rpx;
    height: 64rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    .btn-icon {
      font-size: 40rpx;
    }
  }
}

.send-btn {
  height: 68rpx;
  padding: 0 32rpx;
  background-color: #e5e7eb;
  color: #999999;
  border: none;
  border-radius: 34rpx;
  font-size: 28rpx;
  font-weight: 600;
  line-height: 68rpx;
  flex-shrink: 0;

  &.active {
    background-color: #1d4ed8;
    color: #ffffff;
  }

  &::after {
    border: none;
  }
}
</style>
