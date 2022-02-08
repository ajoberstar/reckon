pluginManagement {
  plugins {
    id("com.gradle.plugin-publish") version "0.20.0"

    id("org.ajoberstar.grgit") version "4.1.1"
    id("org.ajoberstar.reckon") version "0.13.0"
    id("org.ajoberstar.stutter") version "0.6.0"

    id("com.diffplug.spotless") version "6.0.0"
  }
}

rootProject.name = "reckon"

include("reckon-core")
include("reckon-gradle")
