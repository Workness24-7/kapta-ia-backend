/* Service Worker: CSS dentro del HTML; shell offline, API siempre a red, HTML siempre fresco. */
const CACHE = "kapta-pwa-v9";
const SHELL = ["./app.js?v=9", "./manifest.webmanifest", "./img/logo-slogan.png?v=9",
  "./img/flags/colombia.png?v=9", "./img/flags/mexico.png?v=9", "./img/flags/peru.png?v=9",
  "./img/flags/chile.png?v=9", "./img/flags/argentina.png?v=9", "./img/flags/ecuador.png?v=9",
  "./img/formas/morada-tl.png?v=9", "./img/formas/nube.png?v=9", "./img/formas/destellos.png?v=9",
  "./img/formas/cian-bl.png?v=9", "./img/formas/gris.png?v=9", "./img/formas/blanca.png?v=9",
  "./img/formas/cuadro-mor.png?v=9", "./img/formas/frijol-der.png?v=9", "./img/formas/cian-abajo.png?v=9"];

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
