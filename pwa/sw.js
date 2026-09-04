/* Service Worker: CSS dentro del HTML; fondo compuesto + shell offline, API a red, HTML fresco. */
const CACHE = "kapta-pwa-v12";
const SHELL = ["./app.js?v=11", "./manifest.webmanifest", "./img/logo-slogan.png?v=9",
  "./img/fondo-login.png?v=12",
  "./img/flags/colombia.png?v=10", "./img/flags/mexico.png?v=10", "./img/flags/peru.png?v=10",
  "./img/flags/chile.png?v=10", "./img/flags/argentina.png?v=10", "./img/flags/ecuador.png?v=10",
  "./img/formas/blanca.png?v=10",
  "./img/formas/cuadro-mor.png?v=10", "./img/formas/frijol-der.png?v=10"];

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
