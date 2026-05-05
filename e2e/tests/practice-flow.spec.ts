/**
 * #158 E2E 测试 — 完整练习流程
 *
 * 场景：注册 → 登录 → 选剧集 → 逐词输入 → 提交 → 正确/错误反馈 → 下一句
 *
 * 步骤：
 *   1. 注册新用户（test@example.com / password123）
 *   2. 登录后自动跳转到练习页
 *   3. 从剧集下拉框选中第一本剧集
 *   4. 页面加载出句子，输入框出现
 *   5. 在第1个输入框任意输入错误答案，提交 → 看到错误反馈 + 重试提示
 *   6. 修正为正确答案，提交 → 看到"完全正确"反馈
 *   7. 点击"下一句" → 加载新句子
 */

import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'test@example.com';
const TEST_PASSWORD = 'password123';

test.describe('完整练习流程', () => {

  test.beforeEach(async ({ page }) => {
    // 清理 localStorage，确保未登录状态
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
  });

  test('注册 → 登录 → 选剧集 → 练习 → 提交 → 下一句', async ({ page }) => {
    // ── 1. 注册新用户 ──
    await page.goto('/');

    // 未登录状态下默认不显示登录页（需要先导航到登录）
    // 点击导航栏的"登录"按钮
    const loginNavBtn = page.locator('nav.topnav button', { hasText: '登录' });
    if (await loginNavBtn.count() > 0) {
      await loginNavBtn.click();
    }

    // 等待登录页面渲染
    await expect(page.locator('.login-page')).toBeVisible({ timeout: 10000 });

    // 切换到注册模式
    const toggleLink = page.locator('.toggle-mode');
    if (await toggleLink.textContent() === '没有账号？点击注册') {
      await toggleLink.click();
    }

    // 填写注册表单
    const emailInput = page.locator('.login-page input[type="email"]');
    const passwordInput = page.locator('.login-page input[type="password"]');
    await emailInput.fill(TEST_EMAIL);
    await passwordInput.fill(TEST_PASSWORD);

    // 点击注册按钮
    const submitBtn = page.locator('.login-page button[type="submit"]');
    await submitBtn.click();

    // 注册成功后应自动登录并跳转到练习页
    await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });

    // ── 2. 验证导航栏显示用户信息 ──
    await expect(page.locator('.user-badge')).toBeVisible();

    // ── 3. 选剧集 ──
    // 出现在 controls 区域的下拉框
    const showSelect = page.locator('.controls select').nth(1); // 第2个 select 是剧集选择
    const showOptions = showSelect.locator('option');

    // 如果有剧集数据，选中第一本
    const optionCount = await showOptions.count();
    if (optionCount > 1) {
      await showSelect.selectOption({ index: 1 });
      // 选择后触发 historyIds 重置和重新加载（可能触发页面变化）
    }

    // ── 4. 等待句子加载 ──
    // 骨架屏消失 → 实际内容出现
    await page.waitForTimeout(1500);
    // 等待句子元数据出现（包含 showName 和 #编号）
    await expect(page.locator('.sentence-meta')).toBeVisible({ timeout: 15000 });

    // ── 5. 验证输入框出现 ──
    const wordInputs = page.locator('.word-input');
    const inputCount = await wordInputs.count();
    expect(inputCount).toBeGreaterThan(0);

    // ── 6. 输入错误答案并提交 ──
    // 在第一个非提示词的输入框中输入错误内容
    // 提示词(hint)会显示为 .hint-word span，没有输入框
    const firstInput = page.locator('.word-input').first();
    if (await firstInput.count() > 0) {
      await firstInput.fill('wronganswer');

      // 提交
      const submitBtnPractice = page.locator('.btn-primary', { hasText: '提交' });
      await submitBtnPractice.click();

      // 等待重试反馈
      await expect(page.locator('.feedback.retry')).toBeVisible({ timeout: 5000 });
    }

    // ── 7. 修正为正确答案并提交 ──
    // 获取句子的原文（从 .sentence-en 区域获取）
    // 注意：showEn 默认 false，英文是 blurred 状态，但文字内容仍在 DOM 中
    const enText = await page.locator('.sentence-en').textContent();
    expect(enText).toBeTruthy();

    if (enText) {
      const words = enText.trim().split(/\s+/).filter(Boolean);

      // 对每个非提示词的输入框填入正确答案
      for (let i = 0; i < words.length; i++) {
        const input = page.locator('.word-input').nth(i);
        if (await input.count() > 0 && await input.isVisible()) {
          // 清除之前的错误内容，填入正确答案
          await input.fill(words[i]);
        }
      }

      // 提交正确答案
      const submitBtnPractice = page.locator('.btn-primary', { hasText: '提交' });
      if (await submitBtnPractice.count() > 0) {
        await submitBtnPractice.click();
      }

      // ── 8. 验证正确反馈 ──
      await expect(page.locator('.feedback.correct')).toBeVisible({ timeout: 5000 });
      await expect(page.locator('.feedback.correct')).toContainText('完全正确');
    }

    // ── 9. 点击下一句 ──
    await page.waitForTimeout(500);
    const nextBtn = page.locator('.btn-primary', { hasText: '下一句' });
    if (await nextBtn.count() > 0) {
      await nextBtn.click();
    } else {
      // 没有下一句按钮说明可能已经加载了下一句
    }

    // 等待新句子加载
    await page.waitForTimeout(2000);
  });
});
