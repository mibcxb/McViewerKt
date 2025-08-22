plugins {
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.jetbrainsKotlinKsp) apply false
    alias(libs.plugins.jetbrainsKotlinKapt) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val mcViewerVersion by extra("1.0.0")
val mcWidgetVersion by extra("1.0.0")