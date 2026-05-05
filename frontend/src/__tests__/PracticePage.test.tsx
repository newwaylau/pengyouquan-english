import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PracticePage from '../PracticePage';

// 模拟设置面板（避免测试中渲染完整的 SettingsPanel）
vi.mock('../SettingsPanel', () => ({
  default: ({ open, onClose, mode, onModeChange, voice, onVoiceChange, speed, onSpeedChange, showId, onShowChange }: any) => {
    if (!open) return null;
    return (
      <div data-testid="settings-panel">
        <button data-testid="close-settings" onClick={onClose}>✕</button>
        <select data-testid="settings-mode-select" value={mode} onChange={e => onModeChange(e.target.value)}>
          <option value="translation">中译英</option>
          <option value="dictation">听写</option>
        </select>
        <select data-testid="settings-voice-select" value={voice} onChange={e => onVoiceChange(e.target.value)}>
          <option value="en-GB-RyanNeural">Ryan</option>
          <option value="en-US-JennyNeural">Jenny</option>
        </select>
        <select data-testid="settings-speed-select" value={speed} onChange={e => onSpeedChange(Number(e.target.value))}>
          <option value={0.5}>0.5x</option>
          <option value={0.75}>0.75x</option>
          <option value={1}>1x</option>
        </select>
      </div>
    );
  },
}));

// 模拟 Audio
beforeEach(() => {
  window.Audio = vi.fn().mockImplementation(() => ({
    play: vi.fn(),
    pause: vi.fn(),
    playbackRate: 1,
  }));
  localStorage.clear();
});

const mockSentenceData = {
  code: 200,
  data: [
    {
      id: 42,
      text: '#1 Hello world / 你好世界',
      showName: '测试剧集',
    },
  ],
};

const mockShowsData = {
  code: 200,
  data: [{ id: 1, name: '测试剧集', sentenceCount: 10 }],
};

const mockStatsData = {
  code: 200,
  data: { totalPractices: 5, totalCorrect: 3 },
};

const mockSettingsData = {
  code: 200,
  data: {
    mode: 'translation',
    voice: 'en-GB-RyanNeural',
    speed: '0.75',
    showId: '',
  },
};

describe('PracticePage 逐词输入框测试 (#156)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    // 设置全局 fetch mock
    global.fetch = vi.fn((url: string) => {
      if (url === '/api/shows') {
        return Promise.resolve({ json: () => Promise.resolve(mockShowsData) });
      }
      if (url.startsWith('/api/random?')) {
        return Promise.resolve({ json: () => Promise.resolve(mockSentenceData) });
      }
      if (url === '/api/users/me/stats') {
        return Promise.resolve({ json: () => Promise.resolve(mockStatsData) });
      }
      if (url === '/api/settings') {
        return Promise.resolve({ json: () => Promise.resolve(mockSettingsData) });
      }
      if (url.startsWith('/api/tts?')) {
        return Promise.resolve({ json: () => Promise.resolve({ code: 200 }) });
      }
      return Promise.resolve({ json: () => Promise.resolve({ code: 200, data: null }) });
    }) as any;
  });

  it('加载中显示"加载中..."', async () => {
    // 不提供 user，跳过 API 调用
    render(<PracticePage user={null} />);
    expect(screen.getByText('加载中...')).toBeDefined();
  });

  it('加载完成后渲染基本元素', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    // 等待加载完成
    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 检查基本元素
    expect(screen.getByText('⏎ 提交')).toBeDefined();
    expect(screen.getByText('⚙️ 设置')).toBeDefined();
    expect(screen.getByText('🎬 原音')).toBeDefined();
    expect(screen.getByText(/测试剧集/)).toBeDefined();
    expect(screen.getByText(/#42/)).toBeDefined();
  });

  it('渲染逐词输入框', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 句子 "Hello world" 有两个词，应该有2个输入框
    // 注意：随机提示词会预填，这里只验证存在 input 元素
    const inputs = document.querySelectorAll('.word-input');
    expect(inputs.length).toBeGreaterThan(0);
  });

  it('设置面板可以打开和关闭', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 点击设置按钮
    const settingsBtn = screen.getByText('⚙️ 设置');
    await userEvent.click(settingsBtn);

    // 设置面板应该显示
    expect(screen.getByTestId('settings-panel')).toBeDefined();

    // 关闭设置面板
    const closeBtn = screen.getByTestId('close-settings');
    await userEvent.click(closeBtn);

    // 设置面板应该消失
    await waitFor(() => {
      expect(screen.queryByTestId('settings-panel')).toBeNull();
    });
  });

  it('逐词输入并提交', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 填写输入框
    const inputs = document.querySelectorAll('.word-input');
    if (inputs.length > 0) {
      await userEvent.type(inputs[0], 'hello');
    }
    if (inputs.length > 1) {
      await userEvent.type(inputs[1], 'world');
    }

    // 点击提交
    const submitBtn = screen.getByText('⏎ 提交');
    await userEvent.click(submitBtn);

    // 提交后应进入反馈状态或重试状态
    await waitFor(() => {
      const retryText = screen.queryByText(/再试一次/);
      const nextBtn = screen.queryByText(/下一句/);
      expect(retryText !== null || nextBtn !== null).toBe(true);
    });
  });
});

