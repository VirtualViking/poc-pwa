rootProject.name = "poc-pwa"

pluginManagement {
    repositories {
        google()
        // Maven Central (URL directa)
        maven("https://repo1.maven.org/maven2")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        // Maven Central (URL directa)
        maven("https://repo1.maven.org/maven2")
    }
}

include(":webApp")
