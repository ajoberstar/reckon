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

import org.ajoberstar.reckon.core.VcsInventory
import org.ajoberstar.reckon.core.Version
import spock.lang.Specification

class SnapshotPreReleaseStrategyTest extends Specification {
  def 'if stage supplier returns an invalid stage, throw'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [] as Set,
      [] as Set
    )
    when:
    strategy('not').reckonTargetVersion(inventory, Version.valueOf('2.0.0'))
    then:
    thrown(IllegalArgumentException)
  }

  def 'if stage is final, return the target normal'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [] as Set,
      [] as Set
    )
    expect:
    strategy('final').reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0'
  }

  def 'if stage is snapshot or null, set pre-release to snapshot'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [] as Set,
      [] as Set
    )
    expect:
    strategy('snapshot').reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0-SNAPSHOT'
    strategy(null).reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0-SNAPSHOT'
  }

  def 'if repo has uncommitted changes, fail if stage is final'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      false,
      null,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [] as Set,
      [] as Set
    )
    when:
    strategy('final').reckonTargetVersion(inventory, Version.valueOf('2.0.0'))
    then:
    thrown(IllegalStateException)
  }

  private SnapshotPreReleaseStrategy strategy(stage) {
    return new SnapshotPreReleaseStrategy({ i, v -> Optional.ofNullable(stage) })
  }
}
