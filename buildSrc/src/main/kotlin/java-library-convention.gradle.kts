plugins {
  `java-library`
  `maven-publish`
  `signing`
  id("locking-convention")
  id("spotless-convention")
}

group = "org.ajoberstar.reckon"
description = "Infer a project's version from your Git repository."

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  withSourcesJar()
  withJavadocJar()
}

publishing {
  repositories {
    maven {
      name = "CentralReleases"
      url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      credentials {
        username = System.getenv("OSSRH_USERNAME")
        password = System.getenv("OSSRH_PASSWORD")
      }
    }

    maven {
      name = "CentralSnapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      credentials {
        username = System.getenv("OSSRH_USERNAME")
        password = System.getenv("OSSRH_PASSWORD")
      }
    }
  }

  publications {
    create<MavenPublication>("main") {
      from(components["java"])

      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }

      pom {
        name.set("Reckon")
        description.set("Infer a project's version from your Git repository.")
        url.set("https://github.com/ajoberstar/reckon")

        developers {
          developer {
            name.set("Andrew Oberstar")
            email.set("ajoberstar@gmail.com")
          }
        }

        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0")
          }
        }

        scm {
          url.set("https://github.com/ajoberstar/reckon")
          connection.set("scm:git:git@github.com:ajoberstar/reckon.git")
          developerConnection.set("scm:git:ssh:git@github.com:ajoberstar/reckon.git")
        }
      }
    }
  }
}

signing {
  setRequired(System.getenv("CI"))
  val signingKey: String? by project
  val signingPassphrase: String? by project
  useInMemoryPgpKeys(signingKey, signingPassphrase)
  sign(publishing.publications)
}
