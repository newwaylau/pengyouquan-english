# 老版设置面板分析报告

> 分析目标：`dictation-app/frontend`（老版5000项目）
> 面板ID：`panel-settings`

---

## 1. 完整 HTML 结构

```html
<!-- Settings Panel -->
<div class="panel" id="panel-settings">
  <!-- ── 标题 + 关闭按钮 ── -->
  <div style="display:flex;justify-content:space-between;align-items:center">
    <h3>⚙️ 设置</h3>
    <button class="btn btn-sm btn-secondary" onclick="hideAllPanels()" style="font-size:0.75rem">✖ 关闭</button>
  </div>

  <!-- ── ① 练习模式 ── -->
  <div class="form-group">
    <label>练习模式</label>
    <div class="voice-pills">
      <label class="mode-pill">
        <input type="radio" name="mode" value="sentry" onchange="setMode('sentry')" checked>
        <span>📝 中译英听写模式</span>
      </label>
      <label class="mode-pill">
        <input type="radio" name="mode" value="dictation" onchange="setMode('dictation')">
        <span>🖊️ 纯听写模式</span>
      </label>
    </div>
  </div>

  <!-- ── ② 剧集选择（三级联动下拉） ── -->
  <div class="form-group">
    <label>剧集选择</label>
    <div style="display:flex;gap:6px;flex-wrap:wrap">
      <select id="show-selector" class="show-select" style="flex:1;min-width:120px"></select>
      <select id="season-selector" class="show-select" style="flex:1;min-width:80px"></select>
      <select id="episode-selector" class="show-select" style="flex:1;min-width:100px"></select>
    </div>
  </div>

  <!-- ── ③ 音色选择（分组 + 网格） ── -->
  <div class="form-group">
    <div class="voice-radio-group" id="voice-radio-group">
      <!-- ▸ 剧集原音（勾选框，永远勾选） -->
      <div class="voice-section-label">剧集原音</div>
      <div class="voice-pills">
        <label class="voice-pill" id="voice-toggle-original">
          <input type="checkbox" onchange="toggleOriginalVoice(this.checked)" id="cb-original" checked>
          <span>默认</span>
        </label>
      </div>
      <hr class="voice-radio-divider">

      <!-- ▸ 导播（6种TTS音色，单选，2列网格） -->
      <div class="voice-section-label">导播</div>
      <div class="voice-pills voice-pills-grid">
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-US-JennyNeural" onchange="setVoiceOption('en-US-JennyNeural')">
          <span>🇺🇸 Jenny 美式女声</span>
        </label>
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-US-GuyNeural" onchange="setVoiceOption('en-US-GuyNeural')">
          <span>🇺🇸 Guy 美式男声</span>
        </label>
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-GB-SoniaNeural" onchange="setVoiceOption('en-GB-SoniaNeural')">
          <span>🇬🇧 Sonia 英式女声</span>
        </label>
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-GB-RyanNeural" onchange="setVoiceOption('en-GB-RyanNeural')">
          <span>🇬🇧 Ryan 英式男声</span>
        </label>
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-AU-NatashaNeural" onchange="setVoiceOption('en-AU-NatashaNeural')">
          <span>🇦🇺 Natasha 澳式女声</span>
        </label>
        <label class="voice-pill">
          <input type="radio" name="voice" value="en-AU-WilliamNeural" onchange="setVoiceOption('en-AU-WilliamNeural')">
          <span>🇦🇺 William 澳式男声</span>
        </label>
      </div>
    </div>
  </div>

  <!-- ── ④ 自动播放 ── -->
  <div class="form-group">
    <label>自动播放 (建议选择导播)</label>
    <div class="voice-pills">
      <label class="voice-pill">
        <input type="radio" name="autoPlay" value="browser" onchange="setAutoPlayVoice('browser')">
        <span>🔊 剧集原音</span>
      </label>
      <label class="voice-pill">
        <input type="radio" name="autoPlay" value="server" onchange="setAutoPlayVoice('server')" checked>
        <span>🎙️ 导播</span>
      </label>
    </div>
  </div>

  <!-- ── ⑤ 播放速度 ── -->
  <div class="form-group">
    <label>播放速度</label>
    <div style="display:flex;gap:8px">
      <button class="btn btn-sm btn-secondary speed-btn" data-speed="0.5" onclick="setSpeed(0.5)">0.5x</button>
      <button class="btn btn-sm btn-secondary speed-btn" data-speed="0.75" onclick="setSpeed(0.75)">0.75x</button>
      <button class="btn btn-sm btn-secondary speed-btn speed-active" data-speed="1" onclick="setSpeed(1)">1x</button>
      <button class="btn btn-sm btn-secondary speed-btn" data-speed="1.5" onclick="setSpeed(1.5)">1.5x</button>
    </div>
  </div>

  <!-- ── ⑥ 快捷键说明（只读展示） ── -->
  <div class="form-group">
    <label>快捷键</label>
    <div style="font-size:0.8rem;color:var(--text-muted);line-height:2">
      <kbd>-</kbd> 剧集原音 &nbsp; <kbd>=</kbd> 选中的音色 / 默认 Ryan &nbsp; <kbd>\</kbd> 下一句<br>
      <kbd>[</kbd> 中文 &nbsp; <kbd>]</kbd> 答案<br>
      <kbd>Enter</kbd> 提交<br>
    </div>
  </div>
</div>
```

