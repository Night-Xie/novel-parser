const CACHE_NAME = 'novel-reader-v2';

// The essential files that run your app
const APP_SHELL = [
    '/',
    '/index.html',
    '/styles.css',
    '/scripts.js',
    '/manifest.json'
];

// 1. Install: Cache the App Shell
self.addEventListener('install', (e) => {
    e.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(APP_SHELL);
        })
    );
});

// 2. Activate: Clean up old caches if you ever update the version number
self.addEventListener('activate', (e) => {
    e.waitUntil(
        caches.keys().then((keyList) => {
            return Promise.all(keyList.map((key) => {
                if (key !== CACHE_NAME) {
                    return caches.delete(key);
                }
            }));
        })
    );
});

// 3. Fetch: Intercept network requests
self.addEventListener('fetch', (e) => {
    e.respondWith(
        caches.match(e.request).then((cachedResponse) => {
            // If the file is in the cache, serve it instantly (even if offline)
            if (cachedResponse) {
                return cachedResponse;
            }
            
            // Otherwise, fetch it from the SHTTPS server
            return fetch(e.request).then((networkResponse) => {
                // We dynamically cache any new HTML chapter you visit!
                return caches.open(CACHE_NAME).then((cache) => {
                    // Only cache successful GET requests
                    if (e.request.method === 'GET' && networkResponse.status === 200) {
                        cache.put(e.request, networkResponse.clone());
                    }
                    return networkResponse;
                });
            });
        }).catch(() => {
            // Optional: You could return a custom "You are offline" page here
            console.log("Offline and file not cached.");
        })
    );
});
