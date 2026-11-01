// Service worker mínimo: red primero y caché como respaldo, solo para archivos de la propia app.
// Requisito para que el navegador trate la página como PWA instalable.
const CACHE = 'poc-pmv-v1';

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));

self.addEventListener('fetch', (e) => {
  const solicitud = e.request;
  // YouTube y otros orígenes no se cachean
  if (solicitud.method !== 'GET' || new URL(solicitud.url).origin !== self.location.origin) return;
  e.respondWith(
    fetch(solicitud)
      .then((respuesta) => {
        const copia = respuesta.clone();
        caches.open(CACHE).then((cache) => cache.put(solicitud, copia));
        return respuesta;
      })
      .catch(() => caches.match(solicitud))
  );
});
