import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

val versionProvider = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0")
    isIgnoreExitValue = true
}.standardOutput.asText.map { output ->
    val version = output.trim()
    if (version.isNotEmpty() && version.startsWith("v")) {
        version.substring(1)
    } else version.ifEmpty {
        "1.0.0"
    }
}.orElse("1.0.0")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DeepLink"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":deeplink-kmp-domain"))
            // api(): HandleDeepLinkUseCase recibe NavigationCoordinator en su
            // constructor -- quien wirea Koin en el modulo consumidor necesita verlo.
            api(libs.napoli.kmm.base.presentation)
            // implementation(): NavigateToRoute solo se usa dentro del cuerpo de
            // invoke(), nunca en la firma publica de la clase.
            implementation(libs.napoli.kmm.navigation.domain)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "cl.baldomeronapoli.deeplink"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    repositories {
        mavenLocal()

        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir("repo"))
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/elNapoli/kmm-deeplink")
            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            groupId = "cl.baldomeronapoli"
            version = versionProvider.get()
        }
    }
}
