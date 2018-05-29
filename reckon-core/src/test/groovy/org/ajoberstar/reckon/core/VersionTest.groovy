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
package org.ajoberstar.reckon.core

import spock.lang.Specification
import spock.lang.Unroll

class VersionTest extends Specification {
  @Unroll
  def 'parse returns empty if argument is not valid semantic version'(String version) {
    expect:
    Version.parse(version) == Optional.empty()
    where:
    version << [null, 'not a version', '1.0-345']
  }

  def 'parse returns version if argument is valid version'() {
    expect:
    Version.parse('1.0.0-rc.1').map { it.toString() } == Optional.of('1.0.0-rc.1')
  }

  def 'isFinal returns true only if the version does not have a pre-release component'() {
    expect:
    !Version.valueOf('1.0.0-rc.1').isFinal()
    Version.valueOf('1.0.0').isFinal()
  }

  def 'getNormal returns the normal component of the version only'() {
    expect:
    Version.valueOf('1.2.3').getNormal() == Version.valueOf('1.2.3')
    Version.valueOf('1.2.3-rc.1').getNormal() == Version.valueOf('1.2.3')
    Version.valueOf('1.2.3+other.stuff').getNormal() == Version.valueOf('1.2.3')
  }

  def 'incrementNormal correctly uses the scope'() {
    given:
    def base = Version.valueOf('1.2.3-rc.1')
    expect:
    base.incrementNormal(Scope.MAJOR) == Version.valueOf('2.0.0')
    base.incrementNormal(Scope.MINOR) == Version.valueOf('1.3.0')
    base.incrementNormal(Scope.PATCH) == Version.valueOf('1.2.4')
  }

  def 'inferScope correctly finds the scope'() {
    expect:
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('1.2.4-milestone.1')) == Optional.of(Scope.PATCH)
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('1.3.0-milestone.1')) == Optional.of(Scope.MINOR)
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('2.0.0-milestone.1')) == Optional.of(Scope.MAJOR)
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('0.4.0')) == Optional.empty()
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('2.1.0')) == Optional.empty()
    Scope.infer(Version.valueOf('1.2.3'), Version.valueOf('1.2.5')) == Optional.empty()
  }
}
