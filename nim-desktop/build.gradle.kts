plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

dependencies {
    implementation(project(":nim-core"))
}

javafx {
    version = "21"
    modules = listOf("javafx.controls")
}

application {
    mainClass.set("nim.desktop.Launcher")
}
