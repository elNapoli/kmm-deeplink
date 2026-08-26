rootProject.name = "NapoliKmmDeepLink"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()

        maven {
            name = "GitHubPackagesBase"
            url = uri("https://maven.pkg.github.com/elNapoli/kmm-base")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("PAT_READ_PACKAGES") ?: System.getenv("GITHUB_TOKEN")
            }
        }

        maven {
            name = "GitHubPackagesNavigation"
            url = uri("https://maven.pkg.github.com/elNapoli/kmm-navigation")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("PAT_READ_PACKAGES") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

include(":deeplink-kmp")
