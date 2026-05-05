/**
 * 朋友圈英语 — 消息提示工具
 */

/**
 * 显示成功提示
 * @param {string} msg - 提示内容
 */
function showSuccess(msg) {
  wx.showToast({
    title: msg,
    icon: 'success',
    duration: 1500
  })
}

/**
 * 显示错误提示
 * @param {string} msg - 提示内容
 */
function showError(msg) {
  wx.showToast({
    title: msg,
    icon: 'error',
    duration: 2000
  })
}

/**
 * 显示加载中
 * @param {string} msg - 加载文字
 */
function showLoading(msg) {
  wx.showLoading({
    title: msg || '加载中...',
    mask: true
  })
}

/**
 * 隐藏加载
 */
function hideLoading() {
  wx.hideLoading()
}

/**
 * 显示模态对话框
 * @param {string} title
 * @param {string} content
 * @param {function} confirmCallback
 */
function showModal(title, content, confirmCallback) {
  wx.showModal({
    title,
    content,
    success(res) {
      if (res.confirm && confirmCallback) {
        confirmCallback()
      }
    }
  })
}

module.exports = { showSuccess, showError, showLoading, hideLoading, showModal }
