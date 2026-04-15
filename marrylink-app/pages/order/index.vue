<template>
  <view class="order-container">
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentStatus === tab.value }"
        @click="changeTab(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>
    
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view v-if="orderList.length > 0">
        <view
          v-for="order in orderList"
          :key="order.id"
          class="order-card"
        >
          <view class="order-header">
            <text class="order-no">订单号: {{ order.orderNo }}</text>
            <text class="order-status" :class="'status-' + order.status">
              {{ getStatusText(order.status) }}
            </text>
          </view>
          
          <view class="order-info">
            <view class="info-row">
              <text class="label">主持人:</text>
              <text class="value">{{ order.hostName }}</text>
            </view>
            <view class="info-row">
              <text class="label">婚礼日期:</text>
              <text class="value">{{ order.weddingDate }}</text>
            </view>
            <view class="info-row">
              <text class="label">婚礼类型:</text>
              <text class="value">{{ getWeddingTypeName(order.weddingType) }}</text>
            </view>
            <view class="info-row">
              <text class="label">订单金额:</text>
              <text class="value amount">¥{{ order.amount }}</text>
            </view>
            <view class="info-row">
              <text class="label">创建日期:</text>
              <text class="value">{{ formatDate(order.createTime) }}</text>
            </view>
          </view>
          
          <!-- 已完成订单的评价展示区域（所有用户可见） -->
          <view v-if="order.status === 4 && order.rating" class="rating-section">
            <view class="rating-divider"></view>
            <!-- 已评分：展示评分结果和评价内容 -->
            <view class="rating-display">
              <view class="rating-header">
                <text class="rating-label">用户评价</text>
                <view class="stars-row">
                  <text
                    v-for="star in 5"
                    :key="star"
                    class="star-icon rated"
                    :class="{ filled: star <= order.rating }"
                  >★</text>
                </view>
              </view>
              <view v-if="order.comment" class="comment-display">
                <text class="comment-text">{{ order.comment }}</text>
              </view>
            </view>
          </view>
          
          <!-- 已完成订单的评分操作区域（仅新人可评分，且未评价时显示） -->
          <view v-if="order.status === 4 && !order.rating && userInfo && userInfo.userType === 'CUSTOMER'" class="rating-section">
            <view class="rating-divider"></view>
            <!-- 未评分：可点击评分 -->
            <view class="rating-action">
              <text class="rating-label">评价主持人</text>
              <view class="stars-row">
                <text
                  v-for="star in 5"
                  :key="star"
                  class="star-icon selectable"
                  :class="{ filled: star <= (hoverRating[order.id] || 0) }"
                  @click.stop="selectStar(order, star)"
                >★</text>
              </view>
            </view>
          </view>
          
          <!-- 主持人操作区域：变更订单状态 -->
          <view v-if="isHost" class="host-action-section">
            <view class="rating-divider"></view>
            <view class="action-buttons">
              <view class="action-btn primary" @click.stop="showStatusActionSheet(order)">
                变更状态
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view v-else class="empty">
        <text class="empty-text">暂无订单</text>
      </view>
      
      <view v-if="loading" class="loading">加载中...</view>
      <view v-if="noMore && orderList.length > 0" class="no-more">没有更多了</view>
    </scroll-view>
    
    <!-- 评分确认弹窗 -->
    <view v-if="showRatingModal" class="modal-mask" @click="cancelRating">
      <view class="modal-content" @click.stop>
        <text class="modal-title">评价主持人</text>
        <text class="modal-desc">为主持人 {{ ratingOrder.hostName }} 评分</text>
        <view class="modal-stars">
          <text
            v-for="star in 5"
            :key="star"
            class="star-icon modal-star"
            :class="{ filled: star <= pendingRating }"
            @click="pendingRating = star"
          >★</text>
        </view>
        <text class="modal-hint">{{ pendingRating }}星 - {{ getRatingText(pendingRating) }}</text>
        <view class="comment-input-section">
          <text class="comment-label">写评价（选填）</text>
          <textarea
            class="comment-textarea"
            v-model="pendingComment"
            placeholder="分享您对主持人的评价..."
            maxlength="200"
          />
          <text class="comment-count">{{ pendingComment.length }}/200</text>
        </view>
        <view class="modal-buttons">
          <view class="modal-btn cancel" @click="cancelRating">取消</view>
          <view class="modal-btn confirm" @click="confirmRating">确认评价</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getOrderList, rateOrder, updateOrderStatus } from '@/api/order'
import { getTagList } from '@/api/host'