---

## 2. 每个元素的 CSS 类名

| HTML 元素 | CSS 类名 | 说明 |
|---|---|---|
| 设置面板容器 | `.panel` | 初始 `display:none`，打开后加 `.show` → `display:block` |
| 面板标题 | `.panel h3` | 字号 `1rem`，颜色 `var(--primary)` (#6c5ce7) |
| 关闭按钮 | `.btn .btn-sm .btn-secondary` | 行内样式 `font-size:0.75rem` |
| 表单组容器 | `.form-group` | 底部间距 `12px` |
| 表单标签 | `.form-group label` | `font-size:0.8rem`，颜色 `var(--text-muted)` |
| 模式选择按钮 | `.mode-pill` | 胶囊式按钮，内嵌隐藏 radio |
| 音色选择按钮 | `.voice-pill` | 胶囊式按钮，内嵌隐藏 radio/checkbox |
| 音色网格容器 | `.voice-pills-grid` | 2列网格 flex 布局 |
| 音色分组标签 | `.voice-section-label` | `font-size:0.78rem`，灰色文字 |
| 音色分隔线 | `.voice-radio-divider` | `1px solid var(--card-border)` |
| 下拉选 | `.show-select` | 剧集/季/集三级选择 |
| 速度按钮 | `.speed-btn` | `min-width:40px; text-align:center` |
| 速度选中态 | `.speed-active` | 紫色背景 `var(--primary)` + 白色文字 |

### 视觉细节（共用 CSS 变量）

| 变量名 | 值 | 用途 |
|---|---|---|
| `--bg` | `#14142a` | 整体背景 |
| `--card-bg` | `#1e1e38` | 面板背景色 |
| `--card-border` | `#2e2e50` | 面板边框色 |
| `--text` | `#e8e8f0` | 主文字色 |
| `--text-muted` | `#8888aa` | 辅助文字色 |
| `--primary` | `#6c5ce7` | 主题紫色（选中态/标题） |
| `--radius` | `16px` | 面板圆角 |
| `--radius-sm` | `10px` | 按钮/输入框圆角 |

---

## 3. 每个表单元素的交互逻辑

### ① 练习模式（`mode-pill` → `setMode()`）

- **类型**: 二选一 radio（`name="mode"`）
- **触发**: `onchange` → `setMode('sentry'|'dictation')`
- **逻辑**:
  ```js
  function setMode(mode) {
    state.mode = mode;
    LSS('mode', mode);  // 存入 localStorage da_mode
    // 更新 mode-badge 文字
    $('mode-badge').textContent = mode === 'dictation'
      ? '🖊️ 纯听写模式'
      : '📝 中译英听写模式';
    // 纯听写模式显示"显示中文"按钮，中译英模式隐藏
    const cnBtn = $('btn-toggle-cn');
    if (cnBtn) cnBtn.style.display = mode === 'dictation' ? '' : 'none';
    showToast(...);
    // 如果正在练习，重载下一句
    if (state.pageMode === 'practice') loadNextSentence();
  }
  ```
- **持久化**: localStorage `da_mode`，默认 `'sentry'`
- **启动时强制覆盖**: 页面加载时始终强制设为 `sentry` 并勾选对应 radio

### ② 剧集选择（三级联动下拉）

- **类型**: 三个关联的 `<select>`（动画填充 JS）
- **ID**: `show-selector` / `season-selector` / `episode-selector`
- **逻辑**:
  - `loadShowList()` 从 `/api/shows` 获取所有剧集，解析为 `showGroups` 层级结构
  - `populateShowSelector()` → 填充剧目下拉，恢复 `state.selectedShow`
  - 切换剧目 → `populateSeasonSelector(title)` → 填充季下拉
  - 切换季 → `populateEpisodeSelector(title, season, epId)` → 填充集下拉
  - 切换集 → `LS('episodeId', id)` 保存 + 更新 `state.selectedEpisodeId`
  - 集变更 → `loadNextSentence()` 重载练习
- **持久化**: `da_selectedShow`、`da_selectedSeason`、`da_episodeId`

### ③ 音色选择（voice-pill → setVoiceOption()）

- **类型**: 一个 checkbox（剧集原音，永远勾选）+ 六个 radio（导播音色）
- **checkbox 逻辑**:
  ```js
  function toggleOriginalVoice(checked) {
    const cb = $('cb-original');
    if (!checked) cb.checked = true;  // 永远不允许取消勾选
    showToast('🎬 剧集原音');
  }
  ```
- **radio 逻辑**:
  ```js
  function setVoiceOption(val) {
    state.serverVoice = val;
    LSS('selectedVoice', val);
    // 更新 TTS 重播按钮文字
    const btn = document.getElementById('btn-tts-replay');
    if (btn) btn.textContent = '🎙️ ' + shortName;
    showToast('🎙️ ' + VOICE_NAMES[val]);
  }
  ```
- **持久化**: localStorage `da_selectedVoice`，默认 `'en-GB-RyanNeural'`
- **启动恢复**: 页面加载时读取 localStorage，勾选对应 radio，同步 TTS 按钮文字

### ④ 自动播放（voice-pill → setAutoPlayVoice()）

- **类型**: 二选一 radio（`name="autoPlay"`）
- **逻辑**:
  ```js
  function setAutoPlayVoice(v) {
    state.autoPlayVoice = v;
    LSS('autoPlayVoice', v);
    showToast(v === 'server' ? '🎙️ 导播自动播放' : '🔊 剧集原音自动播放');
  }
  ```
- **持久化**: localStorage `da_autoPlayVoice`，默认 `'server'`
- **使用**: 在句子加载/重播时判断：
  ```js
  if (state.autoPlayVoice === 'server' && clean) {
    playAudio(`${API_BASE}/api/tts?text=...&voice=${state.serverVoice}`);
  } else {
    playAudio(`${API_BASE}/api/tts?text=...&voice=${state.serverVoice}`);
  }
  ```
  > 注意：当前代码中两种路径实际上调用的是同一个逻辑。

### ⑤ 播放速度（speed-btn → setSpeed()）

- **类型**: 四个按钮（0.5x / 0.75x / 1x / 1.5x）
- **触发**: `onclick="setSpeed(0.5)"` 等
- **逻辑**:
  ```js
  function setSpeed(s) {
    state.playbackSpeed = parseFloat(s);
    LSS('speed', state.playbackSpeed);
    // 切换速度按钮高亮
    document.querySelectorAll('.speed-btn')
      .forEach(b => b.classList.toggle('speed-active', b.dataset.speed == s));
  }
  ```
- **持久化**: localStorage `da_speed`，默认 `0.75`
- **使用**: 每次 `playAudio()` 时设置 `a.playbackRate = state.playbackSpeed`
- **视觉**: `speed-active` 类名赋予紫色背景 + 白色文字，其余为普通 secondary 按钮

### ⑥ 快捷键（只读展示区）

- 无交互，纯展示 `<kbd>` 标签
- 字号 `0.8rem`，颜色 `var(--text-muted)`，行高 `2`

---

## 4. 面板打开/关闭机制

```js
// 关闭所有面板
function hideAllPanels() {
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('show'));
}

// 切换单个面板（toggle）
function togglePanel(id) {
  const p = $(id);                      // 用 ID 获取面板 DOM
  const o = p.classList.contains('show'); // 检查是否已打开
  hideAllPanels();                        // 先全部关闭
  if (!o) p.classList.add('show');        // 如果之前关闭则打开
}

// 暴露为全局函数
window.togglePanel = togglePanel;
```

- 点击"⚙️ 设置"触发 `togglePanel('panel-settings')`
- 点击"✖ 关闭"触发 `hideAllPanels()`
- 搜索按钮、答题跳转等也会调用 `hideAllPanels()` 确保面板关闭

---

## 5. 颜色、间距、字体大小汇总

| 元素 | 字体大小 | 间距 | 颜色 |
|---|---|---|---|
| 面板 `.panel` | — | padding: `20px`, margin-bottom: `16px` | `--card-bg` (#1e1e38), border `--card-border` |
| 面板标题 `h3` | `1rem` | margin-bottom: `12px` | `--primary` (#6c5ce7) |
| 标签 `label` | `0.8rem` | margin-bottom: `4px` | `--text-muted` (#8888aa) |
| 模式/音色胶囊（.mode-pill / .voice-pill） | `0.82rem` / `0.8rem` | padding: `6px 12px` / `6px 10px`, gap: `5px` | border `--card-border`, bg `rgba(255,255,255,0.05)` |
| 胶囊选中态 | 同 | border-color → `--primary`, bg → `rgba(108,92,231,0.12)` |
| 胶囊 hover 态 | 同 | border-color → `--text-muted`, bg → `rgba(255,255,255,0.06)` |
| 音频选择 `.show-select` | `0.8rem` | padding: `4px 8px`, border-radius: `8px` | bg `rgba(255,255,255,0.08)`, border `--card-border` |
| 速度按钮 `.speed-btn` | — | min-width: `40px` | 默认 secondary button 样式 |
| 速度按钮 `.speed-active` | — | — | bg `--primary` + white text |
| 快捷键区域 | `0.8rem` | line-height: `2` | `--text-muted` |
| 分隔线 `.voice-radio-divider` | — | margin: `4px 0 2px` | `--card-border` |
| 分组标签 `.voice-section-label` | `0.78rem` | padding: `2px` | `--text-muted` |

---

## 6. 交互行为总结图

```
用户操作               → JS函数              → 状态变更                  → 持久化 → 副作用
──────────────────────────────────────────────────────────────────────────────────────────
点击练习模式            setMode(mode)          state.mode                 da_mode    更新徽章/按钮/TTS重载
切换剧集               populateShowSelector   state.selectedShow         da_selectedShow
切换季/集              populateSeason/Episode state.selectedSeason/EpId  da_*        重载句子
点击音色               setVoiceOption(val)     state.serverVoice         da_selectedVoice  更新 TTS 按钮
勾选"剧集原音"          toggleOriginalVoice    (无状态变更,强制勾选)      —           Toast
选择自动播放源          setAutoPlayVoice(v)    state.autoPlayVoice        da_autoPlayVoice  Toast
点击速度按钮            setSpeed(s)            state.playbackSpeed        da_speed    更新按钮高亮
快捷键(全局键盘事件)     —                      —                         —           playAudio(...)
```

---

## 7. 关键设计特征

1. **纯 JS 管理开关** — 面板显示不依赖 CSS 动画，只有 `.show` 类的 `display:block/none` 切换
2. **全部持久化** — 6个设置项全部写入 localStorage（前缀 `da_`），页面刷新后恢复
3. **胶囊式单选** — 所有选择类控件（模式/音色/自动播放）都使用隐藏 input + label 样式化的胶囊按钮
4. **强制唯一值** — `hideAllPanels()` 在每次 toggle 前执行，保证同时只有一个面板打开
5. **剧集原音 checkbox 不可取消** — `toggleOriginalVoice()` 中如果 `checked=false` 立即重新勾选，保证永远选中
6. **启动覆盖** — 练习模式在页面加载时强制设为 `sentry` 并覆盖 localStorage，无论之前是什么值
7. **网格布局** — 6种音色使用 `voice-pills-grid` 2列网格，在大屏和移动端自适应
8. **无外部依赖** — 面板交互全部用原生 JS + onclick/inline onchange，无框架无组件

---

*报告生成时间: 2026-05-05*
*分析版本: dictation-app V2 (老版 5000)*
