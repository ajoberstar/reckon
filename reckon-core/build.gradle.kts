plugins {
  id("org.ajoberstar.defaults.java-library")
}

group = "org.ajoberstar.reckon"
description = "Infer a project's version from your Git repository."

mavenCentral {
  developerName.set("Andrew Oberstar")
  developerEmail.set("andrew@ajoberstar.org")
  githubOwner.set("ajoberstar")
  githubRepository.set("reckon")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

dependencies {
  // logging
  implementation("org.slf4j:slf4j-api:[1.7.25,1.8.0[")

  // git
  api("org.eclipse.jgit:org.eclipse.jgit:[6.0,7.0[")

  // util
  implementation("org.apache.commons:commons-lang3:[3.5,4.0[")
  implementation("com.github.zafarkhaja:java-semver:[0.9,)")
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter("latest.release")
      dependencies {
        implementation("org.junit.jupiter:junit-jupiter-params")
        implementation("org.mockito:mockito-core:latest.release")
        implementation("org.ajoberstar.grgit:grgit-core:[5.0,6.0[")
        runtimeOnly("org.slf4j:slf4j-simple:[1.7.25,1.8.0[")
      }
    }
  }
}

tasks.named<Jar>("jar") {
  manifest {
    attributes.put("Automatic-Module-Name", "org.ajoberstar.reckon.core")
  }
}
