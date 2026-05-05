/**
 * 朋友圈英语 — 微信登录页逻辑
 */
const { post } = require('../../utils/request')
const { showError, showLoading, hideLoading } = require('../../utils/toast')

const app = getApp()

Page({
  data: {
    hasLoggedIn: false,
    nickname: '',
    avatar: ''
  },

  onLoad() {
    // 检查是否已登录
    const token = wx.getStorageSync('token')
    if (token) {
      const userInfo = wx.getStorageSync('userInfo') || {}
      this.setData({
        hasLoggedIn: true,
        nickname: userInfo.nickname || '用户',
        avatar: userInfo.avatar || ''
      })
    }
  },

  /** 微信一键登录 */
  handleWechatLogin() {
    showLoading('登录中...')
    wx.login({
      success: (res) => {
        if (res.code) {
          // 调后端微信登录接口
          post('/api/auth/wechat-login', { code: res.code }, true)
            .then((data) => {
              // data: { token, email, nickname, avatar, role }
              wx.setStorageSync('token', data.token)
              wx.setStorageSync('userInfo', {
                nickname: data.nickname || '',
                avatar: data.avatar || '',
                email: data.email || ''
              })
              app.globalData.token = data.token
              app.globalData.userInfo = {
                nickname: data.nickname || '',
                avatar: data.avatar || '',
                email: data.email || ''
              }
              hideLoading()
              this.setData({
                hasLoggedIn: true,
                nickname: data.nickname || '用户',
                avatar: data.avatar || ''
              })
              wx.showToast({ title: '登录成功', icon: 'success' })
              // 成功登录后跳转到首页
              wx.switchTab({ url: '/pages/index/index' })
            })
            .catch((err) => {
              hideLoading()
              showError(err.message || '登录失败')
            })
        } else {
          hideLoading()
          showError('获取微信 code 失败')
        }
      },
      fail: () => {
        hideLoading()
        showError('微信登录失败')
      }
    })
  },

  /** 跳转练习 */
  goToPractice() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