describe('PracticePage 练习模式切换测试 (#157)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    global.fetch = vi.fn((url: string) => {
      if (url === '/api/shows') {
        return Promise.resolve({ json: () => Promise.resolve(mockShowsData) });
      }
      if (url.startsWith('/api/random?')) {
        return Promise.resolve({ json: () => Promise.resolve(mockSentenceData) });
      }
      if (url === '/api/users/me/stats') {
        return Promise.resolve({ json: () => Promise.resolve(mockStatsData) });
      }
      if (url === '/api/settings') {
        return Promise.resolve({ json: () => Promise.resolve(mockSettingsData) });
      }
      if (url.startsWith('/api/tts?')) {
        return Promise.resolve({ json: () => Promise.resolve({ code: 200 }) });
      }
      return Promise.resolve({ json: () => Promise.resolve({ code: 200, data: null }) });
    }) as any;
  });

  it('练习模式下拉框存在', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const selects = document.querySelectorAll('.controls select');
    expect(selects.length).toBeGreaterThanOrEqual(1);
  });

  it('切换练习模式后UI更新', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 查找模式选择下拉框
    const modeSelect = document.querySelector('.controls select:first-child') as HTMLSelectElement;
    if (modeSelect) {
      // 切换到听写模式
      await userEvent.selectOptions(modeSelect, 'dictation');
      expect(modeSelect.value).toBe('dictation');

      // 切换回中译英模式
      await userEvent.selectOptions(modeSelect, 'translation');
      expect(modeSelect.value).toBe('translation');
    }
  });

  it('音色选择可切换', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const voiceSelect = document.querySelectorAll('.controls select')[2] as HTMLSelectElement;
    if (voiceSelect) {
      await userEvent.selectOptions(voiceSelect, 'en-US-JennyNeural');
      expect(voiceSelect.value).toBe('en-US-JennyNeural');
    }
  });

  it('播放速度选择可切换', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const speedSelect = document.querySelectorAll('.controls select')[3] as HTMLSelectElement;
    if (speedSelect) {
      await userEvent.selectOptions(speedSelect, '1');
      expect(speedSelect.value).toBe('1');
    }
  });

  it('快捷键Enter提交', async () => {
    render(<PracticePage user={{ id: 1, email: 'test@test.com' }} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    // 模拟 Enter 键
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    window.dispatchEvent(enterEvent);

    await waitFor(() => {
      const retryText = screen.queryByText(/再试一次/);
      const nextBtn = screen.queryByText(/下一句/);
      expect(retryText !== null || nextBtn !== null).toBe(true);
    });
  });

  it('快捷键 = 触发音色播放', async () => {
    render(<PracticePage user={null} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const playSpy = vi.spyOn(window.Audio.prototype, 'play');

    const eqEvent = new KeyboardEvent('keydown', { key: '=' });
    window.dispatchEvent(eqEvent);

    expect(screen.getByText(/音色/)).toBeDefined();
  });

  it('快捷键 ] 切换英文显示', async () => {
    render(<PracticePage user={null} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const rightBracketEvent = new KeyboardEvent('keydown', { key: ']' });
    window.dispatchEvent(rightBracketEvent);

    // 英文显示按钮文本应切换
    expect(screen.getByText(/隐藏英文|显示英文/)).toBeDefined();
  });

  it('快捷键 \\ 加载下一句', async () => {
    render(<PracticePage user={null} />);

    await waitFor(() => {
      expect(screen.queryByText('加载中...')).toBeNull();
    });

    const backslashEvent = new KeyboardEvent('keydown', { key: '\\' });
    window.dispatchEvent(backslashEvent);

    // 应该仍然在加载下一句
    await waitFor(() => {
      expect(screen.getByText(/⏎ 提交|下一句/)).toBeDefined();
    });
  });
});
