import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

/** Optional settings from the git-ignored `local.properties`; see the README. */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}

/** Reads [key] from `local.properties`, falling back to an environment variable of the same name. */
fun setting(key: String): String =
    localProperties.getProperty(key)?.trim().orEmpty()
        .ifEmpty { providers.environmentVariable(key).orNull?.trim().orEmpty() }

fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Debug builds fall back to this documented inspector code so the demo works out of the box. */
val debugInspectorPin = "0000"

android {
    namespace = "com.jumincho.cvpass"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jumincho.cvpass"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "BUSINESS_API_KEY", javaString(setting("BUSINESS_API_KEY")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", javaString(setting("FIREBASE_PROJECT_ID")))
        buildConfigField("String", "FIREBASE_APPLICATION_ID", javaString(setting("FIREBASE_APPLICATION_ID")))
        buildConfigField("String", "FIREBASE_API_KEY", javaString(setting("FIREBASE_API_KEY")))
    }

    buildTypes {
        debug {
            buildConfigField("String", "INSPECTOR_PIN", javaString(setting("INSPECTOR_PIN").ifEmpty { debugInspectorPin }))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            buildConfigField("String", "INSPECTOR_PIN", javaString(setting("INSPECTOR_PIN")))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            // Robolectric renders the screens with the real resources.
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        textReport = true
        textOutput = File("stdout")
        // Versions are pinned on purpose (see gradle/libs.versions.toml); don't report newer releases.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable", "OldTargetApi")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.mlkit.text.recognition.korean)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(testFixtures(project(":core")))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Screen tests run on Robolectric, which needs JUnit 4; the vintage engine runs them on the JUnit Platform.
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testRuntimeOnly(libs.junit.vintage.engine)
}
