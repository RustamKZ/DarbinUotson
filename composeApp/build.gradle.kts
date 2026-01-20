import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation("io.insert-koin:koin-core:3.5.0")
            implementation("io.insert-koin:koin-compose:1.1.0")
            implementation(
                "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
            )
            implementation("org.apache.commons:commons-math3:3.6.1")
            implementation("co.touchlab:kermit:2.0.4")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("com.github.servicenow:stl-decomp-4j:1.0.5")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.example.project_dw.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)

            packageName = "medicaldataanalyzer"
            packageVersion = "1.0.0"
            description = "Medical Data Statistical Analysis Tool"
            vendor = "Your Name"

            modules("java.sql")

            linux {
                appCategory = "Science"
            }

            windows {
                menuGroup = "Medical Data Analyzer"
                upgradeUuid = "bf29db47-eaf3-417b-97a3-279f1a1f684c"
            }
        }
    }
}

afterEvaluate {
    tasks.matching {
        it.name.contains("package") ||
        it.name.contains("createDistributable")
    }.configureEach {
        doLast {
            val pythonSrc = layout.projectDirectory.dir(
                "../python_runtime"
            ).asFile
            val binariesDir = layout.buildDirectory.dir(
                "compose/binaries"
            ).get().asFile

            binariesDir.listFiles()?.forEach { buildType ->
                val appDir = File(buildType, "app/medicaldataanalyzer")
                if (appDir.exists()) {
                    val pythonDst = File(appDir, "python_runtime")

                    println("=== Copying Python runtime ===")
                    println("From: ${pythonSrc.absolutePath}")
                    println("To: ${pythonDst.absolutePath}")

                    pythonDst.deleteRecursively()
                    pythonSrc.copyRecursively(
                        pythonDst,
                        overwrite = true
                    )

                    // Linux executable
                    val linuxEngine = File(
                        pythonDst,
                        "linux/stats_engine"
                    )
                    if (linuxEngine.exists()) {
                        linuxEngine.setExecutable(true)
                    }

                    println("=== Python runtime copied! ===")
                }
        }
    }
}
}