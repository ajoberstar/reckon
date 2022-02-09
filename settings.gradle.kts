pluginManagement {
  plugins {
    id("org.ajoberstar.defaults.java-library") version "0.17.1"
    id("org.ajoberstar.defaults.gradle-plugin") version "0.17.1"

    id("org.ajoberstar.grgit") version "4.1.1"
    id("org.ajoberstar.reckon") version "0.13.1"
    id("org.ajoberstar.stutter") version "0.7.0"

    id("com.diffplug.spotless") version "6.2.1"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "reckon"

include("reckon-core")
include("reckon-gradle")
