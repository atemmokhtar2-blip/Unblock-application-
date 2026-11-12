import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.marayti.unblock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marayti.unblock"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // A single persistent private key is required for install-over updates.
    // Release signing credentials are supplied only through GitHub Actions secrets.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

// Language models and the Arabic display font are bundled into the APK at build time.
// The installed application performs all OCR locally and needs no network permission.
val tessdataDir = layout.projectDirectory.dir("src/main/assets/tessdata")
val fontDir = layout.projectDirectory.dir("src/main/res/font")
tasks.register("downloadAppAssets") {
    outputs.files(
        tessdataDir.file("eng.traineddata"),
        tessdataDir.file("ara.traineddata"),
        fontDir.file("alexandria_variable.ttf")
    )
    doLast {
        val models = mapOf(
            "eng" to "https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata",
            "ara" to "https://github.com/tesseract-ocr/tessdata_fast/raw/main/ara.traineddata"
        )
        tessdataDir.asFile.mkdirs()
        models.forEach { (language, source) ->
            val destination = tessdataDir.asFile.resolve("$language.traineddata")
            if (!destination.exists() || destination.length() < 100_000L) {
                logger.lifecycle("Downloading Tesseract language data: $language")
                val temporary = destination.resolveSibling(destination.name + ".part")
                URL(source).openStream().use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                check(temporary.length() > 100_000L) { "Downloaded OCR model $language is incomplete" }
                check(temporary.renameTo(destination)) { "Could not install OCR model $language" }
            }
        }

        fontDir.asFile.mkdirs()
        val font = fontDir.asFile.resolve("alexandria_variable.ttf")
        if (!font.exists() || font.length() < 100_000L) {
            logger.lifecycle("Downloading Alexandria Arabic font")
            val temporary = font.resolveSibling(font.name + ".part")
            URL("https://github.com/google/fonts/raw/main/ofl/alexandria/Alexandria%5Bwght%5D.ttf")
                .openStream().use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
            check(temporary.length() > 100_000L) { "Downloaded Alexandria font is incomplete" }
            check(temporary.renameTo(font)) { "Could not install Alexandria font" }
        }
    }
}
tasks.named("preBuild").configure { dependsOn("downloadAppAssets") }
