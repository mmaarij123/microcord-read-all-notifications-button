pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://raw.git.hoodnet.work/microcord/microcord/maven") {
            content {
                includeGroup("dev.mantikafasi.manticord")
                includeGroup("dev.mantikafasi.manticord.plugin")
            }
        }
    }
    plugins {
        id("dev.mantikafasi.manticord.plugin") version "1.0.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://raw.git.hoodnet.work/microcord/microcord/maven") {
            content { includeGroup("dev.mantikafasi.manticord") }
        }
    }
}

rootProject.name = "MicrocordReadAllNotificationsButton"
