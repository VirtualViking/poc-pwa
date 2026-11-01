// Puente entre Kotlin y las APIs del navegador: reproductor IFrame de YouTube e instalación de la PWA.
(function () {
  var reproductor = null;
  var promptInstalacion = null;

  // Android/Chrome: el navegador avisa que la app se puede instalar; se guarda para un botón propio.
  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault();
    promptInstalacion = e;
  });

  function cargarApiYouTube() {
    return new Promise(function (resolver, rechazar) {
      if (window.YT && window.YT.Player) { resolver(); return; }
      var anterior = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = function () { if (anterior) anterior(); resolver(); };
      var script = document.createElement('script');
      script.src = 'https://www.youtube.com/iframe_api';
      script.onerror = function () { rechazar('sin conexión con youtube.com'); };
      document.head.appendChild(script);
    });
  }

  var ESTADOS = { '-1': 'sin iniciar', '0': 'terminado', '1': 'reproduciendo', '2': 'pausado', '3': 'cargando', '5': 'listo para reproducir' };
  var ERRORES = { 2: 'ID inválido', 5: 'error del reproductor HTML5', 100: 'video no encontrado o privado', 101: 'no permite reproducción embebida', 150: 'no permite reproducción embebida' };

  window.PocPuente = {
    crearReproductor: function (contenedorId, videoId, alEvento) {
      cargarApiYouTube().then(function () {
        var destino = document.getElementById(contenedorId);
        if (!destino) { alEvento('error', 'No se encontró el contenedor ' + contenedorId); return; }
        reproductor = new YT.Player(destino, {
          width: '100%',
          height: '100%',
          videoId: videoId,
          playerVars: { playsinline: 1, rel: 0 },
          events: {
            onReady: function () { alEvento('listo', 'listo'); },
            onStateChange: function (e) {
              var tipo = e.data === 0 ? 'terminado' : e.data === 1 ? 'reproduciendo' : e.data === 2 ? 'pausado' : 'estado';
              alEvento(tipo, ESTADOS[String(e.data)] || String(e.data));
            },
            onError: function (e) {
              alEvento('error', 'código ' + e.data + ' (' + (ERRORES[e.data] || 'desconocido') + ')');
            }
          }
        });
      }).catch(function (err) { alEvento('error', 'No cargó la API de YouTube: ' + err); });
    },
    cargar: function (videoId) { if (reproductor) reproductor.loadVideoById(videoId); },
    reproducir: function () { if (reproductor) reproductor.playVideo(); },
    pausar: function () { if (reproductor) reproductor.pauseVideo(); },
    saltarAlFinal: function () {
      if (!reproductor) return;
      var duracion = reproductor.getDuration();
      if (duracion > 8) reproductor.seekTo(duracion - 5, true);
    },
    puedeInstalar: function () { return promptInstalacion !== null; },
    instalar: function () {
      if (!promptInstalacion) return;
      promptInstalacion.prompt();
      promptInstalacion.userChoice.finally(function () { promptInstalacion = null; });
    },
    info: function () {
      var instalada = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
      var sw = navigator.serviceWorker && navigator.serviceWorker.controller ? 'activo' : 'no activo';
      var ios = /iPhone|iPad|iPod/.test(navigator.userAgent);
      return (instalada ? 'App instalada (pantalla completa)' : 'Abierta en el navegador') + ' · Service worker: ' + sw + (ios ? ' · iOS' : '');
    }
  };
})();
