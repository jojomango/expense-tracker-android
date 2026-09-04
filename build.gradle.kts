import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

tasks.register("domainPurityCheck") {
    group = "verification"
    description = "Fails if app/src/main/kotlin/.../domain contains any android.*/androidx.* import"
    doLast {
        val script = file("scripts/check-domain-purity.sh")
        val result =
            project.exec {
                commandLine("bash", script.absolutePath)
                isIgnoreExitValue = true
            }
        if (result.exitValue != 0) {
            throw GradleException("domain purity check failed — see output above")
        }
    }
}

tasks.register("verify") {
    group = "verification"
    description = "Aggregate: domain purity -> ktlint -> detekt -> unit tests + coverage -> assembleDebug"
    dependsOn(
        ":domainPurityCheck",
        ":app:ktlintCheck",
        ":app:detekt",
        ":app:testDebugUnitTest",
        ":app:jacocoTestReport",
        ":app:assembleDebug",
    )
    tasks.findByPath(":app:ktlintCheck")?.mustRunAfter(":domainPurityCheck")
    tasks.findByPath(":app:detekt")?.mustRunAfter(":app:ktlintCheck")
    tasks.findByPath(":app:testDebugUnitTest")?.mustRunAfter(":app:detekt")
    tasks.findByPath(":app:jacocoTestReport")?.mustRunAfter(":app:testDebugUnitTest")
    tasks.findByPath(":app:assembleDebug")?.mustRunAfter(":app:jacocoTestReport")
}

subprojects {
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
    }
}
