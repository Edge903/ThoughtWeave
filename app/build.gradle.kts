plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "io.wit117.thoughtweave"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.wit117.thoughtweave"
        minSdk = 24
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time on minSdk 24
        isCoreLibraryDesugaringEnabled = true
    }
    assetPacks.add(":gemma_pack")
    androidResources {
        // Lists the app's languages in Android 13+ system settings (default locale: res/resources.properties).
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // ViewModel tests construct android.database.sqlite.SQLiteException from the android.jar stubs.
        unitTests.isReturnDefaultValues = true
    }
}

// Release guard: a store bundle must not ship the unreviewed agreement.
val agreementDraftMarker = "STATUS: DRAFT"
val agreementFiles = listOf("agreement_en.txt", "agreement_ko.txt")
    .map { layout.projectDirectory.file("src/main/assets/legal/$it").asFile }
val allowDraftLegal = providers.gradleProperty("allowDraftLegal").orNull == "true"
val verifyAgreementNotDraft = tasks.register("verifyAgreementNotDraft") {
    description = "Fails while the user agreement is still marked as a draft."
    doLast {
        val drafts = agreementFiles.filter { it.readText().contains(agreementDraftMarker) }
        if (drafts.isNotEmpty() && !allowDraftLegal) {
            throw GradleException(
                "The user agreement is a DRAFT that requires legal review: " +
                    drafts.joinToString { it.name } +
                    ". Use -PallowDraftLegal=true for internal testing only.",
            )
        }
    }
}

// The model is gitignored, so a bundle built without it would silently ship no AI model.
val packedModel = rootDir.resolve("gemma_pack/src/main/assets/gemma3-1b-it-int4.litertlm")
val verifyModelInPack = tasks.register("verifyModelInPack") {
    description = "Fails if the AI pack does not contain the expected model file."
    doLast {
        if (packedModel.length() != 584_417_280L) {
            throw GradleException("Expected the 584,417,280-byte model at $packedModel")
        }
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyAgreementNotDraft, verifyModelInPack)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.ai.delivery)
    implementation(libs.litertlm.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
