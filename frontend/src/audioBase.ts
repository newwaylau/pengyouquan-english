/**
 * 音频基础 URL 工具
 * IPv6 用户直连 en6.pengyouquan.top（快）
 * IPv4 用户走 Cloudflare（相对路径，正常可用）
 */

let audioBase = ''; // 默认相对路径（IPv4 Cloudflare）
let checked = false;

/** 检测当前用户是否有 IPv6 直连能力 */
function checkIPv6(): Promise<string> {
  return new Promise((resolve) => {
    // 创建一个 Image 对象测试 en6 连通性
    const img = new Image();
    const timeout = setTimeout(() => {
      // 超时 = 没有 IPv6
      checked = true;
      resolve('');
    }, 3000);

    img.onload = () => {
      clearTimeout(timeout);
      checked = true;
      audioBase = 'https://en6.pengyouquan.top';
      resolve(audioBase);
    };

    img.onerror = () => {
      // 加载失败可能只是资源不存在，不代表 IPv6 不通
      // 换用 fetch 再试一次
      clearTimeout(timeout);
      fetch('https://en6.pengyouquan.top/favicon.ico', { mode: 'no-cors' })
        .then(() => {
          checked = true;
          audioBase = 'https://en6.pengyouquan.top';
          resolve(audioBase);
        })
        .catch(() => {
          checked = true;
          resolve('');
        });
    };

    // 用一个小请求测试 IPv6 连通性
    img.src = `https://en6.pengyouquan.top/favicon.ico?t=${Date.now()}`;
  });
}

/** 获取音频基础 URL（IPv6 优先） */
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

/** 初始化 IPv6 检测（在应用启动时调用） */
export function initAudioBase(): void {
  if (!checked) {
    checkIPv6().catch(() => {});
  }
}
