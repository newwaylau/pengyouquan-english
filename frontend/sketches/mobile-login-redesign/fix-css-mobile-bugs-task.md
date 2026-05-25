# Fix Login Page CSS Bugs (Mobile + Light/Dark Mode)

## Changes needed

### File: `frontend/src/index.css`

#### Bug 1: "先逛逛" 按钮文字几乎不可见（深色模式）
**位置:** Line 6421-6426 (inside `@media (max-width: 768px)` block)
```css
.login-v2-skip {
    font-size: 0.65rem; color: rgba(255,255,255,0.06);  /* ← 6% 透明度，几乎看不见！ */
    border: 1px solid rgba(255,255,255,0.03); border-radius: 6px;
    padding: 5px 10px; background: transparent;
    font-family: inherit; cursor: pointer;
}
```
**修复:** `color: rgba(255,255,255,0.06)` → `color: rgba(255,255,255,0.5)`

#### Bug 2: "先逛逛" 按钮文字几乎不可见（白天模式）
**位置:** Lines 6589-6591 (inside `@media (prefers-color-scheme: light)` + `@media (max-width: 768px)`)
```css
.login-v2-skip {
    color: rgba(15,23,42,0.12);  /* ← 12% 透明度，几乎看不见！ */
    border-color: rgba(15,23,42,0.06);
}
```
**修复:** `color: rgba(15,23,42,0.12)` → `color: rgba(15,23,42,0.5)`

#### Bug 3: 语录文字白天模式看不清（白天模式）
**位置:** Lines 6597-6598
```css
.login-v2-quote-card blockquote {
    color: rgba(15,23,42,0.5);  /* ← 50% 透明度，白天背景上对比度不足 */
}
```
**修复:** `rgba(15,23,42,0.5)` → `rgba(15,23,42,0.75)`

Also fix the cite source:
**位置:** Lines 6600-6601
```css
.login-v2-quote-cite {
    color: rgba(15,23,42,0.15);  /* ← 15% 透明度，几乎看不见 */
}
```
**修复:** `rgba(15,23,42,0.15)` → `rgba(15,23,42,0.4)`

Also fix stats labels (the text "剧" / "剧集" / "台词" under numbers):
**位置:** Lines 6606-6607
```css
.login-v2-stats span {
    color: rgba(15,23,42,0.12);  /* ← 12% 透明度 */
}
```
**修复:** `rgba(15,23,42,0.12)` → `rgba(15,23,42,0.4)`

And forgot password link:
**位置:** Lines 6630-6631
```css
.forgot-link span {
    color: rgba(15,23,42,0.12);  /* ← 12% 透明度 */
}
```
**修复:** `rgba(15,23,42,0.12)` → `rgba(15,23,42,0.4)`

#### Bug 4: 密码输入框左边图标与文字重叠
**原因:** `.pwd-wrapper input` 的 `padding-left: 14px` 覆盖了 `.with-icon` 的 `padding-left: 36px`，导致文字起始位置仅14px，而图标占据12-28px（图标left:12px + 16px宽），文字从14px开始=重叠。

**位置:** `frontend/src/v2-missing.css` Line 343-345
```css
.pwd-wrapper input {
    width: 100%;
    padding: 10px 40px 10px 14px;  /* ← 左padding 14px 太窄，与图标重叠 */
    ...
}
```
**修复:** `padding: 10px 40px 10px 14px` → `padding: 10px 40px 10px 36px`

## 验证方法
1. `npm run build` 无报错
2. `docker cp` 部署到SIT
3. 手机端（<768px）测试深色/白天两个模式
   - "先逛逛" 按钮文字清晰可见
   - 语录文字在白天模式足够清晰
   - 密码输入框左边图标不重叠
