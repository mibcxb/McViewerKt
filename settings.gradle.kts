pluginManagement {
    repositories {
        maven("https://nexus.mibcxb.org/repository/maven-public/") {
            credentials {
                username = "mvn-user"
                password = "g3WW4mpD9Fyr"
            }
        }
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//        google()
//        gradlePluginPortal()
//        mavenCentral()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("org.jetbrains.kotlin.plugin.compose").version(extra["kotlin.version"] as String)
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://nexus.mibcxb.org/repository/maven-public/") {
            credentials {
                username = "mvn-user"
                password = "g3WW4mpD9Fyr"
            }
        }
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//        google()
//        mavenCentral()
//        mavenLocal()
    }
}

rootProject.name = "McViewerKt"
include("widget-lib")
include("viewer-lib")
include("viewer-gui")