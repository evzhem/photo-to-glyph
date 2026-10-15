plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val syntheticAssetsDirectory = layout.buildDirectory.dir("generated/m2-assets")
val prepareSyntheticAssets by tasks.registering(Copy::class) {
    from(rootProject.file("tools/fixtures/synthetic/m2")) { include("*.png") }
    into(syntheticAssetsDirectory.map { it.dir("m2") })
}

tasks.named("preBuild") { dependsOn(prepareSyntheticAssets) }

android {
    namespace = "photoglyph.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "photoglyph.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0-m2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets.getByName("main") {
        // Only our five deterministic synthetic PNGs; no user photographs.
        assets.srcDir(rootProject.file("tools/fixtures/synthetic/m0"))
        assets.srcDir(syntheticAssetsDirectory)
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.exifinterface)
    implementation(libs.lifecycle.compose)
}
