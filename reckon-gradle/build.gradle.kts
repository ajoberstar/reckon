plugins {
  id("org.ajoberstar.defaults.gradle-plugin")
  groovy

  id("org.ajoberstar.stutter")
}

group = "org.ajoberstar.reckon"
description = "Infer a project's version from your Git repository."

mavenCentral {
  developerName.set("Andrew Oberstar")
  developerEmail.set("ajoberstar@gmail.com")
  githubOwner.set("ajoberstar")
  githubRepository.set("reckon")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
}

dependencies {
  // gradle
  compileOnly(gradleApi())

  // reckon
  api(project(":reckon-core"))

  // git
  api("org.ajoberstar.grgit:grgit-gradle:[4.0.0,5.0.0)")
  implementation("org.eclipse.jgit:org.eclipse.jgit:[5.0,6.0)")
  compatTestImplementation("org.ajoberstar.grgit:grgit-core:[4.0.0,5.0.0)")

  // util
  implementation("com.google.guava:guava:latest.release")

  // testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

stutter {
  val java8 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(8))
    }
    gradleVersions {
      compatibleRange("4.0")
    }
  }
  val java11 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    gradleVersions {
      compatibleRange("5.0")
    }
  }
  val java17 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
    gradleVersions {
      compatibleRange("7.3")
    }
  }
}

tasks.named("check") {
  dependsOn(tasks.named("compatTest"))
}

tasks.named<Jar>("jar") {
  manifest {
    attributes.put("Automatic-Module-Name", "org.ajoberstar.reckon.gradle")
  }
}

gradlePlugin {
  plugins {
    create("plugin") {
      id = "org.ajoberstar.reckon"
      displayName = "Reckon Plugin"
      description = "Infer a project's version from your Git repository."
      implementationClass = "org.ajoberstar.reckon.gradle.ReckonPlugin"
    }
  }
}
