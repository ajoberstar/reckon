pluginManagement {
  plugins {
    id("org.ajoberstar.defaults.java-library") version "0.20.0"
    id("org.ajoberstar.defaults.gradle-plugin") version "0.20.0"

    id("org.ajoberstar.reckon.settings") version "1.0.0"
    id("org.ajoberstar.stutter") version "2.0.1"

    id("com.diffplug.spotless") version "7.2.1"
  }

  repositories {
    mavenCentral()
  }
}

plugins {
  id("org.ajoberstar.reckon.settings")
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
  setDefaultInferredScope("patch")
  stages("beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
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
