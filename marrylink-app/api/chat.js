import { get, post } from '@/utils/request'

/**
 * 获取会话列表
 * @param {Object} params - { current, size }
 */
export function getConversationList(params) {
  return get('/chat/conversations', params)
}

/**
 * 获取/创建与指定用户的会话
 * @param {String} targetUserId - 对方用户ID
 */
export function getOrCreateConversation(targetUserId) {
  return post('/chat/conversation', { targetUserId })
}

/**
 * 获取聊天记录（分页）
 * @param {String} conversationId - 会话ID
 * @param {Object} params - { current, size, lastMsgId }
 */
export function getChatHistory(conversationId, params) {
  return get(`/chat/history/${conversationId}`, params)
}

/**
 * 发送消息（HTTP 备用通道，WebSocket 不可用时使用）
 * @param {Object} data - { conversationId, receiverId, content, msgType }
 */
export function sendMessage(data) {
  return post('/chat/send', data)
}

/**
 * 标记会话消息为已读
 * @param {String} conversationId - 会话ID
 */
export function markConversationRead(conversationId) {
  return post(`/chat/read/${conversationId}`)
}

/**
 * 获取未读消息总数
 */
export function getChatUnreadCount() {
  return get('/chat/unread/count')
}
