plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.parentapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.parentapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        val pocHost = providers.gradleProperty("pocHost").orElse("192.168.1.49").get()
        val mqttUsername = providers.gradleProperty("pocMqttUsername").orElse("parent-poc").get()
        val mqttPassword = providers.gradleProperty("pocMqttPassword").orElse("").get()
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
        buildConfigField("String", "POC_HOST", literal(pocHost))
        buildConfigField("String", "MQTT_USERNAME", literal(mqttUsername))
        buildConfigField("String", "MQTT_PASSWORD", literal(mqttPassword))

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")



    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    //Datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

}
