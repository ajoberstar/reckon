plugins {
  id("java-library-convention")
  id("java-gradle-plugin")
  groovy

  id("com.gradle.plugin-publish")
  id("org.ajoberstar.stutter")
}

dependencies {
  // gradle
  compileOnly(gradleApi())

  // reckon
  api(project(":reckon-core"))

  // git
  api("org.ajoberstar.grgit:grgit-core:[4.0.0,5.0.0)")
  implementation("org.eclipse.jgit:org.eclipse.jgit:[5.0,6.0)")
  compatTestImplementation("org.ajoberstar.grgit:grgit-core:[4.0.0,5.0.0)")

  // util
  implementation("com.google.guava:guava:latest.release")

  // testing
  compatTestImplementation(gradleTestKit())
  compatTestImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
  compatTestImplementation("org.codehaus.groovy:groovy-all:[2.5,2.6-alpha)")
  compatTestImplementation("junit:junit:latest.release")
}

stutter {
  setSparse(true)

  java(8) {
    compatibleRange("6.1")
  }
  java(11) {
    compatibleRange("6.1")
  }
  java(15) {
    compatibleRange("6.3")
  }
}

tasks.named<Jar>("jar") {
  manifest {
    attributes.put("Automatic-Module-Name", "org.ajoberstar.reckon.gradle")
  }
}

pluginBundle {
  website = "https://github.com/ajoberstar/reckon"
  vcsUrl = "https://github.com/ajoberstar/reckon.git"
  description = "Infer a project\"s version from your Git repository"
  plugins {
    create("publishPlugin") {
      id = "org.ajoberstar.reckon"
      displayName = "Reckon Plugin"
      tags = listOf("semver", "git", "version", "versioning")
    }
  }
  mavenCoordinates {
    groupId = project.group as String
    artifactId = project.name as String
    version = project.version.toString()
  }
}

// remove duplicate publication
gradlePlugin {
  setAutomatedPublishing(false)
}
