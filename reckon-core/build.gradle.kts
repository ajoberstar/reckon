plugins {
  id("org.ajoberstar.defaults.java-library")
  groovy
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
  // logging
  implementation("org.slf4j:slf4j-api:[1.7.25,1.8.0-alpha)") // wait until final 1.8.0 is out to upgrade
  testRuntimeOnly("org.slf4j:slf4j-simple:[1.7.25,1.8.0-alpha)")

  // git
  api("org.eclipse.jgit:org.eclipse.jgit:[5.0,6.0)")
  testImplementation("org.ajoberstar.grgit:grgit-core:[4.0.0,5.0.0)")

  // util
  implementation("org.apache.commons:commons-lang3:[3.5,4.0)")
  implementation("com.github.zafarkhaja:java-semver:[0.9.0,)")

  // testing
  testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
  testImplementation("org.codehaus.groovy:groovy-all:[2.5,2.6-alpha)")
}

tasks.named<Jar>("jar") {
  manifest {
    attributes.put("Automatic-Module-Name", "org.ajoberstar.reckon.core")
  }
}
