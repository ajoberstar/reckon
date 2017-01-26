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

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification
import spock.lang.Unroll

class VersionsTest extends Specification {
  @Unroll
  def 'valueOf returns empty if argument is not valid semantic version'(String version) {
    expect:
    Versions.valueOf(version) == Optional.empty()
    where:
    version << [null, 'not a version', '1.0-345']
  }

  def 'valueOf returns version if argument is valid version'() {
    expect:
    Versions.valueOf('1.0.0-rc.1').map { it.toString() } == Optional.of('1.0.0-rc.1')
  }

  def 'isNormal returns true only if the version does not have a pre-release component'() {
    expect:
    !Versions.isNormal(Version.valueOf('1.0.0-rc.1'))
    Versions.isNormal(Version.valueOf('1.0.0'))
  }

  def 'getNormal returns the normal component of the version only'() {
    expect:
    Versions.getNormal(Version.valueOf('1.2.3')) == Version.valueOf('1.2.3')
    Versions.getNormal(Version.valueOf('1.2.3-rc.1')) == Version.valueOf('1.2.3')
    Versions.getNormal(Version.valueOf('1.2.3+other.stuff')) == Version.valueOf('1.2.3')
  }

  def 'incrementNormal correctly uses the scope'() {
    expect:
    Versions.incrementNormal(Version.valueOf('1.2.3-rc.1'), Scope.MAJOR) == Version.valueOf('2.0.0')
    Versions.incrementNormal(Version.valueOf('1.2.3-rc.1'), Scope.MINOR) == Version.valueOf('1.3.0')
    Versions.incrementNormal(Version.valueOf('1.2.3-rc.1'), Scope.PATCH) == Version.valueOf('1.2.4')
  }
}
