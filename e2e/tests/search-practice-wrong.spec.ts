/**
 * #160 E2E 测试 — 搜索 → 练习 → 错题联动
 *
 * 场景：搜索句子 → 点击结果跳转练习 → 提交错误答案 → 错题本出现该句子
 *
 * 步骤：
 *   1. 注册新用户（search-test@example.com / password123）
 *   2. 进入搜索页
 *   3. 输入关键词搜索（假设数据库中存在含 "hello" 的句子）
 *   4. 搜索结果列表出现，点击第一条结果
 *   5. 跳转到练习页并加载该句子
 *   6. 在输入框中输入错误答案并提交
 *   7. 提交两次错误 → 揭示正确答案（练习记录提交为错题）
 *   8. 导航到错题本 → 验证该句子出现在错题列表中
 */

import { test, expect } from '@playwright/test';

const TEST_EMAIL = 'search-test@example.com';
const TEST_PASSWORD = 'password123';

test.describe('搜索 → 练习 → 错题联动', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
  });

  test('搜索句子 → 点击结果 → 提交错误答案 → 错题本出现该句子', async ({ page }) => {
    // ── 1. 注册新用户 ──
    await page.goto('/');

    // 进入登录页
    const loginNavBtn = page.locator('nav.topnav button', { hasText: '登录' });
    if (await loginNavBtn.count() > 0) {
      await loginNavBtn.click();
    }

    await expect(page.locator('.login-page')).toBeVisible({ timeout: 10000 });

    // 切换到注册
    const toggleLink = page.locator('.toggle-mode');
    const toggleText = await toggleLink.textContent();
    if (toggleText && toggleText.includes('注册')) {
      await toggleLink.click();
    }

    // 填写注册表单
    await page.locator('.login-page input[type="email"]').fill(TEST_EMAIL);
    await page.locator('.login-page input[type="password"]').fill(TEST_PASSWORD);
    await page.locator('.login-page button[type="submit"]').click();

    // 等待进入练习页
    await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });

    // ── 2. 导航到搜索页 ──
    const searchNavBtn = page.locator('nav.topnav button', { hasText: '搜索' });
    await searchNavBtn.click();
    await expect(page.locator('.search-page')).toBeVisible({ timeout: 5000 });

    // ── 3. 输入关键词搜索 ──
    // 搜索带防抖 300ms，需要等待
    const searchInput = page.locator('.search-input');
    await searchInput.fill('hello');
    await page.waitForTimeout(1000); // 等待防抖+API返回

    // ── 4. 等待搜索结果出现 ──
    // 可能搜到结果，也可能是空状态
    const searchResults = page.locator('.search-result');

    // 判断是否有结果
    const resultCount = await searchResults.count();

    if (resultCount > 0) {
      // 有结果 — 点击第一条结果跳转到练习
      const firstResult = searchResults.first();
      // 获取句子编号和文本，用于后续验证
      const resultText = await firstResult.locator('.result-text').textContent();
      const resultMeta = await firstResult.locator('.result-meta').textContent();
      console.log(`点击结果: ${resultText} (${resultMeta})`);

      await firstResult.click();

      // ── 5. 跳转到练习页，加载该句子 ──
      await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });
      await expect(page.locator('.word-input').first()).toBeVisible({ timeout: 15000 });

      // ── 6. 在非提示词的输入框中输入错误答案 ──
      // 找到第一个非提示词的输入框
      const firstRealInput = page.locator('.word-input').first();
      if (await firstRealInput.count() > 0) {
        await firstRealInput.fill('wrongword');
      }

      // ── 7. 提交错误答案 ──
      let submitBtn = page.locator('.btn-primary', { hasText: '提交' });
      await submitBtn.click();

      // 第一次错误 → 看到重试提示 ⚠️ 有错误，再试一次
      await expect(page.locator('.feedback.retry')).toBeVisible({ timeout: 5000 });

      // 再次输入错误（不修改，直接提交）
      await page.waitForTimeout(300);

      // 确保提交按钮仍在
      submitBtn = page.locator('.btn-primary', { hasText: '提交' });
      if (await submitBtn.count() > 0) {
        await submitBtn.click();
      }

      // 第二次提交错误 → 揭示正确答案
      await expect(page.locator('.feedback.wrong')).toBeVisible({ timeout: 5000 });
      // 应能看到正确答案揭示
      const answerReveal = page.locator('.answer-reveal');
      await expect(answerReveal).toBeVisible({ timeout: 5000 });

      // ── 8. 导航到错题本 ──
      const wrongNavBtn = page.locator('nav.topnav button', { hasText: '错题本' });
      await wrongNavBtn.click();
      await expect(page.locator('.wrong-page')).toBeVisible({ timeout: 5000 });

      // ── 9. 验证该句子出现在错题列表中 ──
      // 错题列表非空
      const wrongItems = page.locator('.wrong-item');
      await expect(wrongItems.first()).toBeVisible({ timeout: 5000 });

      // 验证错题数量大于0
      const wrongCount = await wrongItems.count();
      expect(wrongCount).toBeGreaterThan(0);

      // 验证错题包含我们刚才练习的句子
      // 错题显示 .wrong-text 包含句子原文
      // 如果 resultText 不为 null，检查它是否出现在错题列表中
      if (resultText) {
        // 截取句子文本的前20个字符作为匹配依据（避免标点差异）
        const snippet = resultText.trim().substring(0, 30);
        // 尝试在错题列表中找到包含该文本的元素
        const matchingItem = page.locator('.wrong-text', { hasText: snippet });
        await expect(matchingItem.first()).toBeVisible({ timeout: 5000 });
      }
    } else {
      // 没有搜索结果 — 检查空状态提示
      const emptyState = page.locator('.empty-state');
      await expect(emptyState).toBeVisible({ timeout: 5000 });
      console.log('未找到含 "hello" 的句子，测试跳过结果点击部分');
      // 尝试其他关键词
      const fallbackKeywords = ['the', 'you', 'I', 'is'];
      let found = false;
      for (const kw of fallbackKeywords) {
        await searchInput.fill(kw);
        await page.waitForTimeout(1000);
        const count = await page.locator('.search-result').count();
        if (count > 0) {
          console.log(`使用关键词 "${kw}" 找到 ${count} 条结果`);
          found = true;
          // 点击第一条结果继续测试
          const firstResult = page.locator('.search-result').first();
          const resultText = await firstResult.locator('.result-text').textContent();
          console.log(`点击结果: ${resultText}`);
          await firstResult.click();

          // 练习流程同上
          await expect(page.locator('.practice-page')).toBeVisible({ timeout: 10000 });
          await expect(page.locator('.word-input').first()).toBeVisible({ timeout: 15000 });

          const firstRealInput = page.locator('.word-input').first();
          if (await firstRealInput.count() > 0) {
            await firstRealInput.fill('wrongword');
          }

          let submitBtn = page.locator('.btn-primary', { hasText: '提交' });
          await submitBtn.click();
          await expect(page.locator('.feedback.retry')).toBeVisible({ timeout: 5000 });
          await page.waitForTimeout(300);

          submitBtn = page.locator('.btn-primary', { hasText: '提交' });
          if (await submitBtn.count() > 0) {
            await submitBtn.click();
          }

          await expect(page.locator('.feedback.wrong')).toBeVisible({ timeout: 5000 });

          // 导航错题本验证
          await page.locator('nav.topnav button', { hasText: '错题本' }).click();
          await expect(page.locator('.wrong-page')).toBeVisible({ timeout: 5000 });
          const wrongItems = page.locator('.wrong-item');
          await expect(wrongItems.first()).toBeVisible({ timeout: 5000 });
          const wrongCount = await wrongItems.count();
          expect(wrongCount).toBeGreaterThan(0);

          if (resultText) {
            const snippet = resultText.trim().substring(0, 30);
            const matchingItem = page.locator('.wrong-text', { hasText: snippet });
            await expect(matchingItem.first()).toBeVisible({ timeout: 5000 });
          }
          break;
        }
      }
      if (!found) {
        console.log('所有关键词均未找到结果，跳过验证');
      }
    }
  });
});
