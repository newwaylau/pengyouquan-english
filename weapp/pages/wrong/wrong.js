/**
 * 英语剧场 — 错题本页逻辑
 */
const { get, del } = require('../../utils/request')
const { showSuccess, showError } = require('../../utils/toast')

Page({
  data: {
    wrongList: [],
    loading: true
  },

  onShow() {
    this.loadWrongSentences()
  },

  /** 加载错题列表 */
  loadWrongSentences() {
    this.setData({ loading: true })
    get('/api/wrong-sentences').then(data => {
      // data 格式：[{ id, text, showName, errorCount, ... }]
      this.setData({ wrongList: data || [], loading: false })
    }).catch(err => {
      this.setData({ loading: false })
      showError(err.message || '加载失败')
    })
  },

  /** 移除单条错题 */
  removeItem(e) {
    const id = e.currentTarget.dataset.id
    // 阻止冒泡，防止触发父容器跳转
    del('/api/wrong-sentences/' + id).then(() => {
      const list = this.data.wrongList.filter(item => item.id !== id)
      this.setData({ wrongList: list })
      showSuccess('已移除')
    }).catch(err => {
      showError(err.message || '移除失败')
    })
  },

  /** 练习错题 */
  practiceWrong(e) {
    const id = e.currentTarget.dataset.id
    // 跳转到首页并练习该句子
    wx.switchTab({ url: '/pages/index/index' })
    // 通过 storage 传递句子ID
    wx.setStorageSync('practiceSentenceId', id)
  },

  /** 清空全部 */
  clearAll() {
    wx.showModal({
      title: '确认清空',
      content: '确定清空所有错题吗？',
      success: (res) => {
        if (res.confirm) {
          del('/api/wrong-sentences').then(() => {
            this.setData({ wrongList: [] })
            showSuccess('已清空')
          }).catch(err => {
            showError(err.message || '清空失败')
          })
        }
      }
    })
  }
})
