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
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }
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
    implementation("com.rmtheis:tess-two:9.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// The APK bundles both OCR language models. They are downloaded at build time,
// so the installed app can perform OCR fully offline.
val tessdataDir = layout.projectDirectory.dir("src/main/assets/tessdata")
tasks.register("downloadTessdata") {
    outputs.files(
        tessdataDir.file("eng.traineddata"),
        tessdataDir.file("ara.traineddata")
    )
    doLast {
        val dir = tessdataDir.asFile
        dir.mkdirs()
        mapOf(
            "eng" to "https://github.com/tesseract-ocr/tessdata_fast/raw/4.1.0/eng.traineddata",
            "ara" to "https://github.com/tesseract-ocr/tessdata_fast/raw/4.1.0/ara.traineddata"
        ).forEach { (language, source) ->
            val destination = dir.resolve("$language.traineddata")
            if (!destination.exists() || destination.length() < 100_000L) {
                logger.lifecycle("Downloading Tesseract language data: $language")
                URL(source).openStream().use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                check(destination.length() > 100_000L) {
                    "Downloaded Tesseract model $language looks incomplete"
                }
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn("downloadTessdata") }
