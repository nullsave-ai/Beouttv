pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
  repositories {
    maven {
      url = uri(rootDir.resolve("repo"))
    }
    google()
    mavenCentral()
  }
}

rootProject.name = "Beouttv"

include(":app")