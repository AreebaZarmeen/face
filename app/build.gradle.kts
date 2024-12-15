plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}
//
android {
    namespace = "com.example.face"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.face"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
//    kapt {
//        correctErrorTypes = true
//    }
//
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
   kotlinOptions {
        jvmTarget = "1.8"
    }
}
//
dependencies {
//
   implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
//
//
implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.common)
   // implementation(libs.androidx.camera.core)
    //implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.play.services.mlkit.face.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation (libs.mlkit.face.detection)

        val cameraxVersion = "1.3.0-alpha04"
        implementation(libs.androidx.camera.core.v130alpha04)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle.v130alpha04)
        implementation(libs.androidx.camera.view.v130alpha04)

//
//        val room_version = "2.5.0"
//        implementation ("androidx.room:room-runtime:$room_version")
//        kapt ("androidx.room:room-compiler:$room_version")
//        implementation ("androidx.room:room-ktx:$room_version")
//
//        implementation ("org.tensorflow:tensorflow-lite:2.12.0")
//        implementation (libs.tensorflow.lite.support)
//
}
