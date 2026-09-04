/* Service Worker: shell offline, API siempre a red, HTML siempre fresco. */
const CACHE = "kapta-pwa-v5";
const SHELL = ["./styles.css", "./app.js", "./manifest.webmanifest", "./img/logo-slogan.png",
  "./img/flags/colombia.png", "./img/flags/mexico.png", "./img/flags/peru.png",
  "./img/flags/chile.png", "./img/flags/argentina.png", "./img/flags/ecuador.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  // La API del negocio siempre va a la red (datos en vivo).
  if (url.pathname === "/exec" || url.searchParams.has("action")) {
    return;
  }
  // El HTML siempre fresco de la red (evita mezclar versiones vieja/nueva).
  if (e.request.mode === "navigate" || url.pathname.endsWith(".html")) {
    e.respondWith(
      fetch(e.request).then((res) => {
        const copia = res.clone();
        caches.open(CACHE).then((c) => c.put(e.request, copia));
        return res;
      }).catch(() => caches.match(e.request))
    );
    return;
  }
  e.respondWith(
    caches.match(e.request).then((hit) => hit || fetch(e.request).then((res) => {
      const copia = res.clone();
      caches.open(CACHE).then((c) => c.put(e.request, copia));
      return res;
    }))
  );
});
