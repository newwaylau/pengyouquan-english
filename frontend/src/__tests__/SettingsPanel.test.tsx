import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SettingsPanel from '../SettingsPanel';

// 模拟 api client
vi.mock('../api/client', () => ({
  api: {
    shows: vi.fn().mockResolvedValue({
      code: 200,
      data: [
        { id: 1, name: '老友记', sentenceCount: 120 },
        { id: 2, name: '生活大爆炸', sentenceCount: 85 },
      ],
    }),
    saveSettings: vi.fn().mockResolvedValue({ code: 200 }),
  },
}));

describe('SettingsPanel 设置面板', () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    mode: 'translation',
    onModeChange: vi.fn(),
    voice: 'en-GB-RyanNeural',
    onVoiceChange: vi.fn(),
    speed: 0.75,
    onSpeedChange: vi.fn(),
    showId: null as number | null,
    onShowChange: vi.fn(),
    autoPlay: true,
    onAutoPlayChange: vi.fn(),
    preferOriginal: false,
    onPreferOriginalChange: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // --- 打开 / 关闭 ---
  it('open=false 时返回 null 不渲染', () => {
    const { container } = render(<SettingsPanel {...defaultProps} open={false} />);
    expect(container.innerHTML).toBe('');
  });

  it('open=true 时渲染设置面板', () => {
    render(<SettingsPanel {...defaultProps} />);
    expect(screen.getByText('设置')).toBeDefined();
    expect(screen.getByText('关闭')).toBeDefined();
  });

  it('点击关闭按钮调用 onClose', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const closeBtn = screen.getByText('关闭');
    await userEvent.click(closeBtn);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('点击遮罩层调用 onClose', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const overlay = document.querySelector('.settings-overlay')!;
    await userEvent.click(overlay);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('点击面板内部不触发 onClose（冒泡阻止）', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const panel = document.querySelector('.settings-panel')!;
    await userEvent.click(panel);
    expect(defaultProps.onClose).not.toHaveBeenCalled();
  });

  // --- 练习模式切换 ---
  it('模式选择按钮存在', () => {
    render(<SettingsPanel {...defaultProps} />);
    expect(screen.getByText('中译英')).toBeDefined();
    expect(screen.getByText('听写')).toBeDefined();
  });

  it('当前模式高亮', () => {
    render(<SettingsPanel {...defaultProps} mode="dictation" />);
    const pills = document.querySelectorAll('.pill');
    expect(pills[1].classList.contains('active')).toBe(true);
  });

  it('点击模式按钮切换并保存', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const dictationBtn = screen.getByText('听写');
    await userEvent.click(dictationBtn);
    expect(defaultProps.onModeChange).toHaveBeenCalledWith('dictation');
  });

  // --- 音色切换 ---
  it('音色选择按钮列表存在', () => {
    render(<SettingsPanel {...defaultProps} />);
    expect(screen.getByText('Jenny (US Female)')).toBeDefined();
    expect(screen.getByText('Guy (US Male)')).toBeDefined();
    expect(screen.getByText('Sonia (UK Female)')).toBeDefined();
    expect(screen.getByText('Ryan (UK Male)')).toBeDefined();
    expect(screen.getByText('Natasha (AU Female)')).toBeDefined();
    expect(screen.getByText('William (AU Male)')).toBeDefined();
  });

  it('当前音色高亮', () => {
    render(<SettingsPanel {...defaultProps} voice="en-US-JennyNeural" />);
    const activePills = document.querySelectorAll('.pill-group .pill.active');
    // 音色是第2个 pill-group（第1个是模式，第3个是速度）
    const voicePills = document.querySelectorAll('.settings-section:nth-child(4) .pill.active');
    expect(voicePills.length).toBe(1);
    expect(voicePills[0].textContent).toContain('Jenny (US Female)');
  });

  it('点击音色按钮切换并保存', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const jennyBtn = screen.getByText('Jenny (US Female)');
    await userEvent.click(jennyBtn);
    expect(defaultProps.onVoiceChange).toHaveBeenCalledWith('en-US-JennyNeural');
  });

  // --- 播放速度切换 ---
  it('速度选择按钮列表存在', () => {
    render(<SettingsPanel {...defaultProps} />);
    expect(screen.getByText('0.5x')).toBeDefined();
    expect(screen.getByText('0.75x')).toBeDefined();
    expect(screen.getByText('1x')).toBeDefined();
    expect(screen.getByText('1.5x')).toBeDefined();
  });

  it('当前速度高亮', () => {
    render(<SettingsPanel {...defaultProps} speed={1} />);
    const speedPills = document.querySelectorAll('.settings-section:nth-child(5) .pill.active');
    expect(speedPills.length).toBe(1);
    expect(speedPills[0].textContent).toContain('1x');
  });

  it('点击速度按钮切换并保存', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const speedBtn = screen.getByText('1x');
    await userEvent.click(speedBtn);
    expect(defaultProps.onSpeedChange).toHaveBeenCalledWith(1);
  });

  // --- 剧集选择 ---
  it('加载剧集下拉列表', async () => {
    render(<SettingsPanel {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText('老友记 (120句)')).toBeDefined();
      expect(screen.getByText('生活大爆炸 (85句)')).toBeDefined();
    });
  });

  it('取消选择的剧集显示"全部剧集"', () => {
    render(<SettingsPanel {...defaultProps} showId={null} />);
    const select = document.querySelector('select') as HTMLSelectElement;
    expect(select.value).toBe('');
  });

  // --- 自动播放 ---
  it('自动播放复选框默认选中', () => {
    render(<SettingsPanel {...defaultProps} autoPlay={true} />);
    const checkbox = screen.getByLabelText('加载句子后自动播报语音') as HTMLInputElement;
    expect(checkbox.checked).toBe(true);
  });

  it('切换自动播放复选框', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const checkbox = screen.getByLabelText('加载句子后自动播报语音') as HTMLInputElement;
    await userEvent.click(checkbox);
    expect(defaultProps.onAutoPlayChange).toHaveBeenCalledWith(false);
  });

  // --- 原音优先 ---
  it('原音优先复选框默认未选中', () => {
    render(<SettingsPanel {...defaultProps} />);
    const checkbox = screen.getByLabelText('有原音文件时优先播放原音') as HTMLInputElement;
    expect(checkbox.checked).toBe(false);
  });

  it('切换原音优先复选框', async () => {
    render(<SettingsPanel {...defaultProps} />);
    const checkbox = screen.getByLabelText('有原音文件时优先播放原音') as HTMLInputElement;
    await userEvent.click(checkbox);
    expect(defaultProps.onPreferOriginalChange).toHaveBeenCalledWith(true);
  });

  // --- 重置默认 ---
  it('重置按钮调用所有回调并保存默认值', async () => {
    render(<SettingsPanel {...defaultProps} mode="dictation" voice="en-US-JennyNeural" speed={1} />);
    const resetBtn = screen.getByText('🔄 重置为默认值');
    await userEvent.click(resetBtn);

    expect(defaultProps.onModeChange).toHaveBeenCalledWith('translation');
    expect(defaultProps.onVoiceChange).toHaveBeenCalledWith('en-GB-RyanNeural');
    expect(defaultProps.onSpeedChange).toHaveBeenCalledWith(0.75);
    expect(defaultProps.onShowChange).toHaveBeenCalledWith(null);
    expect(defaultProps.onAutoPlayChange).toHaveBeenCalledWith(true);
    expect(defaultProps.onPreferOriginalChange).toHaveBeenCalledWith(false);
  });

  // --- 快捷键提示 ---
  it('快捷键提示列表存在', () => {
    render(<SettingsPanel {...defaultProps} />);
    expect(screen.getByText(/=/)).toBeDefined();
    expect(screen.getByText(/-/)).toBeDefined();
    expect(screen.getByText(/Enter/)).toBeDefined();
    expect(screen.getByText(/\]/)).toBeDefined();
    expect(screen.getByText(/\[/)).toBeDefined();
    expect(screen.getByText(/\\/)).toBeDefined();
  });
});
