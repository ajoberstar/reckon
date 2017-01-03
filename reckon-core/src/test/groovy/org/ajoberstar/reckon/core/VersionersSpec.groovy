/*
 * Copyright 2015-2016 the original author or authors.
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
package org.ajoberstar.reckon.core

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import spock.lang.Specification
import spock.lang.Unroll

import static Scope.*

class VersionersSpec extends Specification {
  private Vcs vcs = Mock()
  private Version base = Version.forIntegers(1, 2, 3)

  def 'identity returns the version it was passed'() {
    expect:
    Versioners.identity().infer(base, null) == Version.forIntegers(1, 2, 3)
  }

  def 'force fails if version is not a valid semver version'() {
    when:
    Versioners.force('blah blah')
    then:
    thrown(ParseException)
  }

  def 'force works for valid semver version'() {
    expect:
    Versioners.force('1.2.3-beta.1').infer(base, vcs) == Version.valueOf('1.2.3-beta.1')
  }

  def 'rebuild fails if no current version'() {
    given:
    vcs.currentVersion >> Optional.empty()
    when:
    Versioners.rebuild().infer(base, vcs)
    then:
    thrown(IllegalStateException)
  }

  def 'rebuild works if current versions present in unmodified repo'() {
    given:
    vcs.currentVersion >> Optional.of(Version.forIntegers(1, 2, 3))
    expect:
    Versioners.rebuild().infer(base, vcs) == Version.forIntegers(1, 2, 3)
  }

  @Unroll
  def 'useScope increments from the base, if no previous release'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    scope.getVersioner().infer(base, vcs) == Version.valueOf(inferred)
    where:
    scope | inferred
    MAJOR | '2.0.0'
    MINOR | '1.3.0'
    PATCH | '1.2.4'
  }

  @Unroll
  def 'useScope increments from the previous release, if available'() {
    given:
    vcs.previousRelease >> Optional.of(Version.forIntegers(2, 3, 4))
    vcs.previousVersion >> Optional.empty()
    expect:
    scope.getVersioner().infer(base, vcs) == Version.valueOf(inferred)
    where:
    scope | inferred
    MAJOR | '3.0.0'
    MINOR | '2.4.0'
    PATCH | '2.3.5'
  }

  @Unroll
  def 'useScope maintains pre-release info from previous version, if has same inferred normal'() {
    given:
    vcs.previousRelease >> Optional.of(Version.forIntegers(2, 3, 4))
    vcs.previousVersion >> Optional.of(Version.valueOf('2.4.0-beta.1'))
    expect:
    scope.getVersioner().infer(base, vcs) == Version.valueOf(inferred)
    where:
    scope | inferred
    MAJOR | '3.0.0'
    MINOR | '2.4.0-beta.1'
    PATCH | '2.3.5'
  }

  @Unroll
  def 'useFinalStage only retains normal version'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.finalStage().getVersioner().infer(Version.valueOf(input), vcs) == Version.valueOf(inferred)
    where:
    input			  | inferred
    '2.3.4'			| '2.3.4'
    '1.2.3-beta.1'	 | '1.2.3'
    '0.1.2-rc.1+abcde' | '0.1.2'
  }

  @Unroll
  def 'useFixedStage increments previous count if base version had same stage'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.fixedStage('rc').getVersioner().infer(Version.valueOf(intermediate), vcs) == Version.valueOf(inferred)
    where:
    intermediate		 | inferred
    '1.2.3-rc.1'	   | '1.2.3-rc.2'
    '4.0.0-rc.3.dev.4' | '4.0.0-rc.4'
  }

  def 'useFixedStage uses "stage.1" if base version has different stage'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.fixedStage('rc').getVersioner().infer(Version.valueOf('4.0.0-milestone.3.dev.4'), vcs) == Version.valueOf('4.0.0-rc.1')
  }

  @Unroll
  def 'useFloatingStage increments previous count if base version had same stage'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.floatingStage('dev').getVersioner().infer(Version.valueOf(intermediate), vcs) == Version.valueOf(inferred)
    where:
    intermediate		 | inferred
    '1.2.3-dev.1'		| '1.2.3-dev.2'
    '1.2.3-dev.2.any.1'  | '1.2.3-dev.3'
    '4.0.0-rc.3.dev.4'   | '4.0.0-rc.3.dev.5'
  }

  def 'useFloatingStage appends "stage.1" if base version had higher-precedence stage'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.floatingStage('dev').getVersioner().infer(Version.valueOf('4.0.0-rc.3'), vcs) == Version.valueOf('4.0.0-rc.3.dev.1')
  }

  def 'useFloatingStage uses "stage.1" if base version had lower-precedence stage'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.floatingStage('rc').getVersioner().infer(Version.valueOf('4.0.0-milestone.3.dev.4'), vcs) == Version.valueOf('4.0.0-rc.1')
  }

  def 'useSnapshotStage uses SNAPSHOT as pre-release'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.empty()
    expect:
    Stage.snapshotStage().getVersioner().infer(Version.valueOf("4.0.0-beta.1"), vcs) == Version.valueOf("4.0.0-SNAPSHOT")
  }

  def 'enforcePrecedence returns base version if it is higher than previous version'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> previous
    expect:
    Versioners.enforcePrecedence().infer(base, vcs) == base
    where:
    previous << [
        Optional.empty(),
        Optional.of(Version.valueOf("1.2.2")),
        Optional.of(Version.valueOf("1.2.3-beta.1")),
        Optional.of(Version.valueOf("1.2.3"))
    ]
  }

  def 'enforcePrecedence throws if base version is lower precedence than previous version'() {
    given:
    vcs.previousRelease >> Optional.empty()
    vcs.previousVersion >> Optional.of(Version.valueOf("1.2.4"))
    when:
    Versioners.enforcePrecedence().infer(base, vcs)
    then:
    thrown(IllegalArgumentException)
  }
}
