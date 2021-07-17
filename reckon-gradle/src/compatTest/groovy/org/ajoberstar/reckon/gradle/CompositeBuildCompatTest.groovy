package org.ajoberstar.reckon.gradle

import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Unroll

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

// Composite builds were added in 3.1
@IgnoreIf({ System.properties['compat.gradle.version'] == '3.0' })
class CompositeBuildCompatTest extends Specification {
  @Rule TemporaryFolder tempDir = new TemporaryFolder()
  File project1Dir
  File project2Dir
  File build1File
  File build2File

  def setup() {
    project1Dir = tempDir.newFolder('project1')
    project2Dir = tempDir.newFolder('project2')
    build1File = projectFile(project1Dir, 'build.gradle')
    build2File = projectFile(project2Dir, 'build.gradle')

    def grgit1 = Grgit.init(dir: project1Dir)
    projectFile(project1Dir, 'settings.gradle') << 'rootProject.name = "project1"'
    projectFile(project1Dir, '.gitignore') << '.gradle\nbuild\n'
    build1File << '''\
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  normal = scopeFromProp()
  preRelease = stageFromProp('beta', 'final')
}

task printVersion {
  doLast {
    println "${project.name} version is ${project.version}"
  }
}
'''
    grgit1.add(patterns: ['.'])
    grgit1.commit(message: 'first commit')
    grgit1.tag.add(name: '1.3.0', message: 'stuff')
    grgit1.close()

    def grgit2 = Grgit.init(dir: project2Dir)
    projectFile(project2Dir, 'settings.gradle') << 'rootProject.name = "project2"'
    projectFile(project2Dir, '.gitignore') << '.gradle\nbuild\n'
    build2File << '''\
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  normal = scopeFromProp()
  preRelease = stageFromProp('beta', 'final')
}

task printVersion {
  doLast {
    println "${project.name} version is ${project.version}"
  }
}
'''
    grgit2.add(patterns: ['.'])
    grgit2.commit(message: 'first commit')
    grgit2.tag.add(name: '1.0.0-beta.1', message: 'stuff')
    grgit2.close()
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
