pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TusherEngine"

// আপনার কম্পিউটারে app ফোল্ডার থাকলে সেটি দেখাবে, কিন্তু জিতপ্যাকে এটি ইগনোর হবে।
val appDir = java.io.File(rootDir, "app")
if (appDir.exists() && java.io.File(appDir, "build.gradle.kts").exists()) {
    include(":app")
}

include(":editor")
