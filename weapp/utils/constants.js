/**
 * 朋友圈英语 — 常量定义
 */

/** 可用音色列表 */
const VOICES = [
  { id: 'en-GB-RyanNeural', name: 'Ryan (英式男声)' },
  { id: 'en-GB-SoniaNeural', name: 'Sonia (英式女声)' },
  { id: 'en-US-JennyNeural', name: 'Jenny (美式女声)' },
  { id: 'en-US-GuyNeural', name: 'Guy (美式男声)' },
  { id: 'en-AU-NatashaNeural', name: 'Natasha (澳式女声)' },
  { id: 'en-AU-WilliamNeural', name: 'William (澳式男声)' }
]

/** 播放速度选项 */
const SPEEDS = [0.5, 0.75, 1.0, 1.5]

/** 练习模式 */
const MODES = [
  { id: 'sentry', name: '中译英' },
  { id: 'dictation', name: '听写' }
]

module.exports = { VOICES, SPEEDS, MODES }
