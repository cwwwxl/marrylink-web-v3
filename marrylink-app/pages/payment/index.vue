<template>
  <view class="payment-container">
    <!-- 支付成功页面 -->
    <view v-if="paySuccess" class="success-page">
      <view class="success-icon-wrap">
        <text class="success-icon">&#10003;</text>
      </view>
      <text class="success-title">支付成功</text>
      <text class="success-amount">¥{{ amount }}</text>
      <text class="success-desc">订单已创建，主持人将尽快与您确认</text>
      <view class="success-btns">
        <view class="success-btn primary" @click="viewOrder">查看订单</view>
        <view class="success-btn default" @click="goHome">返回首页</view>
      </view>
    </view>

    <!-- 支付页面 -->
    <view v-else>
      <!-- 金额展示 -->
      <view class="amount-header">
        <text class="amount-label">支付金额</text>
        <view class="amount-row">
          <text class="currency">¥</text>
          <text class="amount-num">{{ amount }}</text>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="order-info">
        <view class="info-item">
          <text class="info-label">主持人</text>
          <text class="info-value">{{ hostName }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">婚礼日期</text>
          <text class="info-value">{{ weddingDate }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">婚礼类型</text>
          <text class="info-value">{{ weddingTypeName }}</text>
        </view>
      </view>

      <!-- 支付方式选择 -->
      <view class="pay-methods">
        <view class="method-title">选择支付方式</view>
        <view
          class="method-item"
          :class="{ active: payMethod === 'wechat' }"
          @click="payMethod = 'wechat'"
        >
          <view class="method-left">
            <view class="wechat-icon">
              <text class="wechat-icon-text">微</text>
            </view>
            <text class="method-name">微信支付</text>
          </view>
          <view class="radio-circle" :class="{ checked: payMethod === 'wechat' }">
            <text v-if="payMethod === 'wechat'" class="radio-dot">&#10003;</text>
          </view>
        </view>
      </view>

      <!-- 支付按钮 -->
      <view class="pay-bottom">
        <button
          class="pay-btn"
          :class="{ disabled: paying }"
          :disabled="paying"
          @click="handlePay"
        >
          {{ paying ? '支付中...' : '确认支付 ¥' + amount }}
        </button>
        <text class="pay-tip">点击确认支付即表示同意服务协议</text>
      </view>
    </view>

    <!-- 模拟微信支付弹窗 -->
    <view v-if="showWechatModal" class="wechat-overlay">
      <view class="wechat-modal">
        <view class="wechat-header">
          <text class="wechat-close" @click="cancelWechatPay">&#10005;</text>
          <text class="wechat-title">微信支付</text>
          <text class="wechat-placeholder"></text>
        </view>
        <view class="wechat-body">
          <view class="wechat-merchant">
            <text class="merchant-label">收款方</text>
            <text class="merchant-name">MarryLink婚礼服务</text>
          </view>
          <view class="wechat-amount-section">
            <text class="wechat-currency">¥</text>
            <text class="wechat-amount">{{ amount }}</text>
          </view>
          <view class="wechat-type">
            <text class="type-text">付款方式：微信零钱</text>
          </view>
        </view>
        <view class="wechat-footer">
          <button class="wechat-pay-btn" @click="confirmWechatPay">
            确认支付
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { createPaidOrder } from '@/api/order'

export default {
  data() {
    return {
      hostId: '',
      hostName: '',
      weddingDate: '',
      weddingType: '',
      amount: '3000',
      payMethod: 'wechat',
      paying: false,
      showWechatModal: false,
      paySuccess: false,
      weddingTypes: {
        '01': '中式婚礼',
        '02': '西式婚礼',
        '03': '主题婚礼',
        '04': '户外婚礼'
      }
    }
  },

  computed: {
    weddingTypeName() {
      return this.weddingTypes[this.weddingType] || this.weddingType
    }
  },

  onLoad(options) {
    this.hostId = options.hostId || ''
    this.hostName = decodeURIComponent(options.hostName || '主持人')
    this.weddingDate = options.weddingDate || ''
    this.weddingType = options.weddingType || '01'
    this.amount = options.amount || '3000'
  },

  methods: {
    handlePay() {
      if (this.paying) return
      this.showWechatModal = true
    },

    cancelWechatPay() {
      this.showWechatModal = false
    },

    async confirmWechatPay() {
      this.showWechatModal = false
      this.paying = true

      try {
        const res = await createPaidOrder({
          hostId: this.hostId,
          weddingDate: this.weddingDate,
          weddingType: this.weddingType
        })

        if (res.code === 200 || res.code === '00000') {
          this.paySuccess = true
        }
      } catch (error) {
        uni.showToast({
          title: error.message || '支付失败，请重试',
          icon: 'none'
        })
      } finally {
        this.paying = false
      }
    },

    viewOrder() {
      uni.redirectTo({
        url: '/pages/order/index?status=3'
      })
    },

    goHome() {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.payment-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 金额展示 */
.amount-header {
  background-color: #ffffff;
  padding: 48rpx 32rpx;
  text-align: center;
  margin-bottom: 20rpx;

  .amount-label {
    font-size: 28rpx;
    color: #666666;
    display: block;
    margin-bottom: 16rpx;
  }

  .amount-row {
    display: flex;
    align-items: baseline;
    justify-content: center;
  }

  .currency {
    font-size: 40rpx;
    color: #333333;
    font-weight: bold;
    margin-right: 4rpx;
  }

  .amount-num {
    font-size: 72rpx;
    color: #333333;
    font-weight: bold;
  }
}

/* 订单信息 */
.order-info {
  background-color: #ffffff;
  padding: 24rpx 32rpx;
  margin-bottom: 20rpx;

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f5f7fa;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 28rpx;
      color: #666666;
    }

    .info-value {
      font-size: 28rpx;
      color: #333333;
    }
  }
}

/* 支付方式 */
.pay-methods {
  background-color: #ffffff;
  padding: 24rpx 32rpx;
  margin-bottom: 20rpx;

  .method-title {
    font-size: 28rpx;
    color: #666666;
    margin-bottom: 16rpx;
  }

  .method-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    border-radius: 12rpx;

    &.active {
      background-color: transparent;
    }

    .method-left {
      display: flex;
      align-items: center;
    }

    .wechat-icon {
      width: 60rpx;
      height: 60rpx;
      background-color: #07c160;
      border-radius: 12rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20rpx;

      .wechat-icon-text {
        color: #ffffff;
        font-size: 28rpx;
        font-weight: bold;
      }
    }

    .method-name {
      font-size: 30rpx;
      color: #333333;
    }

    .radio-circle {
      width: 44rpx;
      height: 44rpx;
      border-radius: 50%;
      border: 3rpx solid #d4d4d4;
      display: flex;
      align-items: center;
      justify-content: center;

      &.checked {
        background-color: #07c160;
        border-color: #07c160;
      }

      .radio-dot {
        color: #ffffff;
        font-size: 24rpx;
        font-weight: bold;
      }
    }
  }
}

/* 底部支付按钮 */
.pay-bottom {
  padding: 40rpx 32rpx;
  text-align: center;

  .pay-btn {
    width: 100%;
    height: 96rpx;
    background-color: #07c160;
    color: #ffffff;
    border: none;
    border-radius: 48rpx;
    font-size: 34rpx;
    font-weight: bold;
    line-height: 96rpx;

    &.disabled {
      opacity: 0.6;
    }

    &::after {
      border: none;
    }
  }

  .pay-tip {
    display: block;
    font-size: 22rpx;
    color: #999999;
    margin-top: 20rpx;
  }
}

/* 模拟微信支付弹窗 */
.wechat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.wechat-modal {
  width: 100%;
  background-color: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  overflow: hidden;
}

.wechat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .wechat-close {
    font-size: 36rpx;
    color: #999999;
    width: 60rpx;
    text-align: left;
  }

  .wechat-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #333333;
  }

  .wechat-placeholder {
    width: 60rpx;
  }
}

