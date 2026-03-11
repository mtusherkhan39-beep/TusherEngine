pluginManagement {
    repositories {
        google()
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

// জিতপ্যাকের বিল্ড সফল করার জন্য ডেমো অ্যাপটি আপাতত ইনক্লুড করা বন্ধ করা হলো।
// আপনি আপনার কম্পিউটারে কাজ করার সময় নিচে include(":app") যোগ করে নিতে পারেন।
include(":editor")
