import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.jetbrainsKotlinKapt)
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
    testImplementation(kotlin("test"))

    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.slf4j.api)

    implementation(libs.apache.commons.lang3)
    implementation(libs.apache.commons.io)

    implementation(libs.viascom.nanoid)
}

tasks.test {
    useJUnitPlatform()
}