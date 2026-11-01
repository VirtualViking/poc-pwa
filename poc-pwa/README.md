# PoC PMV: reproductor de YouTube en Compose web + PWA

Prueba de concepto del paso 4 del plan. **No es la app**: valida, antes de construirla, los dos riesgos técnicos:

1. **Reproductor de YouTube dentro de Compose web.** Compose dibuja en un `<canvas>` y el reproductor de YouTube es un `<iframe>` HTML. Se incrusta con `HtmlElementView` y se controla con la API IFrame de YouTube a través de un puente en JavaScript (`puente-youtube.js`).
2. **PWA instalable.** Manifest, íconos y service worker para instalarla en la pantalla de inicio.

Además prueba el **modo de compatibilidad**: los navegadores con WasmGC cargan la versión WebAssembly y los antiguos, la versión JavaScript. La pantalla muestra cuál cargó.

## Estructura

```
poc-pwa/
├── settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
├── .github/workflows/pages.yml          # despliegue en GitHub Pages
└── webApp/
    ├── build.gradle.kts                 # targets js + wasmJs
    └── src/
        ├── webMain/kotlin/.../App.kt     # pantalla en Compose
        ├── webMain/kotlin/.../Puente.kt  # declaración external del puente JS
        ├── webMain/kotlin/.../main.kt    # ComposeViewport
        ├── jsMain/, wasmJsMain/          # indica qué versión cargó
        └── webMain/resources/
            ├── index.html                # etiquetas PWA y de iOS
            ├── puente-youtube.js         # API IFrame de YouTube + instalación
            ├── manifest.webmanifest
            ├── sw.js                     # service worker
            └── icons/
```

Versiones: Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material3 1.9.0, Gradle 8.14.3. Requiere JDK 17 o superior.

## Ejecutar en local (IntelliJ IDEA)

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun   # versión WebAssembly, abre el navegador
./gradlew :webApp:jsBrowserDevelopmentRun       # versión JavaScript
./gradlew :webApp:composeCompatibilityBrowserDistribution   # build final (las dos versiones)
```

El build final queda en `webApp/build/dist/composeWebCompatibility/productionExecutable`.

En local, el service worker y la instalación solo funcionan en `localhost`. Para probar en el celular hay que publicarla con HTTPS.

## Publicar en GitHub Pages (para probar en el celular)

1. Crea un repositorio en GitHub, por ejemplo `poc-pwa`, y sube esta carpeta a la rama `main`. Las URLs de GitHub Pages en cuentas gratuitas requieren que el repositorio sea **público**.
2. En el repositorio, ve a *Settings → Pages* y en **Source** elige **GitHub Actions**.
3. El workflow `.github/workflows/pages.yml` se ejecuta en cada push a `main` (o a mano desde *Actions → Publicar PoC en GitHub Pages → Run workflow*). La primera vez tarda varios minutos.
4. Al terminar, la URL aparece en el resumen del workflow y en *Settings → Pages*, con la forma `https://<tu-usuario>.github.io/poc-pwa/`. Ábrela en Chrome en tu Android.

## Videos de prueba

Están en `App.kt` (`videosPrueba`). Si uno de los dos videos válidos muestra el error 101 o 150 (el dueño no permite reproducirlo incrustado), reemplaza su ID por otro video musical. Esa es justamente la validación que hará la app real (RF-26).

## Criterios de la prueba en Android

Marca cada criterio en el celular. El registro de eventos de la pantalla sirve como evidencia: toma capturas.

| # | Criterio | Cómo probarlo | Resultado esperado | ✔ / ✘ |
|---|---|---|---|---|
| 1 | La app abre desde una URL | Abrir la URL de GitHub Pages en Chrome | Se ve la pantalla de la PoC | |
| 2 | Carga la versión Wasm | Leer "Versión" en la tarjeta superior | "WebAssembly" en un Android actualizado | |
| 3 | Tiempo de carga (RNF-03) | Con datos móviles 4G, sin Wi-Fi, medir desde abrir la URL hasta ver la pantalla | 5 s o menos. Anotar el tiempo real | |
| 4 | El reproductor aparece dentro de Compose | Mirar el recuadro negro 16:9 | Se ve el video de YouTube en su lugar, sin tapar otros elementos | |
| 5 | Reproduce con sonido tras un toque | Tocar **Iniciar** | El video suena. Si no, anotar si hubo que tocar el play del propio video | |
| 6 | Avance automático (RF-11) | Tocar **Saltar al final** y esperar 5 s | El registro muestra "Terminó (RF-11)" y empieza el video 2 | |
| 7 | Salto de video fallido (RF-25) | En el video 2, tocar **Saltar al final** | Carga el ID inválido, el registro muestra el error y salta solo al video 1 | |
| 8 | Instalación (RF-03) | Tocar **Instalar**, o en el menú de Chrome "Instalar app" o "Agregar a pantalla principal" | Aparece el ícono en la pantalla de inicio | |
| 9 | Abre como app | Abrir desde el ícono | Sin barra del navegador; la tarjeta dice "App instalada (pantalla completa)" | |
| 10 | Service worker | Leer la tarjeta superior | "Service worker: activo" | |

## iOS: riesgo no verificado

- Kotlin/Wasm requiere Safari 18.2 o superior (iOS 18.2+). En versiones anteriores debe cargar la versión JavaScript.
- No se pudo probar en un iPhone. Quedan sin verificar: "Agregar a inicio" desde Safari, la reproducción con sonido tras el toque y el modo pantalla completa.
- Opciones para verificarlo sin un iPhone propio: pruebas gratuitas de BrowserStack o LambdaTest con iPhones reales, o el iPhone de un compañero.
- Si no se verifica, debe declararse como riesgo no verificado en el documento del proyecto.

## Estado de la verificación antes de entregar

- Versión **JavaScript**: compilada y probada en Chromium. Se verificó el dibujo de la pantalla, el contenedor del reproductor, el registro del service worker, el manifest y el salto ante un error de carga.
- Versión **WebAssembly**: compila sin errores. Su empaquetado final no se pudo ejecutar en el entorno donde se preparó la PoC por una restricción de red de ese entorno. Lo hace GitHub Actions al publicar; si el workflow falla, el log indica el paso.
- **YouTube**: no se pudo reproducir un video real en ese entorno (sin acceso a youtube.com). Los criterios 4 a 7 se verifican en el celular.

## Hallazgo ya medido: peso de la descarga

La versión JavaScript descarga cerca de **4 MB comprimidos** (motor gráfico Skia en Wasm: 3,2 MB; app: 0,7 MB). Es el motivo del criterio 3: con 4G puede quedar al límite del RNF-03 (5 s). Si no se cumple, opciones para la app real: pantalla de carga liviana en HTML mientras carga Compose, compresión Brotli en el hosting y caché del service worker para las visitas siguientes.
