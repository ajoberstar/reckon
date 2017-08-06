/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.gradle.reckon.gradle

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

    remoteFile('master.txt') << 'contents here'
    remote.add(patterns: ['.'])
    remote.commit(message: 'first commit')
    remote.tag.add(name: '1.0.0', message: '1.0.0')
    remoteFile('master.txt') << 'contents here2'
    remote.add(patterns: ['.'])
    remote.commit(message: 'second commit')
  }

  def 'if no git repo found, version is unspecified'() {
    given:
    buildFile << """
plugins {
  id 'org.ajoberstar.grgit'
  id 'org.ajoberstar.reckon'
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
    result.output.normalize() == 'No git repository found for :. Accessing grgit will cause an NPE.\nunspecified\n'
  }

  def 'if no strategies specified, build fails'() {
    given:
    Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.grgit'
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
    result.output.contains('Must provide strategies for normal and preRelease on the reckon extension.')
  }

  def 'if reckoned version has build metadata no tag created'() {
    given:
    Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.grgit'
  id 'org.ajoberstar.reckon'
}

reckon {
  normal = scopeFromProp()
  preRelease = stageFromProp('alpha','beta', 'final')
}
"""
    when:
    def result = build('reckonTagPush')
    then:
    result.task(':reckonTagCreate').outcome == TaskOutcome.SKIPPED
    result.task(':reckonTagPush').outcome == TaskOutcome.SKIPPED
    result.output.contains('Reckoned version: 1.1.0-alpha.0')
  }

  def 'if reckoned version has no build metadata tag created and pushed'() {
    given:
    Grgit.clone(dir: projectDir, uri: remote.repository.rootDir)

    buildFile << """
plugins {
  id 'org.ajoberstar.grgit'
  id 'org.ajoberstar.reckon'
}

reckon {
  normal = scopeFromProp()
  preRelease = stageFromProp('alpha','beta', 'final')
}
"""
    when:
    def result = build('reckonTagPush', '-Preckon.stage=alpha')
    then:
    result.task(':reckonTagCreate').outcome == TaskOutcome.SUCCESS
    result.task(':reckonTagPush').outcome == TaskOutcome.SUCCESS
    result.output.contains('Reckoned version: 1.1.0-alpha.1')
    and:
    remote.tag.list().find { it.name == '1.1.0-alpha.1' }
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
