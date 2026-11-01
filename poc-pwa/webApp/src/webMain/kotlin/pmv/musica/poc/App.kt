package pmv.musica.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLElement

/** Videos de prueba. El tercero es un ID inválido a propósito para probar el salto de videos fallidos (RF-25). */
private data class VideoPrueba(val titulo: String, val id: String)

private val videosPrueba = listOf(
    VideoPrueba("Video válido 1", "dQw4w9WgXcQ"),
    VideoPrueba("Video válido 2", "9bZkp7q19f0"),
    VideoPrueba("ID inválido (debe saltarse)", "xxxxxxxxxxx"),
)

private const val ID_DESTINO = "yt-destino"

private val Fondo = Color(0xFF0D0E12)
private val Tarjeta = Color(0xFF17191F)
private val Acento = Color(0xFFC8F25A)
private val TextoSecundario = Color(0xFFA7ABB5)

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun App() {
    var indice by remember { mutableIntStateOf(0) }
    var estado by remember { mutableStateOf("Cargando reproductor…") }
    var info by remember { mutableStateOf("") }
    var puedeInstalar by remember { mutableStateOf(false) }
    val registro = remember { mutableStateListOf<String>() }
    var segundos by remember { mutableIntStateOf(0) }

    fun anotar(texto: String) {
        val mm = (segundos / 60).toString().padStart(2, '0')
        val ss = (segundos % 60).toString().padStart(2, '0')
        registro.add(0, "[$mm:$ss] $texto")
        if (registro.size > 14) registro.removeAt(registro.lastIndex)
    }

    fun avanzar(motivo: String) {
        indice = (indice + 1) % videosPrueba.size
        val siguiente = videosPrueba[indice]
        anotar("$motivo · carga \"${siguiente.titulo}\"")
        PocPuente.cargar(siguiente.id)
    }

    // Reloj para el registro y refresco del estado de instalación
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            segundos++
            puedeInstalar = PocPuente.puedeInstalar()
            info = PocPuente.info()
        }
    }

    // El reproductor se crea cuando el contenedor HTML ya está en la página
    LaunchedEffect(Unit) {
        delay(400)
        anotar("Versión cargada: $plataforma")
        PocPuente.crearReproductor(ID_DESTINO, videosPrueba[0].id) { tipo, detalle ->
            estado = detalle
            when (tipo) {
                "listo" -> anotar("Reproductor listo")
                "reproduciendo" -> anotar("Reproduciendo \"${videosPrueba[indice].titulo}\"")
                "pausado" -> anotar("Pausado")
                "terminado" -> avanzar("Terminó (RF-11)")
                "error" -> { anotar("Error: $detalle"); avanzar("Video fallido saltado (RF-25)") }
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Acento, background = Fondo, surface = Tarjeta)) {
        Box(Modifier.fillMaxSize().background(Fondo), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Prueba de concepto · PMV", color = Acento, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Reproductor de YouTube en Compose web", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

                Column(
                    Modifier.fillMaxWidth().background(Tarjeta, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Versión: $plataforma", color = Color.White, fontSize = 14.sp)
                    Text(info.ifEmpty { "Detectando modo de ejecución…" }, color = TextoSecundario, fontSize = 13.sp)
                }

                // Riesgo 1: el iframe de YouTube incrustado sobre el canvas de Compose
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black, RoundedCornerShape(12.dp))) {
                    HtmlElementView<HTMLElement>(
                        factory = {
                            // Contenedor fijo: YouTube reemplaza el div interno por su iframe
                            val contenedor = document.createElement("div") as HTMLElement
                            contenedor.className = "yt-contenedor"
                            val destino = document.createElement("div")
                            destino.id = ID_DESTINO
                            contenedor.appendChild(destino)
                            contenedor
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Text("Video ${indice + 1} de ${videosPrueba.size}: ${videosPrueba[indice].titulo}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Estado: $estado", color = TextoSecundario, fontSize = 13.sp)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { PocPuente.reproducir(); anotar("Clic en Iniciar") },
                        colors = ButtonDefaults.buttonColors(containerColor = Acento, contentColor = Fondo),
                    ) { Text("Iniciar", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { PocPuente.pausar() }) { Text("Pausar") }
                    OutlinedButton(onClick = { PocPuente.saltarAlFinal(); anotar("Saltar al final") }) { Text("Saltar al final") }
                    OutlinedButton(onClick = { avanzar("Siguiente manual") }) { Text("Siguiente") }
                }

                // Riesgo 2: instalación de la PWA. En Android Chrome aparece el botón; en iOS se usa "Agregar a inicio".
                Row(
                    Modifier.fillMaxWidth().background(Tarjeta, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (puedeInstalar) "El navegador permite instalar la app." else "Instalación: usa el menú del navegador (en iPhone: Compartir y luego Agregar a inicio).",
                        color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { PocPuente.instalar() },
                        enabled = puedeInstalar,
                        colors = ButtonDefaults.buttonColors(containerColor = Acento, contentColor = Fondo),
                    ) { Text("Instalar") }
                }

                Text("Registro de eventos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Column(
                    Modifier.fillMaxWidth().background(Tarjeta, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (registro.isEmpty()) Text("Sin eventos todavía.", color = TextoSecundario, fontSize = 13.sp)
                    registro.forEach { Text(it, color = TextoSecundario, fontSize = 13.sp) }
                }
            }
        }
    }
}
