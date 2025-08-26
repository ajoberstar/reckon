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
}

testing {
  suites {
    val compatTest by getting(JvmTestSuite::class) {
      useSpock("2.3-groovy-4.0")

      dependencies {
        implementation(gradleTestKit())
        implementation("org.eclipse.jgit:org.eclipse.jgit:[6.0,7.0[")
      }
    }
  }
}

stutter {
  val java11 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
    }
    gradleVersions {
      compatibleRange("7.0", "9.0")
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
  val java21 by matrices.creating {
    javaToolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
    gradleVersions {
      compatibleRange("8.4")
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
