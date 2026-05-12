/**
 * 英语剧场 — 首页练习逻辑
 */
const { get, post } = require('../../utils/request')
const { showError, showSuccess } = require('../../utils/toast')
const { VOICES, SPEEDS, MODES } = require('../../utils/constants')

const app = getApp()

/** 从字幕文本分离中英文 */
function extractEn(text) { return text.includes(' / ') ? text.split(' / ')[0].replace(/^#\d+\s+/, '') : text.replace(/^#\d+\s+/, '') }
function extractCn(text) { return text.includes(' / ') ? text.split(' / ')[1] : '' }

Page({
  data: {
    // 搜索
    searchQuery: '',
    searchResults: [],

    // 句子
    loading: true,
    currentSentence: null,
    currentChinese: '',
    showEnglish: false,
    showChinese: true,

    // 逐词输入
    words: [],
    wordInputs: [],
    hints: new Set(),
    correctWords: new Set(),
    wrongWords: new Set(),

    // 状态
    mode: 'sentry', // sentry | dictation
    answered: false,
    retryCount: 0,
    revealed: false,
    submitBtnText: '⏎ 提交答案',
    submitBtnClass: 'submit',
    submitDisabled: false,
    feedbackMessage: '',
    feedbackType: '',
    hasAudio: false,

    // 剧集
    showName: '',
    showId: null,

    // 统计
    todayCount: 0,

    // 播放
    voice: 'en-GB-RyanNeural',
    speed: 1.0,

    // 历史(排除已练)
    historyIds: [],

    // 搜索模式
    searchPracticeId: null
  },

  onLoad() {
    // 读取本地存储
    const historyIds = wx.getStorageSync('historyIds') || []
    const showId = wx.getStorageSync('showId') || null
    const showName = wx.getStorageSync('showName') || ''

    this.setData({
      historyIds,
      showId,
      showName,
      voice: wx.getStorageSync('voice') || 'en-GB-RyanNeural',
      speed: wx.getStorageSync('speed') || 1.0,
      mode: wx.getStorageSync('mode') || 'sentry'
    })
  },

  onShow() {
    // 每次显示时检查剧集是否变化
    const showId = wx.getStorageSync('showId') || null
    const showName = wx.getStorageSync('showName') || ''
    if (showId !== this.data.showId) {
      this.setData({ showId, showName, historyIds: [] })
      this.loadSentence()
    }
    // 加载当前句子
    if (!this.data.currentSentence && !this.data.loading) {
      this.loadSentence()
    }
    // 处理从错题本传过来的句子ID
    const practiceSentenceId = wx.getStorageSync('practiceSentenceId')
    if (practiceSentenceId) {
      wx.removeStorageSync('practiceSentenceId')
      this.setData({ searchPracticeId: practiceSentenceId })
      this.loadSentence()
    }
    // 加载统计
    this.loadStats()
  },

  /** 加载今日统计 */
  loadStats() {
    get('/api/users/me/stats').then(data => {
      if (data) {
        this.setData({ todayCount: data.totalPractices || 0 })
      }
    }).catch(() => {})
  },

  /** 加载随机句子 */
  loadSentence() {
    const { historyIds, showId, searchPracticeId } = this.data
    this.setData({
      loading: true,
      answered: false,
      retryCount: 0,
      revealed: false,
      feedbackMessage: '',
      searchPracticeId: null
    })

    // 如果有搜索练习ID，直接调单句API
    if (searchPracticeId) {
      get('/api/sentence/' + searchPracticeId).then(data => {
        if (data) { this.setupSentence(data) }
        else { this.setData({ loading: false }) }
      }).catch(() => { this.setData({ loading: false }) })
      return
    }

    const exclude = historyIds.slice(-200).join(',')
    let query = 'limit=15'
    if (exclude) query += '&exclude=' + encodeURIComponent(exclude)
    if (showId) query += '&showId=' + showId

    get('/api/random?' + query).then(data => {
      if (data && data.length > 0) {
        this.setupSentence(data[0])
      } else {
        this.setData({ loading: false })
        showError('没有更多句子了')
      }
    }).catch(err => {
      this.setData({ loading: false })
      showError(err.message || '加载失败')
    })
  },

  /** 设置句子 */
  setupSentence(sentence) {
    const en = extractEn(sentence.text)
    const cn = extractCn(sentence.text)
    const words = en.split(/\s+/).filter(Boolean)
    const mode = this.data.mode

    // 构建输入状态数组
    const wordInputs = words.map((w, i) => {
      const isHint = (w.includes("'") && w.length > 2) || (words.filter((_, j) => !(words[j].includes("'") && words[j].length > 2)).length > 0 && i === this.getRandomHintIndex(words, i))
      return {
        value: isHint ? w : '',
        status: '',
        focused: !isHint && i === 0,
        disabled: isHint,
        hint: isHint ? w : ''
      }
    })

    // 计算非提示词索引
    const hintIndexes = wordInputs.map((wi, idx) => wi.disabled ? idx : -1).filter(v => v >= 0)
    // 确保至少有一个提示词
    if (hintIndexes.length === 0 && words.length > 0) {
      const ri = Math.floor(Math.random() * words.length)
      wordInputs[ri] = { value: words[ri], status: '', focused: false, disabled: true, hint: words[ri] }
    }

    this.setData({
      loading: false,
      currentSentence: sentence,
      currentChinese: cn,
      showEnglish: false,
      showChinese: mode === 'sentry', // 中译英模式中文可见
      words,
      wordInputs,
      answered: false,
      retryCount: 0,
      revealed: false,
      correctWords: new Set(),
      wrongWords: new Set(),
      submitBtnText: '⏎ 提交答案',
      submitBtnClass: 'submit',
      submitDisabled: false,
      feedbackMessage: '',
      feedbackType: '',
      hasAudio: !!sentence.audioFile,
      showName: sentence.showName || this.data.showName
    })

    // 自动播放TTS
    setTimeout(() => {
      this.playTts()
    }, 500)
  },

  /** 获取随机提示词索引 */
  getRandomHintIndex(words, currentIndex) {
    const nonHintCandidates = words.map((w, i) => !(w.includes("'") && w.length > 2) ? i : -1).filter(v => v >= 0)
    if (nonHintCandidates.length === 0) return -1
    // 每次都随机选一个
    return nonHintCandidates[Math.floor(Math.random() * nonHintCandidates.length)]
  },

  /** 输入处理 */
  onWordInput(e) {
    const { index } = e.currentTarget.dataset
    const val = e.detail.value
    const key = e.detail.keyCode || ''

    const wordInputs = [...this.data.wordInputs]
    if (wordInputs[index].disabled) return

    wordInputs[index].value = val
    wordInputs[index].status = ''
    this.setData({ wordInputs })

    // 自动跳下一框
    const maxLen = (this.data.words[index] || '').length
    if (val.length >= maxLen && index < this.data.words.length - 1) {
      this.focusInput(index + 1)
    }
  },

  /** 输入确认（回车） */
  onWordConfirm(e) {
    const { index } = e.currentTarget.dataset
    if (index < this.data.words.length - 1) {
      this.focusInput(index + 1)
    }
  },

  /** 输入失焦 */
  onWordBlur(e) {
    const { index } = e.currentTarget.dataset
    // Backspace 回退上一框已在组件中处理
  },

  /** 聚焦输入框 */
  focusInput(index) {
    const wordInputs = [...this.data.wordInputs]
    wordInputs.forEach((wi, i) => { wi.focused = i === index })
    this.setData({ wordInputs })
  },

  /** 提交答案 */
  handleSubmit() {
    if (this.data.answered) {
      // 下一句
      this.goNext()
      return
    }
    if (this.data.retryCount >= 1 && this.data.retryCount < 2) {
      // 确认重试提交
    }

    const { words, wordInputs, retryCount } = this.data
    const correctSet = new Set()
    const wrongSet = new Set()

    words.forEach((w, i) => {
      if (wordInputs[i].disabled) {
        correctSet.add(i)
        return
      }
      const userVal = wordInputs[i].value.trim().toLowerCase()
      if (userVal === w.toLowerCase()) {
        correctSet.add(i)
      } else {
        wrongSet.add(i)
      }
    })

    // 更新输入状态
    const newInputs = [...wordInputs]
    wrongSet.forEach(i => { newInputs[i].status = 'wrong' })
    correctSet.forEach(i => { if (!newInputs[i].disabled) newInputs[i].status = 'correct' })

    const userWordCount = words.filter((_, i) => !wordInputs[i].disabled).length
    const userCorrectCount = words.filter((_, i) => !wordInputs[i].disabled && correctSet.has(i)).length

    if (wrongSet.size === 0) {
      // 全对
      newInputs.forEach(wi => { wi.focused = false })
      this.setData({
        wordInputs: newInputs,
        answered: true,
        correctWords: correctSet,
        wrongWords: wrongSet,
        showEnglish: true,
        showChinese: true,
        submitBtnText: '⏭️ 下一句',
        submitBtnClass: 'next',
        feedbackMessage: '✅ 完全正确！',
        feedbackType: 'correct'
      })
      // 提交记录
      this.logPractice(true, userCorrectCount, userWordCount)
    } else if (retryCount >= 1) {
      // 再次错误 — 揭示答案
      newInputs.forEach(wi => { wi.focused = false })
      newInputs.forEach((wi, i) => {
        if (wi.status === 'wrong' || wi.status === '') {
          wi.value = words[i]
          wi.status = 'revealed'
          wi.disabled = true
        }
      })
      this.setData({
        wordInputs: newInputs,
        answered: true,
        correctWords: correctSet,
        wrongWords: wrongSet,
        revealed: true,
        showEnglish: true,
        showChinese: true,
        submitBtnText: '⏭️ 下一句',
        submitBtnClass: 'next',
        feedbackMessage: `❌ 正确 ${userCorrectCount}/${userWordCount} 个词`,
        feedbackType: 'wrong'
      })
      // 提交记录（错误）
      this.logPractice(false, userCorrectCount, userWordCount)
    } else {
      // 首次错误 — 重试
      this.setData({
        wordInputs: newInputs,
        correctWords: correctSet,
        wrongWords: wrongSet,
        retryCount: 1,
        feedbackMessage: '⚠️ 有错误，再试一次',
        feedbackType: 'info'
      })
    }
  },

  /** 提交练习记录 */
  logPractice(correct, correctCount, totalWords) {
    const { currentSentence, mode } = this.data
    if (!currentSentence) return
    post('/api/practice/log', {
      sentenceId: currentSentence.id,
      correct,
      correctCount,
      totalWords,
      mode
    }).then(() => {
      // 更新历史
      const historyIds = [...this.data.historyIds, currentSentence.id].slice(-200)
      this.setData({ historyIds })
      wx.setStorageSync('historyIds', historyIds)
    }).catch(() => {})
  },

  /** 下一句 */
  goNext() {
    const { currentSentence, historyIds } = this.data
    const newHistory = [...historyIds, currentSentence.id].slice(-200)
    this.setData({ historyIds: newHistory })
    wx.setStorageSync('historyIds', newHistory)
    this.loadSentence()
  },

  /** 下一句（按钮） */
  nextSentence() {
    if (!this.data.currentSentence) return
    this.loadSentence()
  },

  /** 播放原音 */
  playOriginal() {
    const { currentSentence, hasAudio } = this.data
    if (!currentSentence) return
    if (hasAudio) {
      const url = app.globalData.baseUrl + '/api/audio/' + encodeURIComponent(currentSentence.audioFile)
      const audioCtx = wx.createInnerAudioContext()
      audioCtx.src = url
      audioCtx.playbackRate = this.data.speed
      audioCtx.play()
    } else {
      showError('暂无原音')
    }
  },

  /** 播放 TTS */
  playTts() {
    const { currentSentence, voice } = this.data
    const text = currentSentence ? extractEn(currentSentence.text) : ''
    if (!text) return
    const url = app.globalData.baseUrl + '/api/tts?text=' + encodeURIComponent(text) + '&voice=' + encodeURIComponent(voice)
    const audioCtx = wx.createInnerAudioContext()
    audioCtx.src = url
    audioCtx.playbackRate = this.data.speed
    audioCtx.play()
  },

  /** 切换英文显示 */
  toggleEnglish() {
    this.setData({ showEnglish: !this.data.showEnglish })
  },

  /** 搜索输入 */
  onSearchInput(e) {
    this.setData({ searchQuery: e.detail.value })
  },

  /** 搜索 */
  doSearch() {
    const q = this.data.searchQuery.trim()
    if (q.length < 2) {
      showError('请输入至少2个字符')
      return
    }
    get('/api/search?q=' + encodeURIComponent(q)).then(data => {
      this.setData({ searchResults: data || [] })
    }).catch(err => {
      showError(err.message)
    })
  },

  /** 从搜索结果开始练习 */
  startSearchPractice(e) {
    const id = e.currentTarget.dataset.id
    this.setData({
      searchPracticeId: id,
      searchResults: [],
      searchQuery: ''
    })
    this.loadSentence()
  }
})