export default {
  data() {
    return {
      tabs: [
        { label: '全部', value: null },
        { label: '待确认', value: 1 },
        { label: '已支付', value: 3 },
        { label: '已完成', value: 4 },
        { label: '已取消', value: 5 }
      ],
      currentStatus: null,
      orderList: [],
      current: 1,
      size: 10,
      total: 0,
      loading: false,
      noMore: false,
      weddingTypeDict: [],
      // 评分相关
      hoverRating: {},
      showRatingModal: false,
      ratingOrder: null,
      pendingRating: 0,
      pendingComment: ''
    }
  },
  
  computed: {
    ...mapState('user', ['userInfo']),
    isHost() {
      return this.userInfo && this.userInfo.userType === 'HOST'
    }
  },
  
  async onLoad(options) {
    if (options.status) {
      this.currentStatus = parseInt(options.status)
    }
    await this.loadWeddingTypeDict()
    this.loadOrderList()
  },
  
  methods: {
    async loadWeddingTypeDict() {
      try {
        const res = await getTagList('01')
        if (res.code === 200 || res.code === '00000') {
          this.weddingTypeDict = res.data || []
        }
      } catch (error) {
        console.error('加载婚礼类型字典失败', error)
      }
    },
    
    async loadOrderList() {
      if (this.loading || this.noMore) return
      
      this.loading = true
      try {
        const params = {
          current: this.current,
          size: this.size
        }
        
        if (this.currentStatus !== null) {
          params.status = this.currentStatus
        }
        
        const res = await getOrderList(params)
        
        if (res.code === 200 || res.code === '00000') {
          const newList = res.data.records || []
          this.orderList = this.current === 1 ? newList : [...this.orderList, ...newList]
          this.total = res.data.total
          this.noMore = this.orderList.length >= this.total
        }
      } catch (error) {
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },
    
    changeTab(status) {
      this.currentStatus = status
      this.current = 1
      this.orderList = []
      this.noMore = false
      this.loadOrderList()
    },
    
    loadMore() {
      if (!this.noMore && !this.loading) {
        this.current++
        this.loadOrderList()
      }
    },
    
    getStatusText(status) {
      const statusMap = {
        1: '待确认',
        3: '已支付',
        4: '已完成',
        5: '已取消'
      }
      return statusMap[status] || '未知'
    },
    
    getWeddingTypeName(code) {
      if (!code) return ''
      const tag = this.weddingTypeDict.find(t => t.code === code)
      return tag ? tag.name : code
    },
    
    formatDate(dateTime) {
      if (!dateTime) return ''
      return dateTime.split('T')[0]
    },
    
    // ---- 评分相关方法 ----
    selectStar(order, star) {
      this.$set(this.hoverRating, order.id, star)
      this.ratingOrder = order
      this.pendingRating = star
      this.showRatingModal = true
    },
    
    cancelRating() {
      this.showRatingModal = false
      if (this.ratingOrder) {
        this.$set(this.hoverRating, this.ratingOrder.id, 0)
      }
      this.ratingOrder = null
      this.pendingRating = 0
      this.pendingComment = ''
    },
    
    async confirmRating() {
      if (!this.ratingOrder || this.pendingRating < 1) return
      
      try {
        const res = await rateOrder(this.ratingOrder.id, this.pendingRating, this.pendingComment)
        if (res.code === 200 || res.code === '00000') {
          uni.showToast({ title: '评价成功', icon: 'success' })
          // 更新本地订单数据
          const idx = this.orderList.findIndex(o => o.id === this.ratingOrder.id)
          if (idx !== -1) {
            this.$set(this.orderList[idx], 'rating', this.pendingRating)
            this.$set(this.orderList[idx], 'comment', this.pendingComment)
          }
        }
      } catch (error) {
        // request.js 已处理错误提示
      } finally {
        this.showRatingModal = false
        this.ratingOrder = null
        this.pendingRating = 0
        this.pendingComment = ''
      }
    },
    
    getRatingText(rating) {
      const texts = { 1: '很差', 2: '较差', 3: '一般', 4: '满意', 5: '非常满意' }
      return texts[rating] || ''
    },
    
    // ---- 主持人状态变更相关 ----
    showStatusActionSheet(order) {
      const statusOptions = [
        { status: 1, label: '待确认' },
        { status: 3, label: '已支付' },
        { status: 4, label: '已完成' },
        { status: 5, label: '已取消' }
      ]
      // 过滤掉当前状态
      const items = statusOptions.filter(s => s.status !== order.status)
      
      uni.showActionSheet({
        itemList: items.map(s => s.label),
        success: (res) => {
          const selected = items[res.tapIndex]
          this.confirmStatusChange(order, selected.status, selected.label)
        }
      })
    },
    
    confirmStatusChange(order, newStatus, label) {
      uni.showModal({
        title: '确认操作',
        content: `确定要将订单「${order.orderNo}」状态变更为「${label}」吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await updateOrderStatus(order.id, newStatus)
              if (result.code === 200 || result.code === '00000') {
                uni.showToast({ title: '操作成功', icon: 'success' })
                const idx = this.orderList.findIndex(o => o.id === order.id)
                if (idx !== -1) {
                  this.$set(this.orderList[idx], 'status', newStatus)
                }
              }
            } catch (error) {
              // request.js 已处理错误提示
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.tabs {
  display: flex;
  background-color: #ffffff;
  padding: 20rpx 0;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  
  .tab-item {
    flex: 1;
    text-align: center;
    font-size: 28rpx;
    color: #666666;
    padding: 16rpx 0;
    position: relative;
    
    &.active {
      color: #1d4ed8;
      font-weight: bold;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 6rpx;
        background-color: #1d4ed8;
        border-radius: 3rpx;
      }
    }
  }
}

.order-list {
  flex: 1;
  padding: 20rpx;
}

.order-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f5f7fa;
    margin-bottom: 20rpx;
    
    .order-no {
      font-size: 24rpx;
      color: #999999;
    }
    
    .order-status {
      font-size: 26rpx;
      font-weight: bold;
      padding: 8rpx 20rpx;
      border-radius: 20rpx;
      
      &.status-1 {
        color: #f59e0b;
        background-color: #fef3c7;
      }
      
      &.status-3 {
        color: #3b82f6;
        background-color: #dbeafe;
      }
      
      &.status-4 {
        color: #10b981;
        background-color: #d1fae5;
      }
      
      &.status-5 {
        color: #ef4444;
        background-color: #fee2e2;
      }
    }
  }
  
  .order-info {
    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12rpx 0;
      
      .label {
        font-size: 28rpx;
        color: #666666;
      }
      
      .value {
        font-size: 28rpx;
        color: #333333;
        
        &.amount {
          color: #1d4ed8;
          font-weight: bold;
          font-size: 32rpx;
        }
      }
    }
  }
}

.empty {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 200rpx 0;
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.loading, .no-more {
  text-align: center;
  padding: 30rpx 0;
  font-size: 24rpx;
  color: #999999;
}

// ---- 主持人操作区域样式 ----
.host-action-section {
  .rating-divider {
    height: 1rpx;
    background-color: #f0f2f5;
    margin: 20rpx 0;
  }
  
  .action-buttons {
    display: flex;
    justify-content: flex-end;
    
    .action-btn {
      text-align: center;
      padding: 12rpx 40rpx;
      border-radius: 12rpx;
      font-size: 28rpx;
      font-weight: bold;
      
      &.primary {
        background-color: #1d4ed8;
        color: #ffffff;
      }
    }
  }
}

// ---- 评分区域样式 ----
.rating-section {
  .rating-divider {
    height: 1rpx;
    background-color: #f0f2f5;
    margin: 20rpx 0;
  }
  
  .rating-display {
    .rating-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8rpx 0;
    }
    
    .comment-display {
      margin-top: 16rpx;
      padding: 16rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      
      .comment-text {
        font-size: 26rpx;
        color: #666666;
        line-height: 1.6;
      }
    }
  }
  
  .rating-action {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8rpx 0;
  }
  
  .rating-label {
    font-size: 28rpx;
    color: #666666;
  }
  
  .stars-row {
    display: flex;
    gap: 8rpx;
  }
  
  .star-icon {
    font-size: 40rpx;
    color: #d4d4d4;
    
    &.filled {
      color: #f59e0b;
    }
    
    &.selectable {
      color: #d4d4d4;
      
      &.filled {
        color: #f59e0b;
      }
    }
    
    &.rated {
      color: #d4d4d4;
      
      &.filled {
        color: #f59e0b;
      }
    }
  }
}

// ---- 评分弹窗样式 ----
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-content {
  width: 600rpx;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .modal-title {
    font-size: 36rpx;
    font-weight: bold;
    color: #1d4ed8;
    margin-bottom: 16rpx;
  }
  
  .modal-desc {
    font-size: 28rpx;
    color: #666666;
    margin-bottom: 32rpx;
  }
  
  .modal-stars {
    display: flex;
    gap: 20rpx;
    margin-bottom: 16rpx;
    
    .modal-star {
      font-size: 64rpx;
      color: #d4d4d4;
      transition: color 0.15s;
      
      &.filled {
        color: #f59e0b;
      }
    }
  }
  
  .modal-hint {
    font-size: 26rpx;
    color: #999999;
    margin-bottom: 24rpx;
  }
  
  .comment-input-section {
    width: 100%;
    margin-bottom: 32rpx;
    
    .comment-label {
      display: block;
      font-size: 26rpx;
      color: #666666;
      margin-bottom: 12rpx;
      text-align: left;
    }
    
    .comment-textarea {
      width: 100%;
      height: 160rpx;
      padding: 16rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      font-size: 26rpx;
      color: #333333;
      box-sizing: border-box;
    }
    
    .comment-count {
      display: block;
      text-align: right;
      font-size: 22rpx;
      color: #999999;
      margin-top: 8rpx;
    }
  }
  
  .modal-buttons {
    display: flex;
    width: 100%;
    gap: 24rpx;
    
    .modal-btn {
      flex: 1;
      text-align: center;
      padding: 20rpx 0;
      border-radius: 12rpx;
      font-size: 30rpx;
      
      &.cancel {
        background-color: #f5f7fa;
        color: #666666;
      }
      
      &.confirm {
        background-color: #1d4ed8;
        color: #ffffff;
      }
    }
  }
}
</style>