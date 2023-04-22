package org.ajoberstar.reckon.gradle

import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class BaseCompatTest extends Specification {
  @TempDir File tempDir
  File projectDir
  File buildFile
  Git remote
  Git remote2

  def setup() {
    projectDir = new File(tempDir, 'project')
    buildFile = projectFile('build.gradle')

    def remoteDir = new File(tempDir, 'remote')
    remote = Git.init().setDirectory(remoteDir).call()

    Gits.repoFile(remote, '.gitignore') << '.gradle/\nbuild/\n'
    Gits.repoFile(remote, 'master.txt') << 'contents here'
    Gits.commitAll(remote, 'first commit')
    Gits.tag(remote, '1.0.0')
    Gits.tag(remote, 'project-a/9.0.0')
    Gits.repoFile(remote, 'master.txt') << 'contents here2'
    Gits.commitAll(remote, 'major: second commit')

    def remote2Dir = new File(tempDir, 'remote2')
    remote2 = Gits.clone(remote2Dir, remote)
  }

  def 'if no git repo found, version is defaulted'() {
    given:
    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}

task printVersion {
  doLast  {
    println reckon.version.get()
  }
}
"""
    when:
    def result = build('printVersion', '-q', '--no-configuration-cache')
    then:
    // version will end with a timestamp, so don't try to validate the whole thing
    result.output.normalize().startsWith('0.1.0-alpha.0.0+')
  }

  def 'if no strategies specified, version is unspecified'() {
    given:
    Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

task printVersion {
  doLast  {
    println version
  }
}
"""
    when:
    def result = build('printVersion', '-q', '--no-configuration-cache')
    then:
    result.output.contains('unspecified')
  }

  def 'if version evaluated before reckon configured, reckon can still be evaluated after'() {
    given:
    Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

println version

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}

task printVersion {
  doLast  {
    println version
  }
}
"""
    when:
    def result = build('printVersion', '--no-configuration-cache')
    then:
    result.output.contains('unspecified')
    result.output.contains('Reckoned version: 1.1.0-alpha.0')
  }

  def 'if reckoned version has build metadata no tag created'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  defaultInferredScope = 'patch'
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.0.1-alpha.0')
    result.task(':reckonTagCreate').outcome == TaskOutcome.UP_TO_DATE
    result.task(':reckonTagPush').outcome == TaskOutcome.UP_TO_DATE
  }

  def 'if reckoned version is SNAPSHOT no tag created'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  snapshotFromProp()
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-SNAPSHOT')
    result.task(':reckonTagCreate').outcome == TaskOutcome.UP_TO_DATE
    result.task(':reckonTagPush').outcome == TaskOutcome.UP_TO_DATE
  }

  def 'if reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    Gits.hasTag(remote, '1.1.0-alpha.1')
  }

  def 'can use commit messages for scope and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  stages('alpha','beta', 'final')
  scopeCalc = calcScopeFromProp().or(calcScopeFromCommitMessages())
  stageCalc = calcStageFromProp()
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 2.0.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    Gits.hasTag(remote, '2.0.0-alpha.1')
  }

  def 'can use commit messages for scope but override with prop and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  stages('alpha','beta', 'final')
  scopeCalc = calcScopeFromProp().or(calcScopeFromCommitMessages())
  stageCalc = calcStageFromProp()
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '-Preckon.scope=patch', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.0.1-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    Gits.hasTag(remote, '1.0.1-alpha.1')
  }

  def 'remote can be overridden and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  remote = 'other-remote'
}
"""
    Gits.commitAll(local)
    Gits.remoteAdd(local, 'other-remote', remote2.getRepository().getDirectory())
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    !Gits.hasTag(remote, '1.1.0-alpha.1')
    Gits.hasTag(remote2, '1.1.0-alpha.1')
  }

  def 'tag parser/writer can be overridden and reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  
  tagParser = tagName -> java.util.Optional.of(tagName)
      .filter(name -> name.startsWith("project-a/"))
      .map(name -> name.replace("project-a/", ""))
      .flatMap(name -> org.ajoberstar.reckon.core.Version.parse(name))
  tagWriter = version -> "project-a/" + version
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 9.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    Gits.hasTag(remote, 'project-a/9.1.0-alpha.1')
  }

  def 'tag message can be overridden and if reckoned version is significant tag created and pushed'() {
    given:
    def local = Gits.clone(projectDir, remote)

    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha','beta', 'final')
  tagMessage = version.map(v -> "Version " + v)
}
"""
    Gits.commitAll(local)
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha', '--configuration-cache')
    then:
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    and:
    Gits.hasTag(remote, '1.1.0-alpha.1')
    // TODO also test message
  }

  def 'if reckoned version is rebuild, skip tag create, but push'() {
    given:
    def local = Gits.clone(projectDir, remote)


    buildFile << """
plugins {
  id 'org.ajoberstar.reckon'
}

reckon {
  defaultInferredScope = 'minor'
  scopeFromProp()
  stageFromProp('alpha', 'beta', 'final')
}
"""
    Gits.commitAll(local)
    Gits.tag(local, '1.1.0')
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

  private File projectFile(String path) {
    File file = new File(projectDir, path)
    file.parentFile.mkdirs()
    return file
  }
}
