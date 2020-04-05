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

class BaseCompatTest extends Specification {
  @Rule TemporaryFolder tempDir = new TemporaryFolder()
  File projectDir
  File buildFile
  Grgit remote

  def setup() {
    projectDir = tempDir.newFolder('project')
    buildFile = projectFile('build.gradle')

    def remoteDir = tempDir.newFolder('remote')
    remote = Grgit.init(dir: remoteDir)

    remoteFile('.gitignore') << '.gradle/\nbuild/\n'
    remoteFile('master.txt') << 'contents here'
    remote.add(patterns: ['.'])
    remote.commit(message: 'first commit')
    remote.tag.add(name: '1.0.0', message: '1.0.0')
    remoteFile('master.txt') << 'contents here2'
    remote.add(patterns: ['.'])
    remote.commit(message: 'second commit')
  }

  def 'if no git repo found, version is defaulted'() {
    given:
    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}

task printVersion {
  doLast  {
    println project.version
  }
}
"""
    when:
    def result = build('printVersion', '-q')
    then:
    // version will end with a timestamp, so don't try to validate the whole thing
    result.output.normalize().startsWith('0.1.0-alpha.0.0+')
  }

  def 'if no strategies specified, build fails'() {
    given:
    Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

task printVersion {
  doLast  {
    println project.version
  }
}
"""
    when:
    def result = buildAndFail('printVersion')
    then:
    result.output.contains('Must provide a scope supplier.')
  }

  def 'if reckoned version has build metadata no tag created'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}
"""
    local.add(patterns: ['build.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.0')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SKIPPED
    result.task(':reckonTagPush').outcome == TaskOutcome.SKIPPED
  }

  def 'if reckoned version is SNAPSHOT no tag created'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  snapshotFromProp()
}
"""
    local.add(patterns: ['build.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush')
    then:
    result.output.contains('Reckoned version: 1.1.0-SNAPSHOT')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SKIPPED
    result.task(':reckonTagPush').outcome == TaskOutcome.SKIPPED
  }

  def 'if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}
"""
    local.add(patterns: ['build.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == '1.1.0-alpha.1' }
  }

  def 'if reckoned version is rebuild, skip tag and push'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)


    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha', 'beta', 'final')
}
"""
    local.add(patterns: ['build.gradle'])
    local.commit(message: 'Build file')
    local.tag.add(name: '1.1.0', message: '1.1.0')
    when:
    def result = build('reckonTagPush')
    then:
    result.output.contains('Reckoned version: 1.1.0')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SKIPPED
    result.task(':reckonTagPush').outcome == TaskOutcome.SKIPPED
  }

  def 'old syntax of extension does not fail'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  normal = scopeFromProp()
  preRelease = stageFromProp('alpha','beta', 'final')
}

task printVersion {
  doLast  {
    println project.version
  }
}
"""
    local.add(patterns: ['build.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('printVersion')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.0')
  }

  @IgnoreIf({ System.properties['compat.gradle.version'].toString().startsWith("4.") })
  def 'Kotlin build script can use isFinal'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)
    File kotlinBuildFile = projectFile('build.gradle.kts')

    kotlinBuildFile << """
plugins {
  id("org.ajoberstar.reckon")
}

reckon {
  scopeFromProp()
  stageFromProp("alpha", "beta", "final")
}

tasks.register("printIsFinal") {
  doLast  {
    val version = project.version as org.ajoberstar.reckon.gradle.VersionProvider
    print("Is Final: \${version.version.isFinal()}")
  }
}
"""
    local.add(patterns: ['build.gradle.kts'])
    local.commit(message: 'Build file')
    when:
    def result = build('printIsFinal')
    then:
    result.output.contains('Is Final: false')
  }

  private BuildResult build(String... args = []) {
    return GradleRunner.create()
      .withGradleVersion(System.properties['compat.gradle.version'])
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .forwardOutput()
      .withArguments((args + '--stacktrace') as String[])
      .build()
  }

  private BuildResult buildAndFail(String... args = []) {
    return GradleRunner.create()
      .withGradleVersion(System.properties['compat.gradle.version'])
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .forwardOutput()
      .withArguments((args + '--stacktrace') as String[])
      .buildAndFail()
  }

  private File remoteFile(String path) {
    File file = new File(remote.repository.rootDir, path)
    file.parentFile.mkdirs()
    return file
  }

  private File projectFile(String path) {
    File file = new File(projectDir, path)
    file.parentFile.mkdirs()
    return file
  }
}
