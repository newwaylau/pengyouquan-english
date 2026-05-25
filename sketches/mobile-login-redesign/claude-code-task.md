# 手机端登录页改造任务

## 目标
把手机端的登录页（LoginPage.tsx + 相关 CSS）改造成更简洁、精致的设计，只保留语录 + 统计数字 + 登录/注册表单。

## 设计参考
文件：`sketches/mobile-login-redesign/variants/final-simple.html`
用浏览器打开这个 HTML 看最终效果。

## 具体改动

### 1. LoginPage.tsx — JSX 结构调整
当前文件：`frontend/src/LoginPage.tsx`

把 return 部分的 JSX 改成以下结构（从上到下）：

```
<div className="login-page-new login-v2-page">
  <!-- brand bar: 只保留一个"剧"logo -->
  <div className="login-v2-topbar">
    <div className="login-v2-brand-mark">剧</div>
    <span className="login-v2-brand-name">英语剧场</span>
    <div style="flex:1"></div>
    <button className="login-v2-skip" onClick={onHome}>先逛逛 →</button>
  </div>

  <!-- quote card -->
  <div className="login-v2-quote-card">
    <blockquote>"The night is dark and full of terrors."</blockquote>
    <div className="login-v2-quote-cite">Melisandre · Game of Thrones</div>
  </div>

  <!-- stats: from backend API -->
  <div className="login-v2-stats">
    <div><strong>2</strong><span>剧</span></div>
    <div><strong>17</strong><span>剧集</span></div>
    <div><strong>10K+</strong><span>台词</span></div>
  </div>

  <!-- email input -->
  <div className="field-group">
    <label>邮箱 / 手机号</label>
    <div className="login-input-icon-wrap">
      <span className="login-input-icon">[mail icon SVG]</span>
      <input className="login-form-input with-icon" ... />
    </div>
  </div>

  <!-- password input -->
  <div className="field-group">
    <label>密码</label>
    <div className="pwd-wrapper login-input-icon-wrap">
      <span className="login-input-icon">[lock icon SVG]</span>
      <input className="with-icon" ... />
      <span className="eye-btn">[eye SVG]</span>
    </div>
  </div>

  <!-- forgot password -->
  <div className="forgot-link">
    <span onClick={goToForgot}>忘记密码？</span>
  </div>

  <!-- login + register buttons side by side -->
  <div className="login-v2-actions">
    <button type="submit" className="btn-primary login-v2-btn-primary">登录</button>
    <button type="button" className="btn-outline login-v2-btn-secondary" onClick={() => setMode('register')}>注册</button>
  </div>
</div>
```

**要求：**
- 删除所有 hero 标题、副标题、brand 区域里的大段文字
- 删除登录后 welcome 面板不在此次改动范围内，保持不动
- 统计数字先用固定值（2/17/10K+），后续加 API
- 保留所有原有逻辑代码（handleSubmit, handleSendCode 等）
- **"先逛逛"按钮只保留一个**（右上角的），删除原来表单上方的那个
- 保留 mode 切换（login/register/forgot），但要通过点击"注册"按钮（不是 tab）切换 mode
- **不要 tab 栏**，用底部"注册"按钮切换模式
- 忘记密码链接只在表单内出现一次（不要 tab 的忘记密码）

### 2. 修复图标重叠问题
文件：`frontend/src/v2-missing.css`

`.with-icon { padding-left: 36px; }` 在第 303 行
`.login-form-input { padding: 10px 14px; }` 在第 319 行

因为 `login-form-input` 在 v2-missing.css 里定义在 `with-icon` **之后**，它的 `padding: 10px 14px` 覆盖了 `with-icon` 的 `padding-left: 36px`。

**修复方法：** 把 `.with-icon` 的规则改为 `padding: 10px 14px 10px 36px;` 这样就不会被覆盖了。或者把 `.with-icon` 移到 `.login-form-input` 后面。

### 3. CSS 样式更新
文件：`frontend/src/index.css`

在手机端媒体查询 `@media (max-width: 768px)` 里（约第 6367 行），添加新的登录页手机端样式：

