package pmv.musica.poc

/**
 * Puente hacia `puente-youtube.js` (cargado en index.html).
 * Encapsula la API IFrame de YouTube y la instalación de la PWA, que son APIs del navegador.
 */
external object PocPuente {
    /** Crea el reproductor dentro del elemento con id [contenedorId]. [alEvento] recibe (tipo, detalle). */
    fun crearReproductor(contenedorId: String, videoId: String, alEvento: (String, String) -> Unit)
    fun cargar(videoId: String)
    fun reproducir()
    fun pausar()
    /** Adelanta el video a sus últimos 5 segundos para probar el avance automático sin esperar. */
    fun saltarAlFinal()
    /** true cuando el navegador ofreció instalar la app (evento beforeinstallprompt, solo Chrome/Android). */
    fun puedeInstalar(): Boolean
    fun instalar()
    /** Texto con el modo de ejecución: navegador o instalada, estado del service worker. */
    fun info(): String
}

/** "WebAssembly" o "JavaScript (respaldo)": muestra qué versión cargó el modo de compatibilidad. */
expect val plataforma: String
