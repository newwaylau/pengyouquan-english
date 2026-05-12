/**
 * 音频基础 URL
 * 全部走相对路径（Cloudflare Tunnel 统一代理）
 * 不再需要 IPv4/IPv6 区分检测
 */

let audioBase = '';

/** 获取音频基础 URL */
export function getAudioBase(): string {
  return audioBase;
}

/** 获取完整的音频 URL */
export function getAudioUrl(path: string): string {
  if (audioBase) {
    return audioBase + path;
  }
  return path;
}

/** 获取完整的 TTS URL */
export function getTtsUrl(text: string, voice: string): string {
  const params = `text=${encodeURIComponent(text)}&voice=${encodeURIComponent(voice)}`;
  if (audioBase) {
    return audioBase + '/api/tts?' + params;
  }
  return `/api/tts?${params}`;
}

/** 初始化（空操作，保留兼容） */
export function initAudioBase(): void {
  // 不再需要 IPv6 检测
}
