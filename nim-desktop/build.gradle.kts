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


// goi launcher tranh loi ve classpath va modulepath khi ke thua tha?ng tu application
application {
    mainClass.set("nim.desktop.Launcher")
}