```css
@media (max-width: 768px) {
  /* 登录页手机端重写 */
  .login-v2-page {
    flex-direction: column;
    min-height: 0;
    padding: 0 20px 24px;
    background: #0a0e1a;
    background-image: 
      radial-gradient(ellipse 160% 50% at 50% -40%, rgba(20,184,166,0.07) 0%, transparent 70%),
      radial-gradient(ellipse 100% 40% at 70% 130%, rgba(20,184,166,0.03) 0%, transparent 60%),
      linear-gradient(180deg, #070b18 0%, #0d1225 100%);
  }
  
  .login-v2-art {
    display: none; /* 隐藏旧的品牌区，新设计不用 */
  }
  
  .login-v2-panel {
    width: 100%;
    min-width: 0;
    padding: 0;
    border-left: none;
    background: transparent;
    border: none;
    box-shadow: none;
  }
  
  .login-v2-inner {
    max-width: 100%;
  }
  
  .login-v2-form-head {
    display: none; /* 隐藏旧表单头部 */
  }
  
  /* 新样式 */
  .login-v2-topbar {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 16px 0 36px;
  }
  
  .login-v2-brand-mark {
    width: 32px; height: 32px;
    border-radius: 8px;
    background: #14b8a6;
    display: flex; align-items: center; justify-content: center;
    font-size: 0.8rem; font-weight: 700; color: #0a0e1a;
  }
  
  .login-v2-brand-name {
    font-size: 0.85rem; font-weight: 600; color: rgba(255,255,255,0.7);
  }
  
  .login-v2-skip {
    font-size: 0.65rem; color: rgba(255,255,255,0.06);
    border: 1px solid rgba(255,255,255,0.03); border-radius: 6px;
    padding: 5px 10px; background: transparent;
    font-family: inherit; cursor: pointer;
  }
  
  .login-v2-quote-card {
    background: rgba(255,255,255,0.015);
    border: 1px solid rgba(255,255,255,0.04);
    border-radius: 14px;
    padding: 20px;
    margin-bottom: 24px;
    position: relative;
  }
  
  .login-v2-quote-card::before {
    content: '';
    position: absolute;
    top: 0; left: 20px; right: 20px;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(20,184,166,0.2), transparent);
  }
  
  .login-v2-quote-card blockquote {
    font-family: 'Playfair Display', Georgia, serif;
    font-size: 1.1rem; font-style: italic;
    color: rgba(255,255,255,0.55);
    line-height: 1.6; margin-bottom: 8px;
  }
  
  .login-v2-quote-cite {
    font-size: 0.65rem; color: rgba(255,255,255,0.15);
  }
  
  .login-v2-quote-cite em {
    font-style: normal; color: #14b8a6;
  }
  
  .login-v2-stats {
    display: flex;
    justify-content: center;
    gap: 32px;
    margin-bottom: 36px;
  }
  
  .login-v2-stats div {
    text-align: center;
  }
  
  .login-v2-stats strong {
    display: block;
    font-size: 1.1rem;
    font-weight: 700;
    color: rgba(255,255,255,0.85);
  }
  
  .login-v2-stats span {
    font-size: 0.6rem;
    color: rgba(255,255,255,0.12);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-top: 3px;
    display: block;
  }
}
```

也添加桌面端对应的样式更新（login-v2-topbar, login-v2-brand-mark, login-v2-brand-name, login-v2-skip, login-v2-quote-cite, login-v2-stats 等），放在 `@media (min-width: 769px)` 或 Claude Design overrides 里。

### 4. v2-missing.css 里的 login-v2-panel 规则
文件：`frontend/src/v2-missing.css`

第 758 行的 `.login-v2-panel { width: 420px; ... }` 之前被包进了 `@media (min-width: 769px)`，保持这个改动。

## 验证步骤
1. `npm run build` 能通过
2. 手机端登录页看起来跟设计图一致
3. 桌面端登录页不受影响
4. 邮箱和密码输入框里的图标不重叠
5. 先逛逛按钮只出现一次
6. 注册入口只出现一次（底部"注册"按钮或点击后切换）
7. 忘记密码只出现一次

## 注意
- 只修改前端，不修改后端
- 不改变业务逻辑
- 保持代码风格与现有项目一致（使用现有 CSS class 名称和变量）
- 新的样式 class 命名用 login-v2- 前缀
