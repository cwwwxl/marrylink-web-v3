/**
 * WebSocket 连接管理器
 * 用于新人端和主持人端的实时沟通
 */

const getWsBaseUrl = () => {
  const isDev = process.env.NODE_ENV === 'development'
  if (isDev) {
    return 'ws://localhost:8080/api/v1/ws/chat'
  } else {
    // 生产环境：由 nginx 代理 WebSocket
    const protocol = 'wss'
    return `${protocol}://${location.host}/app-api/ws/chat`
  }
}

class WebSocketManager {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.reconnectCount = 0
    this.maxReconnect = 5
    this.reconnectInterval = 3000
    this.heartbeatInterval = 30000
    this.listeners = {}
  }

  /**
   * 建立 WebSocket 连接
   */
  connect() {
    if (this.isConnected) return

    const token = uni.getStorageSync('token')
    if (!token) {
      console.warn('[WebSocket] 未登录，无法建立连接')
      return
    }

    const url = `${getWsBaseUrl()}?token=${encodeURIComponent(token)}`

    this.socketTask = uni.connectSocket({
      url,
      success: () => {
        console.log('[WebSocket] 正在连接...')
      },
      fail: (err) => {
        console.error('[WebSocket] 连接失败:', err)
        this._tryReconnect()
      }
    })

    this.socketTask.onOpen(() => {
      console.log('[WebSocket] 连接成功')
      this.isConnected = true
      this.reconnectCount = 0
      this._startHeartbeat()
      this._emit('open')
    })

    this.socketTask.onMessage((res) => {
      try {
        const data = JSON.parse(res.data)
        this._emit('message', data)

        // 根据消息类型分发事件
        if (data.type) {
          this._emit(data.type, data)
        }
      } catch (e) {
        console.warn('[WebSocket] 消息解析失败:', res.data)
      }
    })

    this.socketTask.onClose((res) => {
      console.log('[WebSocket] 连接关闭:', res)
      this.isConnected = false
      this._stopHeartbeat()
      this._emit('close', res)
      this._tryReconnect()
    })

    this.socketTask.onError((err) => {
      console.error('[WebSocket] 连接错误:', err)
      this.isConnected = false
      this._emit('error', err)
    })
  }

  /**
   * 发送消息
   * @param {Object} data - 消息数据
   */
  send(data) {
    if (!this.isConnected || !this.socketTask) {
      console.warn('[WebSocket] 未连接，无法发送消息')
      return false
    }

    this.socketTask.send({
      data: JSON.stringify(data),
      success: () => {},
      fail: (err) => {
        console.error('[WebSocket] 发送失败:', err)
      }
    })
    return true
  }

  /**
   * 发送聊天消息
   * @param {String} conversationId - 会话ID
   * @param {String} receiverId - 接收者ID
   * @param {String} content - 消息内容
   * @param {String} msgType - 消息类型 text/image
   */
  sendChatMessage(conversationId, receiverId, content, msgType = 'text') {
    return this.send({
      type: 'CHAT',
      conversationId,
      receiverId,
      content,
      msgType
    })
  }

  /**
   * 发送已读回执
   * @param {String} conversationId - 会话ID
   */
  sendReadReceipt(conversationId) {
    return this.send({
      type: 'READ',
      conversationId
    })
  }

  /**
   * 发送正在输入状态
   * @param {String} conversationId - 会话ID
   */
  sendTyping(conversationId) {
    return this.send({
      type: 'TYPING',
      conversationId
    })
  }

  /**
   * 关闭连接
   */
  close() {
    this._stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectCount = this.maxReconnect // 防止自动重连
    if (this.socketTask) {
      this.socketTask.close()
      this.socketTask = null
    }
    this.isConnected = false
  }

  /**
   * 注册事件监听
   * @param {String} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  }

  /**
   * 移除事件监听
   * @param {String} event - 事件名称
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    if (!this.listeners[event]) return
    if (callback) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    } else {
      delete this.listeners[event]
    }
  }

  /**
   * 触发事件
   */
  _emit(event, data) {
    const callbacks = this.listeners[event]
    if (callbacks) {
      callbacks.forEach(cb => {
        try {
          cb(data)
        } catch (e) {
          console.error(`[WebSocket] 事件处理错误 (${event}):`, e)
        }
      })
    }
  }

  /**
   * 开始心跳
   */
  _startHeartbeat() {
    this._stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.send({ type: 'PING' })
      }
    }, this.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  _stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 尝试重连
   */
  _tryReconnect() {
    if (this.reconnectCount >= this.maxReconnect) {
      console.warn('[WebSocket] 达到最大重连次数，停止重连')
      return
    }

    if (this.reconnectTimer) return

    this.reconnectCount++
    console.log(`[WebSocket] ${this.reconnectInterval / 1000}秒后尝试第${this.reconnectCount}次重连...`)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, this.reconnectInterval)
  }
}

// 导出单例
const wsManager = new WebSocketManager()
export default wsManager
