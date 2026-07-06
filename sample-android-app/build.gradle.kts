plugins {
    id("com.android.library") version "9.2.1"
    id("io.github.dreamofloser.android-testgen")
}

android {
    namespace = "com.example.androidapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

testGen {
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    testOutputDir.set(layout.projectDirectory.dir("src/test/java"))
    reportOutputDir.set(layout.buildDirectory.dir("reports/testgen"))
    packageIncludes.set(listOf("com.example.androidapp"))
    minimumQualityScore.set(80)
    expectedTestTaskName.set(":sample-android-app:testDebugUnitTest")
}




