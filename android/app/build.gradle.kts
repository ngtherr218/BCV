import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localDevProperties = Properties().apply {
    val file = rootProject.file("dev.properties")
    if (file.isFile) file.inputStream().use(::load)
}

val deployedApiUrl = "https://bcv-admin-test.onrender.com/"

fun previewCredential(apiUrl: String, key: String): String {
    val credentialApi = localDevProperties.getProperty("bcvPreviewLoginApiBaseUrl") ?: "http://127.0.0.1:5081/"
    return if (apiUrl.trimEnd('/') == credentialApi.trimEnd('/')) localDevProperties.getProperty(key).orEmpty() else ""
}

fun quotedBuildValue(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r") + "\""


android {
    namespace = "com.project.bcv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.project.bcv"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PREVIEW_LOGIN_EMAIL", "\"\"")
        buildConfigField("String", "PREVIEW_LOGIN_PASSWORD", "\"\"")
        buildConfigField("boolean", "STEP_TEST_ENABLED", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "STEP_TEST_ENABLED", "true")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            val apiUrl = providers.gradleProperty("bcvApiBaseUrl")
                .orElse(localDevProperties.getProperty("bcvApiBaseUrl") ?: deployedApiUrl)
                .get()
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
            val goongMaptilesKey = providers.gradleProperty("bcvGoongMaptilesKey")
                .orElse(localDevProperties.getProperty("bcvGoongMaptilesKey") ?: "")
                .get()
            buildConfigField("String", "GOONG_MAPTILES_KEY", "\"$goongMaptilesKey\"")
            val goongApiKey = providers.gradleProperty("bcvGoongApiKey")
                .orElse(localDevProperties.getProperty("bcvGoongApiKey") ?: "")
                .get()
            buildConfigField("String", "GOONG_API_KEY", "\"$goongApiKey\"")
            // Local test account from dev.properties, so the login form only needs "Đăng nhập".
            buildConfigField("String", "PREVIEW_LOGIN_EMAIL",
                quotedBuildValue(previewCredential(apiUrl, "bcvPreviewLoginEmail")))
            buildConfigField("String", "PREVIEW_LOGIN_PASSWORD",
                quotedBuildValue(previewCredential(apiUrl, "bcvPreviewLoginPassword")))
        }
        create("preview") {
            initWith(getByName("debug"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            val apiUrl = providers.gradleProperty("bcvApiBaseUrl")
                .orElse(localDevProperties.getProperty("bcvApiBaseUrl") ?: deployedApiUrl).get()
            buildConfigField("String", "PREVIEW_LOGIN_EMAIL",
                quotedBuildValue(previewCredential(apiUrl, "bcvPreviewLoginEmail")))
            buildConfigField("String", "PREVIEW_LOGIN_PASSWORD",
                quotedBuildValue(previewCredential(apiUrl, "bcvPreviewLoginPassword")))
        }
        release {
            val apiUrl = providers.gradleProperty("bcvApiBaseUrl").orElse(deployedApiUrl).get()
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
            val goongMaptilesKey = providers.gradleProperty("bcvGoongMaptilesKey").orElse("").get()
            buildConfigField("String", "GOONG_MAPTILES_KEY", "\"$goongMaptilesKey\"")
            val goongApiKey = providers.gradleProperty("bcvGoongApiKey").orElse("").get()
            buildConfigField("String", "GOONG_API_KEY", "\"$goongApiKey\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
