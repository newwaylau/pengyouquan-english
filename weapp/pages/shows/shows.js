/**
 * 英语剧场 — 剧集选择页逻辑
 */
const { get } = require('../../utils/request')
const { showError } = require('../../utils/toast')

Page({
  data: {
    shows: [],
    loading: true,
    showId: null,
    showName: ''
  },

  onLoad() {
    this.loadShows()
    const showId = wx.getStorageSync('showId') || null
    const showName = wx.getStorageSync('showName') || ''
    this.setData({ showId, showName })
  },

  onShow() {
    // 每次显示刷新选中状态
    const showId = wx.getStorageSync('showId') || null
    const showName = wx.getStorageSync('showName') || ''
    this.setData({ showId, showName })
  },

  /** 加载剧集列表 */
  loadShows() {
    this.setData({ loading: true })
    get('/api/shows').then(data => {
      this.setData({ shows: data || [], loading: false })
    }).catch(err => {
      this.setData({ loading: false })
      showError(err.message || '加载剧集失败')
    })
  },

  /** 选择剧集 */
  selectShow(e) {
    const id = e.currentTarget.dataset.id ? Number(e.currentTarget.dataset.id) : null
    const name = e.currentTarget.dataset.name || ''

    wx.setStorageSync('showId', id)
    wx.setStorageSync('showName', name)
    this.setData({ showId: id, showName: name })

    // 清除历史，切换剧集后重新出题
    wx.setStorageSync('historyIds', [])
    // 保存到服务端
    this.saveSetting('showId', String(id || ''))

    wx.showToast({ title: '已选择 ' + name, icon: 'success' })
  },

  /** 保存设置到服务端 */
  saveSetting(key, value) {
    try {
      const request = require('../../utils/request')
      request.put('/api/settings', { [key]: value })
        .catch(() => {})
    } catch (e) {
      // 静默失败
    }
  }
})
