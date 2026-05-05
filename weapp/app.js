/**
 * 朋友圈英语 — 微信小程序
 * 全局 App 实例
 */
App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8080',
    showId: null,
    showName: ''
  },

  onLaunch() {
    // 读取本地存储的 token
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }
  }
})
