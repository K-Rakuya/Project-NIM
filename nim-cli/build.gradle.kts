plugins {
    application
}

dependencies {
    implementation(project(":nim-core"))
}

application {
    mainClass.set("nim.cli.Main")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

tasks.register<JavaExec>("benchmark") {
    group = "application"
    description = "Chạy công cụ tự đấu AI vs AI, xuất số liệu CSV"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("nim.cli.BenchmarkRunner")
}


