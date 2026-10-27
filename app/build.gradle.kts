import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    id("jacoco")
}

// Firebase 設定檔（google-services.json）不進 repo：裡面的 API key 雖然不是祕密，但 repo 是公開的，
// 被拿去灌假事件會汙染分析資料。CI 從 GitHub Secret 還原。沒有這個檔時不套用 Firebase 的兩個外掛，
// App 照常組得起來，執行期 Firebase 不會初始化，分析一律走 no-op（見 `FirebaseAnalyticsSink`）。
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.jojomango.expensetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jojomango.expensetracker"
        minSdk = 26
        targetSdk = 36
        // 內部測試版由 scripts/release-internal.sh 傳入 -PexpenseVersionCode=<yyMMddHH>，每次上傳都比上一次大，
        // 不用為了出測試版改這個檔案。平常開發與 CI 沒有傳，就用下面這個數字。
        versionCode = providers.gradleProperty("expenseVersionCode").orNull?.toIntOrNull() ?: 3
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 上傳金鑰（upload key）與密碼**絕對不進 repo**：從 ~/.gradle/gradle.properties 或環境變數讀
    // （CI 從 GitHub Secret 還原）。四個值都沒設定時 release 退回用 debug 金鑰簽，只是為了能在
    // emulator 上實測 R8 版本——Play Console 會直接拒收 debug 簽章的檔案，不會被誤傳上去。
    val releaseSigning =
        listOf("EXPENSE_UPLOAD_STORE_FILE", "EXPENSE_UPLOAD_STORE_PASSWORD", "EXPENSE_UPLOAD_KEY_ALIAS", "EXPENSE_UPLOAD_KEY_PASSWORD")
            .associateWith { key -> providers.gradleProperty(key).orElse(providers.environmentVariable(key)).orNull }
    val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }

    // 用正式金鑰簽的版本是要上傳到 Play 的：少了 Firebase 設定檔的話，使用者打開「協助改善 App」
    // 也什麼都收不到，而且不會有任何錯誤提示——寧可在組建時就擋下來。只擋 release 的 task，
    // debug 組建與測試照常可以跑。
    if (hasReleaseSigning && !hasFirebaseConfig) {
        tasks.matching { it.name == "preReleaseBuild" }.configureEach {
            doFirst {
                throw GradleException("已設定上傳金鑰，但找不到 app/google-services.json——上架版本必須包含 Firebase 設定檔")
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigning.getValue("EXPENSE_UPLOAD_STORE_FILE")!!) // hasReleaseSigning 已確認四個值都非空
                storePassword = releaseSigning.getValue("EXPENSE_UPLOAD_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("EXPENSE_UPLOAD_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("EXPENSE_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
    }

    // Room 的 schema JSON 進版控：MigrationTestHelper 要靠舊版本的 schema 建出舊資料庫來測遷移（T10.3）。
    // Robolectric 讀的是 debug 變體合併後的 assets（test 原始碼集的 assets 不會給它），所以掛在 debug；
    // 只有 debug APK 多幾 KB 的 JSON，release 不受影響。
    sourceSets {
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime.ktx)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // SPEC.md §5 v2：只有這三個會連網的套件（Analytics、Crashlytics、Play Billing）。
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.billing)

    testImplementation(libs.junit5.jupiter.api)
    testImplementation(libs.junit5.jupiter.params)
    testRuntimeOnly(libs.junit5.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Robolectric 跑 Room persistence 測試（TESTCASES.md 原本要的 data/src/test/
    // JVM 分層）——Robolectric 本身只支援 JUnit4，用 junit-vintage-engine 讓它跟
    // 其他 JUnit5 domain 測試在同一個 testDebugUnitTest task 裡共存。
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room.testing)
    testRuntimeOnly(libs.junit5.vintage.engine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ktlint {
    version.set("1.3.1")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates a JaCoCo report for the domain package and enforces the 90% coverage gate."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*_Hilt*.*",
            "**/Hilt_*.*",
            "**/*_Factory.*",
            "**/*_MembersInjector.*",
            "**/dagger/**",
            "**/hilt_aggregated_deps/**",
        )

    // Only the domain package is measured against the 90% gate — see SPEC.md P5.
    val domainClasses =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug/com/jojomango/expensetracker/domain") {
            exclude(fileFilter)
        }
    classDirectories.setFrom(domainClasses)
    sourceDirectories.setFrom(files("src/main/kotlin/com/jojomango/expensetracker/domain"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    val domainClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug/com/jojomango/expensetracker/domain")
    classDirectories.setFrom(domainClasses)
    sourceDirectories.setFrom(files("src/main/kotlin/com/jojomango/expensetracker/domain"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )

    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// domain/ now has real logic (Money/Week/Month, Phase 1) with tests, so the
// 90% coverage gate is enforced from here on.
tasks.named("jacocoTestReport") {
    finalizedBy("jacocoCoverageVerification")
}
