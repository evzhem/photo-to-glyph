plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}

application {
    mainClass.set("photoglyph.cli.MainKt")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8")
}

tasks.register<JavaExec>("smoke") {
    group = "verification"
    description = "Run the real Kotlin pipeline on the five original synthetic PNGs."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args(
        "smoke",
        rootProject.file("tools/fixtures/synthetic/m0").absolutePath,
        layout.buildDirectory.dir("smoke").get().asFile.absolutePath,
    )
    jvmArgs("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

tasks.register<JavaExec>("sandbox") {
    group = "application"
    description = "Serve the diagnostic browser sandbox using the real core (not the Android app)."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args("sandbox", rootProject.projectDir.absolutePath, "8080")
    jvmArgs("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8")
}

tasks.test {
    workingDir(rootProject.projectDir)
    systemProperty("photoglyph.projectRoot", rootProject.projectDir.absolutePath)
    jvmArgs("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8")
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
