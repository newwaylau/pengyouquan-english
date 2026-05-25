所有的改动都在 `prefers-color-scheme: light` 下 `@media (max-width: 768px)` 块内：

## 改动清单

### 1. Label 文字 — 15% → 50%
```css
.field-group label {
  /* color: rgba(15,23,42,0.15); → rgba(15,23,42,0.5) */
}
```

### 2. 输入框边框 — #e2e8f0 → #cbd5e1（更深）
```css
.field-group input:not([type="submit"]):not([type="button"]) {
  border-color: #cbd5e1;  /* 原来 #e2e8f0 太淡 */
}
```

### 3. 占位文字 — 10% → 40%
```css
.field-group input::placeholder {
  color: rgba(15,23,42,0.4);  /* 原来 0.1 几乎看不见 */
}
```

### 4. 左侧图标 — 10% → 35%
```css
.login-input-icon {
  color: rgba(15,23,42,0.35);  /* 原来 0.1 */
}
.pwd-wrapper .eye-btn {
  color: rgba(15,23,42,0.35);  /* 原来 0.1 */
}
```
