import { get, post, put } from '@/utils/request'

/**
 * 获取订单列表（分页）
 */
export function getOrderList(params) {
  return get('/order/page', params)
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return get(`/order/${id}`)
}

/**
 * 创建订单
 */
export function createOrder(data) {
  return post('/order/create', data)
}

/**
 * 创建已支付订单（模拟微信支付后直接创建已支付订单）
 */
export function createPaidOrder(data) {
  return post('/order/createPaid', data)
}

/**
 * 更新订单状态
 */
export function updateOrderStatus(id, status) {
  return put(`/order/${id}/status?status=${status}`)
}

/**
 * 对已完成订单进行评分和评价
 * @param {number} id 订单ID
 * @param {number} rating 评分 1-5
 * @param {string} comment 评价内容（可选）
 */
export function rateOrder(id, rating, comment) {
  return post(`/order/${id}/rate`, { rating, comment })
}