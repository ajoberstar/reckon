package org.ajoberstar.reckon.gradle


import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

class CompositeBuildCompatTest extends Specification {
  @TempDir File tempDir
  File project1Dir
  File project2Dir
  File build1File
  File build2File

  def setup() {
    project1Dir = new File(tempDir, 'project1')
    project2Dir = new File(tempDir, 'project2')
    build1File = projectFile(project1Dir, 'build.gradle')
    build2File = projectFile(project2Dir, 'build.gradle')

    def git1 = Git.init().setDirectory(project1Dir).call()
    projectFile(project1Dir, 'settings.gradle') << 'rootProject.name = "project1"'
    projectFile(project1Dir, '.gitignore') << '.gradle\nbuild\n'
    build1File << '''\
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('beta', 'final')
}

task printVersion {
  doLast {
    println "${project.name} version is ${project.version}"
  }
}
'''
    Gits.commitAll(git1, 'first commit')
    Gits.tag(git1, '1.3.0')
    git1.close()

    def git2 = Git.init().setDirectory(project2Dir).call();
    projectFile(project2Dir, 'settings.gradle') << 'rootProject.name = "project2"'
    projectFile(project2Dir, '.gitignore') << '.gradle\nbuild\n'
    build2File << '''\
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('beta', 'final')
}

task printVersion {
  doLast {
    println "${project.name} version is ${project.version}"
  }
}
'''
    Gits.commitAll(git2, 'first commit')
    Gits.tag(git2, '1.0.0-beta.1')
    git2.close()
  }

  def 'if build included in composite build, reckon properties are ignored'() {
    when:
    def result = build(project2Dir, 'printVersion', '--include-build', project1Dir.absolutePath, '-Preckon.scope=major', '-Preckon.stage=beta')
    then:
    result.output.contains('Reckoned version: 1.3.0')
    result.output.contains('Reckoned version: 1.0.0-beta.2')
  }

  private BuildResult build(File projectDir, String... args = []) {
    return GradleRunner.create()
      .withGradleVersion(System.properties['compat.gradle.version'])
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .forwardOutput()
      .withArguments((args + '--stacktrace') as String[])
      .build()
  }

  private BuildResult buildAndFail(File projectDir, String... args = []) {
    return GradleRunner.create()
      .withGradleVersion(System.properties['compat.gradle.version'])
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .forwardOutput()
      .withArguments((args + '--stacktrace') as String[])
      .buildAndFail()
  }

  private File projectFile(File projectDir, String path) {
    File file = new File(projectDir, path)
    file.parentFile.mkdirs()
    return file
  }
}
