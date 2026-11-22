import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("dev.mantikafasi.manticord.plugin")
}

val pluginPackage = "io.github.mmaarij123.readallnotificationsbutton"
val signingProperties = layout.projectDirectory.file("keystore.properties").asFile
    .takeIf { it.isFile }
    ?.inputStream()?.use { source -> Properties().apply { load(source) } }

manticordPlugin {
    id.set(pluginPackage)
    entrypoint.set("$pluginPackage.ReadAllNotificationsPlugin")
    description.set("Mark unread server channels and joined threads as read with one tap")
    manticordVersionMinimum.set(1)
    manticordVersionMaximumExclusive.set(2)
    usesResources.set(true)
    async()
}

android {
    namespace = pluginPackage
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = pluginPackage
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingProperties?.let { properties ->
        val releaseSigner = signingConfigs.create("release") {
            storeFile = file(properties.getProperty("storeFile"))
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
        buildTypes.named("release") { signingConfig = releaseSigner }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly("dev.mantikafasi.manticord:plugin-api:1.0.6")
    compileOnly(libs.rxjava)
    compileOnly(libs.appcompat)
    compileOnly(libs.fragment)
}
