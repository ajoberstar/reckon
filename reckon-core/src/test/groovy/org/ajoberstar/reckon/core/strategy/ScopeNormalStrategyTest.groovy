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
package org.ajoberstar.reckon.core.strategy

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.reckon.core.Scope
import org.ajoberstar.reckon.core.VcsInventory
import spock.lang.Specification

class ScopeNormalStrategyTest extends Specification {
  def 'if scope supplier returns null, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    when:
    new ScopeNormalStrategy({ null }).reckonNormal(inventory)
    then:
    thrown(NullPointerException)
  }

  def 'if scope supplier returns invalid scope, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    when:
    new ScopeNormalStrategy({ Optional.of("general") }).reckonNormal(inventory)
    then:
    def e = thrown(IllegalArgumentException)
    e.getMessage() == 'Scope "general" is not one of: major, minor, patch'
  }

  def 'if supplier returns empty, scope defaults to minor if base version is base normal'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.2'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.empty() }).reckonNormal(inventory) == Version.valueOf('1.3.0')
  }

  def 'if supplier returns empty, scope defaults to scope used by base version'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.empty() }).reckonNormal(inventory) == Version.valueOf('1.2.3')
  }

  def 'if no conflict with parallel or claimed, incremented version is returned'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory) == Version.valueOf('2.0.0')
  }

  def 'if incremented version is in the parallel normals, increment again'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('2.0.0')] as Set,
        [] as Set
        )
    expect:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory) == Version.valueOf('3.0.0')
  }

  def 'if target normal is in the claimed versions, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [Version.valueOf('2.0.0')] as Set
        )
    when:
    new ScopeNormalStrategy({ Optional.of('major') }).reckonNormal(inventory)
    then:
    thrown(IllegalStateException)
  }

}
