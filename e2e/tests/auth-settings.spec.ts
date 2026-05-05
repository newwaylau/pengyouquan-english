/**
 * #159 E2E 测试 — 注册 → 登录 → 设置持久化
 *
 * 场景：注册新用户 → 登录 → 打开设置 → 切换练习模式 → 保存 → 刷新页面 → 验证设置持久化
 *
 * 步骤：
 *   1. 注册新用户（auth-test@example.com / password123）
 *   2. 注册成功后自动登录，进入练习页
 *   3. 点击设置按钮打开设置面板
 *   4. 切换练习模式为"听写"
 *   5. 切换音色为"Guy (US Male)"
 *   6. 切换播放速度为"0.5x"
 *   7. 点击设置面板外的遮罩层关闭设置
 *   8. 刷新页面
 *   9. 重新打开设置面板 → 验证模式/音色/速度与之前一致
 */

import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'auth-test@example.com';
const TEST_PASSWORD = 'password123';

test.describe('注册 → 登录 → 设置持久化', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
  });

  test('注册 → 登录 → 切换模式 → 保存 → 刷新 → 验证持久化', async ({ page }) => {
    // ── 1. 注册新用户 ──
    await page.goto('/');

    // 点击导航栏"登录"进入登录页面
    const loginNavBtn = page.locator('nav.topnav button', { hasText: '登录' });
    if (await loginNavBtn.count() > 0) {
      await loginNavBtn.click();
    }

    await expect(page.locator('.login-page')).toBeVisible({ timeout: 10000 });

    // 切换到注册模式
    const toggleLink = page.locator('.toggle-mode');
    const toggleText = await toggleLink.textContent();
    if (toggleText && toggleText.includes('注册')) {
      await toggleLink.click();
    }

    // 填写表单
    await page.locator('.login-page input[type="email"]').fill(TEST_EMAIL);
    await page.locator('.login-page input[type="password"]').fill(TEST_PASSWORD);
    await page.locator('.login-page button[type="submit"]').click();

    // 等待登录完成进入练习页
    await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });

    // ── 2. 打开设置面板 ──
    const settingsBtn = page.locator('.settings-btn');
    await settingsBtn.click();
    await expect(page.locator('.settings-panel')).toBeVisible({ timeout: 5000 });

    // ── 3. 切换练习模式为"听写" ──
    const dictationPill = page.locator('.settings-panel .pill-group').first().locator('.pill', { hasText: '听写' });
    await dictationPill.click();

    // ── 4. 切换音色为 Guy (US Male) ──
    // 音色在第三个 .pill-group 中（模式→剧集→音色→速度→...）
    // 更可靠的方式是按文本定位
    const guyPill = page.locator('.settings-panel .pill', { hasText: 'Guy' });
    if (await guyPill.count() > 0) {
      await guyPill.click();
    }

    // ── 5. 切换播放速度为 0.5x ──
    const speedPill = page.locator('.settings-panel .pill', { hasText: '0.5x' });
    if (await speedPill.count() > 0) {
      await speedPill.click();
    }

    // ── 6. 保存 — 每次点击 pill 时已经触发 save，无需额外操作
    // 关闭设置面板（点击遮罩层）
    const overlay = page.locator('.settings-overlay');
    await overlay.click({ position: { x: 10, y: 10 } }); // 点击遮罩边缘
    await expect(page.locator('.settings-panel')).not.toBeVisible({ timeout: 3000 });

    // ── 7. 等待设置保存到服务端（给 API 一点时间）
    await page.waitForTimeout(1000);

    // ── 8. 刷新页面 ──
    await page.reload();

    // 刷新后还是已登录状态（token 在 localStorage 中），应该直接进入练习页
    await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });

    // 等待设置从服务端加载（getSettings 会在页面加载时调用）
    await page.waitForTimeout(2000);

    // ── 9. 重新打开设置面板 ──
    const settingsBtn2 = page.locator('.settings-btn');
    await settingsBtn2.click();
    await expect(page.locator('.settings-panel')).toBeVisible({ timeout: 5000 });

    // ── 10. 验证设置持久化 ──
    // 验证模式 pill — 听写模式应该 active
    const activeModePill = page.locator('.settings-panel .pill-group').first().locator('.pill.active');
    await expect(activeModePill).toContainText('听写');

    // 验证音色 — Guy 应该 active
    // 使用 nth 定位到音色的 pill group（第3个 .pill-group）
    const voicePills = page.locator('.settings-panel .pill-group').nth(2);
    const activeVoicePill = voicePills.locator('.pill.active');
    await expect(activeVoicePill).toContainText('Guy');

    // 验证速度 — 0.5x 应该 active
    // 速度的 pill group
    const speedPills = page.locator('.settings-panel .pill-group').nth(3);
    const activeSpeedPill = speedPills.locator('.pill.active');
    await expect(activeSpeedPill).toContainText('0.5x');

    // ── 11. 额外验证：顶部的 controls 也保持一致 ──
    // 模式 select 框应显示听写
    const modeSelect = page.locator('.controls select').first();
    await expect(modeSelect).toHaveValue('dictation');

    // 速度 select 框应显示 0.5
    const speedSelect = page.locator('.controls select').nth(3);
    await expect(speedSelect).toHaveValue('0.5');
  });
});
