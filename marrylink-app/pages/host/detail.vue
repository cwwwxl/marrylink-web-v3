<template>
  <view class="detail-container">
    <scroll-view class="detail-scroll" scroll-y>
      <!-- 主持人头部信息 -->
      <view class="host-header">
        <image class="host-avatar" :src="hostAvatarUrl" mode="aspectFill"></image>
        <view class="host-basic">
          <view class="name-row">
            <text class="host-name">{{ hostInfo.name }}</text>
            <view class="host-rating">
              <text class="rating-star">⭐</text>
              <text class="rating-score">{{ averageRating }}</text>
            </view>
          </view>
          <view class="host-tags">
            <text class="tag" v-for="tag in hostInfo.tags" :key="tag">{{ getTagNameByCode(tag) }}</text>
          </view>
          <view class="host-stats">
            <view class="stat-item">
              <text class="stat-value">{{ reviews.length || '0' }}</text>
              <text class="stat-label">场婚礼</text>
            </view>
            <view class="stat-item">
              <text class="stat-value">{{ reviews.length }}</text>
              <text class="stat-label">条评价</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 价格信息 -->
      <view class="price-section card">
        <view class="section-title">服务价格</view>
        <view class="price-info">
          <text class="price-label">起步价</text>
          <text class="price-value">¥{{ hostInfo.price || '3000' }}</text>
        </view>
        <text class="price-tip">*具体价格根据婚礼规模和需求而定</text>
      </view>
      
      <!-- 个人简介 -->
      <view class="intro-section card">
        <view class="section-title">个人简介</view>
        <text class="intro-text">{{ hostInfo.introduction || '专业婚礼主持人，拥有丰富的婚礼主持经验，擅长各种风格的婚礼主持，为您打造完美难忘的婚礼。' }}</text>
      </view>
      
      <!-- 服务特色 -->
      <view class="feature-section card">
        <view class="section-title">服务特色</view>
        <view class="feature-list">
          <view class="feature-item" v-for="(feature, index) in features" :key="index">
            <text class="feature-icon">{{ feature.icon }}</text>
            <view class="feature-content">
              <text class="feature-title">{{ feature.title }}</text>
              <text class="feature-desc">{{ feature.desc }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 案例展示 - 已屏蔽 -->
      <!-- <view class="case-section card">
        <view class="section-title">案例展示</view>
        <view class="case-grid">
          <image
            class="case-image"
            v-for="(image, index) in caseImageUrls"
            :key="index"
            :src="image"
            mode="aspectFill"
            @click="previewImage(index)"
          ></image>
        </view>
      </view> -->
      
      <!-- 用户评价 -->
      <view class="review-section card">
        <view class="section-title">用户评价 ({{ reviews.length }})</view>
        <view class="review-list" v-if="reviews.length > 0">
          <view class="review-item" v-for="review in reviews" :key="review.id">
            <view class="review-header">
              <view class="reviewer-avatar-placeholder">{{ review.userName ? review.userName.charAt(0) : '匿' }}</view>
              <view class="reviewer-info">
                <text class="reviewer-name">{{ review.userName || '匿名用户' }}</text>
                <view class="review-rating">
                  <text class="star" v-for="i in 5" :key="i">{{ i <= review.rating ? '⭐' : '☆' }}</text>
                </view>
              </view>
              <text class="review-date">{{ formatDate(review.weddingDate) }}</text>
            </view>
            <text class="review-content">{{ review.comment || '用户暂未留下评价内容' }}</text>
          </view>
        </view>
        <view class="no-review" v-else>
          <text class="no-review-text">暂无评价</text>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部操作栏 -->
    <view class="bottom-bar" v-if="!isHost">
      <view class="contact-btn" @click="handleContact">
        <text class="btn-icon">💬</text>
        <text class="btn-text">咨询</text>
      </view>
      <button class="book-btn" @click="handleBook">立即支付</button>
    </view>
  </view>
</template>

<script>
import { mapState } from 'vuex'
import { getHostDetail, getTagList, getHostReviews } from '@/api/host'
import { BASE_URL } from '@/utils/request'

export default {
  data() {
    return {
      hostId: '',
      hostInfo: {},
      tagDict: [],  // 标签字典，用于存储完整的标签信息
      features: [
        {
          icon: '🎤',
          title: '专业主持',
          desc: '多年婚礼主持经验，流程把控精准'
        },
        {
          icon: '💡',
          title: '创意策划',
          desc: '根据新人需求定制个性化流程'
        },
        {
          icon: '🎭',
          title: '多种风格',
          desc: '温馨、浪漫、幽默等多种风格'
        },
        {
          icon: '⏰',
          title: '准时守信',
          desc: '严格遵守时间，保证婚礼顺利'
        }
      ],
      reviews: []  // 从订单中获取的真实评价
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
    hostAvatarUrl() {
      if (!this.hostInfo.avatar) {
        return '/static/default-avatar.png'
      }
      if (this.hostInfo.avatar.startsWith('http')) {
        return this.hostInfo.avatar
      }
      return BASE_URL + this.hostInfo.avatar
    },
    // 计算平均评分
    averageRating() {
      if (this.reviews.length === 0) {
        return '5.0'
      }
      const total = this.reviews.reduce((sum, review) => sum + (review.rating || 5), 0)
      return (total / this.reviews.length).toFixed(1)
    }
  },
  
  onLoad(options) {
    if (options.id) {
      this.hostId = options.id
      // 先加载标签字典，再加载主持人详情和评价
      this.loadTagList()
        .finally(() => {
          this.loadHostDetail()
          this.loadHostReviews()
        })
    }
  },
  
  methods: {
    // 加载标签列表
    async loadTagList() {
      try {
        const res = await getTagList("01")  // "01" 是主持人风格标签分类代码
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
    
    // 加载主持人详情
    async loadHostDetail() {
      try {
        const res = await getHostDetail(this.hostId)
        this.hostInfo = res.data || {}
      } catch (error) {
        console.error('加载主持人详情失败:', error)
      }
    },
    
    // 加载主持人评价（从订单中获取）
    async loadHostReviews() {
      try {
        const res = await getHostReviews(this.hostId)
        this.reviews = res.data || []
      } catch (error) {
        console.error('加载主持人评价失败:', error)
        this.reviews = []
      }
    },
    
    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    },
    
    // 咨询 - 跳转到实时沟通
    handleContact() {
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
        url: `/pages/chat/room?targetUserId=${this.hostId}&targetName=${encodeURIComponent(this.hostInfo.name || '主持人')}&targetAvatar=${encodeURIComponent(this.hostInfo.avatar || '')}`
      })
    },
    
    // 预约
    handleBook() {
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
        url: `/pages/schedule/calendar?hostId=${this.hostId}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 120rpx;
}

.detail-scroll {
  height: calc(100vh - 120rpx);
}

.host-header {
  background-color: #ffffff;
  padding: 32rpx;
  display: flex;
  margin-bottom: 20rpx;
  
  .host-avatar {
    width: 160rpx;
    height: 160rpx;
    border-radius: 16rpx;
    margin-right: 24rpx;
  }
  
  .host-basic {
    flex: 1;
    
    .name-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12rpx;
      
      .host-name {
        font-size: 36rpx;
        font-weight: bold;
        color: #333333;
      }
      
      .host-rating {
        display: flex;
        align-items: center;
        
        .rating-star {
          font-size: 28rpx;
          margin-right: 4rpx;
        }
        
        .rating-score {
          font-size: 28rpx;
          color: #f59e0b;
          font-weight: bold;
        }
      }
    }
    
    .host-tags {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 16rpx;
      
      .tag {
        font-size: 22rpx;
        color: #1d4ed8;
        background-color: rgba(29, 78, 216, 0.1);
        padding: 6rpx 16rpx;
        border-radius: 8rpx;
        margin-right: 12rpx;
        margin-bottom: 8rpx;
      }
    }
    
    .host-stats {
      display: flex;
      
      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-right: 40rpx;
        
        .stat-value {
          font-size: 32rpx;
          font-weight: bold;
          color: #1d4ed8;
          margin-bottom: 4rpx;
        }
        
        .stat-label {
          font-size: 22rpx;
          color: #999999;
        }
      }
    }
  }
}

.card {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 24rpx;
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

.price-section {
  .price-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;
    
    .price-label {
      font-size: 28rpx;
      color: #666666;
    }
    
    .price-value {
      font-size: 48rpx;
      color: #ef4444;
      font-weight: bold;
    }
  }
  
  .price-tip {
    font-size: 24rpx;
    color: #999999;
  }
}

.intro-section {
  .intro-text {
    font-size: 28rpx;
    color: #666666;
    line-height: 1.8;
  }
}

.feature-section {
  .feature-list {
    .feature-item {
      display: flex;
      margin-bottom: 24rpx;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .feature-icon {
        font-size: 48rpx;
        margin-right: 20rpx;
      }
      
      .feature-content {
        flex: 1;
        
        .feature-title {
          display: block;
          font-size: 28rpx;
          font-weight: bold;
          color: #333333;
          margin-bottom: 8rpx;
        }
        
        .feature-desc {
          font-size: 24rpx;
          color: #666666;
        }
      }
    }
  }
}

.case-section {
  .case-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16rpx;
    
    .case-image {
      width: 100%;
      height: 200rpx;
      border-radius: 8rpx;
    }
  }
}

.review-section {
  .review-list {
    .review-item {
      padding-bottom: 24rpx;
      margin-bottom: 24rpx;
      border-bottom: 1rpx solid #e5e7eb;
      
      &:last-child {
        margin-bottom: 0;
        border-bottom: none;
      }
      
      .review-header {
        display: flex;
        align-items: center;
        margin-bottom: 16rpx;
        
        .reviewer-avatar {
          width: 64rpx;
          height: 64rpx;
          border-radius: 50%;
          margin-right: 16rpx;
        }
        
        .reviewer-avatar-placeholder {
          width: 64rpx;
          height: 64rpx;
          border-radius: 50%;
          margin-right: 16rpx;
          background-color: #1d4ed8;
          color: #ffffff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 28rpx;
          font-weight: bold;
        }
        
        .reviewer-info {
          flex: 1;
          
          .reviewer-name {
            display: block;
            font-size: 28rpx;
            color: #333333;
            margin-bottom: 4rpx;
          }
          
          .review-rating {
            .star {
              font-size: 20rpx;
              margin-right: 2rpx;
            }
          }
        }
        
        .review-date {
          font-size: 22rpx;
          color: #999999;
        }
      }
      
      .review-content {
        font-size: 26rpx;
        color: #666666;
        line-height: 1.6;
      }
    }
  }
  
  .no-review {
    padding: 40rpx 0;
    text-align: center;
    
    .no-review-text {
      font-size: 28rpx;
      color: #999999;
    }
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  
  .contact-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-right: 24rpx;
    
    .btn-icon {
      font-size: 40rpx;
      margin-bottom: 4rpx;
    }
    
    .btn-text {
      font-size: 22rpx;
      color: #666666;
    }
  }
  
  .book-btn {
    flex: 1;
    height: 80rpx;
    background-color: #1d4ed8;
    color: #ffffff;
    border: none;
    border-radius: 40rpx;
    font-size: 32rpx;
    font-weight: bold;
    line-height: 80rpx;
  }
}
</style>