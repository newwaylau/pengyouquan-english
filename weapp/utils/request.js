/**
 * 英语剧场 — HTTP 请求封装
 * 自动带 token 和 baseUrl，统一处理响应
 */
const app = getApp()

const BASE_URL = app.globalData.baseUrl

/**
 * 发起 HTTP 请求
 * @param {string} method - 请求方法 GET/POST/PUT/DELETE
 * @param {string} path - 请求路径（不含 baseUrl）
 * @param {object} data - 请求体（可选）
 * @param {boolean} noAuth - 是否不需要 token（默认 false）
 * @returns {Promise} - 返回 res.data（已解包 ApiResponse）
 */
function request(method, path, data, noAuth) {
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    const token = noAuth ? null : (wx.getStorageSync('token') || app.globalData.token)
    if (token) {
      header['Authorization'] = 'Bearer ' + token
    }

    wx.request({
      url: BASE_URL + path,
      method,
      data,
      header,
      success(res) {
        // 解析 ApiResponse 包裹
        const body = res.data
        if (body && body.code === 200) {
          resolve(body.data)
        } else if (body && body.code === 401) {
          // token 过期，清除并跳转登录
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          wx.redirectTo({ url: '/pages/login/login' })
          reject(new Error(body.message || '未登录'))
        } else {
          reject(new Error(body ? body.message : '请求失败'))
        }
      },
      fail(err) {
        reject(new Error('网络异常：' + (err.errMsg || '未知错误')))
      }
    })
  })
}

/**
 * GET 请求
 */
function get(path, data, noAuth) {
  return request('GET', path, data, noAuth)
}

/**
 * POST 请求
 */
function post(path, data, noAuth) {
  return request('POST', path, data, noAuth)
}

/**
 * PUT 请求
 */
function put(path, data, noAuth) {
  return request('PUT', path, data, noAuth)
}

/**
 * DELETE 请求
 */
function del(path, data, noAuth) {
  return request('DELETE', path, data, noAuth)
}

module.exports = { get, post, put, del, BASE_URL }
