import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Versión JS: respaldo para navegadores sin soporte de WasmGC (modo de compatibilidad)
    js {
        outputModuleName = "webApp"
        browser {
            // La PoC no tiene pruebas de navegador; evita descargar Karma
            testTask { enabled = false }
        }
        binaries.executable()
    }

    // Versión Wasm: la usan los navegadores modernos
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "webApp"
        browser {
            testTask { enabled = false }
        }
        binaries.executable()
    }

    sourceSets {
        webMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.compose.material3)
        }
    }
}
