import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}
val mcViewerVersion: String by rootProject.extra

group = "com.mibcxb"
version = mcViewerVersion

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)

    implementation(project(":common-lib"))
    implementation(project(":widget-lib"))
    implementation(project(":viewer-lib"))

    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.jetbrains.lifecycle.runtime)
    implementation(libs.jetbrains.lifecycle.viewmodel)
    implementation(libs.jetbrains.navigation)
    implementation(libs.jetbrains.material3.adaptive)

    implementation(libs.vinceglb.filekit.core)
    implementation(libs.vinceglb.filekit.dialogs)
    implementation(libs.vinceglb.filekit.dialogs.compose)

    implementation(libs.coil.compose)
}

compose.desktop {
    application {
        mainClass = "com.mibcxb.viewer.gui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "McViewer"
            packageVersion = mcViewerVersion
        }
    }
}