pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VideoEditor"
include(":app")
include(":core:data")
include(":core:domain")
include(":core:presentation")
include(":feature:gallery")
include(":feature:editor")
include(":library:rendering")