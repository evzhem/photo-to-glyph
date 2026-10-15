import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    group = "photoglyph"
    version = "0.1.0-SNAPSHOT"
}

val ktlintVersion = libs.versions.ktlint.cli.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<KtlintExtension> {
        version.set(ktlintVersion)
        filter {
            exclude("**/build/**", "**/generated/**")
        }
    }
    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt.yml"))
        source.setFrom(files("src/main/kotlin"))
        ignoreFailures = false
    }
}

tasks.named("build") {
    dependsOn(":core:build", ":app:build", ":cli:build", ":tools:check")
}

tasks.named("check") {
    dependsOn(":core:check", ":app:check", ":cli:check", ":tools:check")
}
