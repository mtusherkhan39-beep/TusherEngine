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

// স্মার্ট ইনক্লুড: যদি 'app' ফোল্ডারটি থাকে (যেমন আপনার লোকালে), তবেই এটি ইনক্লুড হবে।
// এটি করলে জিতপ্যাকে বিল্ড ফেইল হবে না কারণ সেখানে 'app' ফোল্ডারটি থাকবে না।
if (java.io.File(rootDir, "app").exists()) {
    include(":app")
}

include(":editor")
