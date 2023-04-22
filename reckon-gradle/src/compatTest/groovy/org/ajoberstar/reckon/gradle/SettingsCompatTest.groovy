package org.ajoberstar.reckon.gradle

import spock.lang.Specification
import spock.lang.TempDir

import org.ajoberstar.grgit.Grgit
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class SettingsCompatTest extends Specification {
  @TempDir File tempDir
  File projectDir
  File settingsFile
  File buildFile
  Grgit remote
  Grgit remote2

  def setup() {
    projectDir = new File(tempDir, 'project')
    settingsFile = projectFile('settings.gradle')
    buildFile = projectFile('build.gradle')

    def remoteDir = new File(tempDir, 'remote')
    remote = Grgit.init(dir: remoteDir)

    remoteFile('.gitignore') << '.gradle/\nbuild/\n'
    remoteFile('master.txt') << 'contents here'
    remote.add(patterns: ['.'])
    remote.commit(message: 'first commit')
    remote.tag.add(name: '1.0.0', message: '1.0.0')
    remote.tag.add(name: 'project-a/9.0.0', message: '9.0.0')
    remoteFile('master.txt') << 'contents here2'
    remote.add(patterns: ['.'])
    remote.commit(message: 'major: second commit')

    def remote2Dir = new File(tempDir, 'remote2')
    remote2 = Grgit.clone(dir: remote2Dir, uri: remote.repository.rootDir)
  }

  def 'if no git repo found, version is defaulted'() {
    given:
    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}
"""
    buildFile << """
tasks.register('printVersion') {
  doLast  {
    println version
  }
}
"""
    when:
    def result = build('printVersion', '-q', '--no-configuration-cache')
    then:
    // version will end with a timestamp, so don't try to validate the whole thing
    result.output.normalize().startsWith('0.1.0-alpha.0.0+')
  }

  def 'if no strategies specified, build fails'() {
    given:
    Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}
"""
    buildFile << """
tasks.register('printVersion') {
  doLast  {
    println version
  }
}
"""
    when:
    def result = buildAndFail('printVersion', '-q', '--no-configuration-cache')
    then:
    result.output.contains('Must provide a scope supplier.')
  }

  def 'if reckoned version has build metadata no tag created'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  defaultInferredScope = 'patch'
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.0.1-alpha.0')
    result.task(':reckonTagCreate').outcome == TaskOutcome.UP_TO_DATE
    result.task(':reckonTagPush').outcome == TaskOutcome.UP_TO_DATE
  }

  def 'if reckoned version is SNAPSHOT no tag created'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  snapshotFromProp()
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-SNAPSHOT')
    result.task(':reckonTagCreate').outcome == TaskOutcome.UP_TO_DATE
    result.task(':reckonTagPush').outcome == TaskOutcome.UP_TO_DATE
  }

  def 'if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == '1.1.0-alpha.1' }
  }

  def 'can use commit messages for scope and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  stages('alpha','beta', 'final')
  scopeCalc = calcScopeFromProp().or(calcScopeFromCommitMessages())
  stageCalc = calcStageFromProp()
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 2.0.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == '2.0.0-alpha.1' }
  }

  def 'can use commit messages for scope but override with prop and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  stages('alpha','beta', 'final')
  scopeCalc = calcScopeFromProp().or(calcScopeFromCommitMessages())
  stageCalc = calcStageFromProp()
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.scope=patch', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.0.1-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == '1.0.1-alpha.1' }
  }

  def 'remote can be overridden and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  remote = 'other-remote'
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    local.remote.add(name: 'other-remote', url: remote2.getRepository().getRootDir())
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    !remote.tag.list().find { it.name == '1.1.0-alpha.1' }
    remote2.tag.list().find { it.name == '1.1.0-alpha.1' }
  }

  def 'tag parser/writer can be overridden and reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')

  tagParser = tagName -> java.util.Optional.of(tagName)
      .filter(name -> name.startsWith("project-a/"))
      .map(name -> name.replace("project-a/", ""))
      .flatMap(name -> org.ajoberstar.reckon.core.Version.parse(name))
  tagWriter = version -> "project-a/" + version
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 9.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == 'project-a/9.1.0-alpha.1' }
  }

  def 'tag message can be overridden and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  tagMessage = version.map(v -> "Version " + v)
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    remote.tag.list().find { it.name == '1.1.0-alpha.1' && it.shortMessage == 'Version 1.1.0-alpha.1' }
  }

  def 'if reckoned version is rebuild, skip tag create, but push'() {
    given:
    def local = Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)


    settingsFile << """
plugins {
  id 'org.ajoberstar.reckon.settings'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha', 'beta', 'final')
}
"""
    local.add(patterns: ['settings.gradle'])
    local.commit(message: 'Build file')
    local.tag.add(name: '1.1.0', message: '1.1.0')
    when:
    def result = build('reckonTagPush', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0')
    result.task(':reckonTagCreate').outcome == TaskOutcome.UP_TO_DATE
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
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

  private File remote2File(String path) {
    File file = new File(remote2.repository.rootDir, path)
    file.parentFile.mkdirs()
    return file
  }

  private File projectFile(String path) {
    File file = new File(projectDir, path)
    file.parentFile.mkdirs()
    return file
  }
}
