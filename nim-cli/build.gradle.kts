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
