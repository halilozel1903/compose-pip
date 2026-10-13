plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.github.halilozel1903.pip"
    compileSdk = 37

    defaultConfig {
        // PictureInPictureParams (aspect ratio, actions, source rect hint) start at Android 8.0.
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":pip-core"))
    api(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    // ComponentActivity: OnPictureInPictureModeChangedProvider, OnUserLeaveHintProvider.
    api(libs.androidx.activity.compose)
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.compose-pip".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "pip")
    pom {
        name.set("Compose PiP")
        description.set("Picture-in-picture for Compose video screens: auto enter, custom actions, aspect ratio, source rect hints and seamless resize on phone, tablet and TV.")
    }
}
