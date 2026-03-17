plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yourname.videoeditor.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation("androidx.core:core-ktx:1.12.0")
}