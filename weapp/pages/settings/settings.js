/**
 * 朋友圈英语 — 设置页逻辑
 */
const { get, put } = require('../../utils/request')
const { showSuccess, showError } = require('../../utils/toast')
const { VOICES, SPEEDS } = require('../../utils/constants')

Page({
  data: {
    mode: 'sentry',
    voice: 'en-GB-RyanNeural',
    speed: 1.0,
    voices: VOICES,
    speeds: SPEEDS,
    loaded: false
  },

  onShow() {
    this.loadSettings()
  },

  /** 从后端加载设置 */
  loadSettings() {
    // 先读本地缓存
    const localMode = wx.getStorageSync('mode') || 'sentry'
    const localVoice = wx.getStorageSync('voice') || 'en-GB-RyanNeural'
    const localSpeed = wx.getStorageSync('speed') || 1.0

    this.setData({
      mode: localMode,
      voice: localVoice,
      speed: localSpeed
    })

    // 从后端同步
    get('/api/settings').then(data => {
      if (data) {
        const mode = data.mode || localMode
        const voice = data.voice || localVoice
        const speed = data.speed ? Number(data.speed) : localSpeed
        this.setData({ mode, voice, speed })
        // 同步到本地缓存
        wx.setStorageSync('mode', mode)
        wx.setStorageSync('voice', voice)
        wx.setStorageSync('speed', speed)
      }
    }).catch(() => {
      // 离线时使用本地缓存
    })
  },

  /** 设置练习模式 */
  setMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({ mode })
    wx.setStorageSync('mode', mode)
    this.saveToServer('mode', mode)
  },

  /** 设置音色 */
  setVoice(e) {
    const voice = e.currentTarget.dataset.voice
    this.setData({ voice })
    wx.setStorageSync('voice', voice)
    this.saveToServer('voice', voice)
  },

  /** 设置播放速度 */
  setSpeed(e) {
    const speed = e.currentTarget.dataset.speed
    this.setData({ speed })
    wx.setStorageSync('speed', speed)
    this.saveToServer('speed', String(speed))
  },

  /** 保存到后端 */
  saveToServer(key, value) {
    put('/api/settings', { [key]: value }).then(() => {
      showSuccess('已保存')
    }).catch(() => {
      // 静默失败，本地缓存起作用
    })
  }
})
