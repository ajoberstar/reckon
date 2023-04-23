plugins {
  id("org.ajoberstar.defaults.gradle-plugin")
  id("groovy")

  id("org.ajoberstar.stutter")
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
  // gradle
  compileOnly(gradleApi())

  // reckon
  api(project(":reckon-core"))

  // git
  implementation("org.eclipse.jgit:org.eclipse.jgit:[6.0,7.0[")

  // testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.eclipse.jgit:org.eclipse.jgit:[6.0,7.0[")
  compatTestImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

stutter {
  val java11 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    gradleVersions {
      compatibleRange("7.0")
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
    create("settings") {
      id = "org.ajoberstar.reckon.settings"
      displayName = "Reckon Settings Plugin"
      description = "Infer a build's version from your Git repository."
      implementationClass = "org.ajoberstar.reckon.gradle.ReckonSettingsPlugin"
    }
  }
}
