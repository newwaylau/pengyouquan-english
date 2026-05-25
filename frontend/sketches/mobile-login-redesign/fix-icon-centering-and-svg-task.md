# Fix Login Input Icon Issues

Two issues to fix on the login page:

## 1. 邮箱输入框图标改成用户图标
File: `frontend/src/LoginPage.tsx`, line 456

The current SVG is NOT an @ symbol (it has a circle + path that looks like a user), but change it to a clearer user icon so users don't mistake it for @.

Current SVG (line 456):
```tsx
<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"><circle cx="12" cy="12" r="4"/><path d="M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94"/></svg>
```

Replace with a standard user/person icon (head + shoulders, like Lucide's `user` icon):
```tsx
<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
```

## 2. 密码框图标没有垂直居中
Root cause: `.pwd-wrapper` has `display: flex; align-items: center` but `.pwd-wrapper input` has `margin-bottom: 12px` (inherited from `.field-group input`). This makes `.pwd-wrapper` 57px tall while the input is only 45px. The icon uses `top: 50%` relative to the parent, so it's 6px off-center.

Also, the email icon inside `.login-input-icon-wrap` should be more robustly centered too.

**Fix for password input**: In `frontend/src/v2-missing.css`, add `margin-bottom: 0` to the `.field-group .pwd-wrapper input` rule (around line 310-312), and also to the regular `.pwd-wrapper input` rule (around line 351-353):

```css
.field-group .pwd-wrapper input {
  padding: 10px 40px 10px 36px;
  margin-bottom: 0;
}
```

And update line 345-ish:
```css
.pwd-wrapper input {
  width: 100%;
  padding: 10px 40px 10px 36px;
  margin-bottom: 0;
  ...
}
```

**Fix for email icon centering**: Also add `margin-bottom: 0` on the field-group email input override.

## Verification
1. `npm run build` succeeds
2. Email icon shows a user/person icon (head + shoulders), not @
3. Both email and password input icons are vertically centered
4. Icons don't overlap with input text
5. Test on both dark and light modes
