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

// TRIK SAKTI: Memaksa Gradle mendownload SDK sendiri jika tidak ditemukan di Codespaces
providers.gradleProperty("android.sdk.elg").orNull?.let {
    System.setProperty("android.builder.sdkDownload", "true")
}
System.setProperty("android.builder.sdkDownload", "true")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusicStream"
include(":app")