.wechat-body {
  padding: 48rpx 32rpx;
  text-align: center;

  .wechat-merchant {
    margin-bottom: 40rpx;

    .merchant-label {
      font-size: 24rpx;
      color: #999999;
      display: block;
      margin-bottom: 8rpx;
    }

    .merchant-name {
      font-size: 30rpx;
      color: #333333;
    }
  }

  .wechat-amount-section {
    display: flex;
    align-items: baseline;
    justify-content: center;
    margin-bottom: 32rpx;

    .wechat-currency {
      font-size: 40rpx;
      color: #333333;
      font-weight: bold;
      margin-right: 4rpx;
    }

    .wechat-amount {
      font-size: 80rpx;
      color: #333333;
      font-weight: bold;
    }
  }

  .wechat-type {
    .type-text {
      font-size: 26rpx;
      color: #999999;
    }
  }
}

.wechat-footer {
  padding: 32rpx;

  .wechat-pay-btn {
    width: 100%;
    height: 96rpx;
    background-color: #07c160;
    color: #ffffff;
    border: none;
    border-radius: 48rpx;
    font-size: 34rpx;
    font-weight: bold;
    line-height: 96rpx;

    &::after {
      border: none;
    }
  }
}

/* 支付成功页面 */
.success-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 48rpx;

  .success-icon-wrap {
    width: 120rpx;
    height: 120rpx;
    background-color: #07c160;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 32rpx;

    .success-icon {
      color: #ffffff;
      font-size: 60rpx;
      font-weight: bold;
    }
  }

  .success-title {
    font-size: 40rpx;
    font-weight: bold;
    color: #333333;
    margin-bottom: 16rpx;
  }

  .success-amount {
    font-size: 48rpx;
    font-weight: bold;
    color: #333333;
    margin-bottom: 24rpx;
  }

  .success-desc {
    font-size: 28rpx;
    color: #999999;
    margin-bottom: 64rpx;
  }

  .success-btns {
    width: 100%;

    .success-btn {
      width: 100%;
      height: 88rpx;
      border-radius: 44rpx;
      font-size: 30rpx;
      font-weight: bold;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 24rpx;

      &.primary {
        background-color: #1d4ed8;
        color: #ffffff;
      }

      &.default {
        background-color: #f5f7fa;
        color: #666666;
      }
    }
  }
}
</style>
