// 英语剧场 — Service Worker
// 缓存名称，版本更新时变更
const CACHE_NAME = 'pengyouquan-english-v4'

// 安装时预缓存的静态资源
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/logo192.png',
  '/logo512.png',
  '/favicon.ico',
  '/favicon.svg',
  '/icons.svg',
  '/manifest.json',
]

// 安装事件：预缓存关键资源
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  )
})

// 激活事件：清理旧缓存
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) =>
        Promise.all(
          cacheNames
            .filter((name) => name !== CACHE_NAME)
            .map((name) => caches.delete(name))
        )
      )
      .then(() => self.clients.claim())
  )
})

// 拦截请求：缓存优先（静态资源）、网络优先（API）
self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  // API 请求走网络优先
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          // 成功时缓存响应（仅 GET）
          if (request.method === 'GET') {
            const cloned = response.clone()
            caches.open(CACHE_NAME).then((cache) => cache.put(request, cloned))
          }
          return response
        })
        .catch(() => caches.match(request))
    )
    return
  }

  // 静态资源走缓存优先
  event.respondWith(
    caches.match(request).then((cached) => cached || fetch(request))
  )
})
