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
import spock.lang.Unroll

class StagePreReleaseStrategyTest extends Specification {
  VcsInventory inventory = new VcsInventory(
  'abcdef',
  true,
  null,
  Version.valueOf('1.2.3-milestone.2'),
  Version.valueOf('1.2.2'),
  5,
  [] as Set,
  [] as Set
  )

  def 'if stage supplier returns an invalid stage, throw'() {
    when:
    strategy('not').reckonTargetVersion(inventory, Version.valueOf('2.0.0'))
    then:
    thrown(IllegalArgumentException)
  }

  def 'final stage will return the target normal'() {
    expect:
    strategy('final').reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0'
  }

  def 'if target does not contain stage and stage is null, use the default stage and add num commits and commit id'() {
    expect:
    strategy(null).reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0-initial.0.5+abcdef'
  }

  def 'if target does not contain stage and stage is an empty string, use the default stage and add num commits and commit id'() {
    expect:
    strategy('').reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0-initial.0.5+abcdef'
  }

  def 'if target does not contain stage and stage is present, add num commits and commit id'() {
    expect:
    strategy(null).reckonTargetVersion(inventory, Version.valueOf('1.2.3')).toString() == '1.2.3-milestone.2.5+abcdef'
  }

  def 'if target contains stage and stage matches, increment'() {
    expect:
    strategy('milestone').reckonTargetVersion(inventory, Version.valueOf('1.2.3')).toString() == '1.2.3-milestone.3'
  }

  def 'if target contains stage and stage differs, start from 1'() {
    expect:
    strategy('rc').reckonTargetVersion(inventory, Version.valueOf('1.2.3')).toString() == '1.2.3-rc.1'
    strategy('rc').reckonTargetVersion(inventory, Version.valueOf('2.0.0')).toString() == '2.0.0-rc.1'
  }

  def 'if repo has no commits, show build metadata as uncommitted'() {
    given:
    def inventoryUncommitted = new VcsInventory(
      null,
      false,
      null,
      Version.valueOf('1.2.3-milestone.2'),
      Version.valueOf('1.2.2'),
      5,
      [] as Set,
      [] as Set
    )
    expect:
    strategy(null).reckonTargetVersion(inventoryUncommitted, Version.valueOf('1.2.3')).toString() == '1.2.3-milestone.2.5+uncommitted'

  }

  def 'if repo has uncommitted changes, show build metadata as uncommitted'() {
    given:
    def inventoryUncommitted = new VcsInventory(
      'abcdef',
      false,
      null,
      Version.valueOf('1.2.3-milestone.2'),
      Version.valueOf('1.2.2'),
      5,
      [] as Set,
      [] as Set
    )
    expect:
    strategy(null).reckonTargetVersion(inventoryUncommitted, Version.valueOf('1.2.3')).toString() == '1.2.3-milestone.2.5+abcdef.uncommitted'
  }

  @Unroll
  def 'if repo has uncommitted changes, fail when calculating a #stage stage'(String stage) {
    given:
    def inventoryUncommitted = new VcsInventory(
      'abcdef',
      false,
      null,
      Version.valueOf('1.2.3-milestone.2'),
      Version.valueOf('1.2.2'),
      5,
      [] as Set,
      [] as Set
    )
    when:
    strategy(stage).reckonTargetVersion(inventoryUncommitted, Version.valueOf('1.2.3'))
    then:
    thrown(IllegalStateException)
    where:
    stage << ['rc', 'final']
  }

  private StagePreReleaseStrategy strategy(String stage) {
    return new StagePreReleaseStrategy(['initial', 'milestone', 'rc', 'final'] as Set, { i, v -> Optional.ofNullable(stage) })
  }
}
