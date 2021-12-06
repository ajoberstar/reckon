pluginManagement {
  plugins {
    id("com.gradle.plugin-publish") version "0.12.0"

    id("org.ajoberstar.grgit") version "4.0.2"
    id("org.ajoberstar.reckon") version "0.12.0"
    id("org.ajoberstar.stutter") version "0.5.1"

    id("com.diffplug.spotless") version "5.5.2"
  }
}

rootProject.name = "reckon"

include("reckon-core")
include("reckon-gradle")
