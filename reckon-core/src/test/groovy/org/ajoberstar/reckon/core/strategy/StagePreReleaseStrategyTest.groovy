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
import org.ajoberstar.reckon.core.VcsInventory
import spock.lang.Specification

class StagePreReleaseStrategyTest extends Specification {
  VcsInventory inventory = new VcsInventory(
  'abcdef',
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
    strategy('final').reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0')
  }

  def 'if target does not contain stage and stage is null, use the default stage and add num commits and commit id'() {
    expect:
    strategy(null).reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0-dev.0.5+abcdef')
  }

  def 'if target does not contain stage and stage is present, add num commits and commit id'() {
    expect:
    strategy(null).reckonTargetVersion(inventory, Version.valueOf('1.2.3')) == Version.valueOf('1.2.3-milestone.2.5+abcdef')
  }

  def 'if target contains stage and stage matches, increment'() {
    expect:
    strategy('milestone').reckonTargetVersion(inventory, Version.valueOf('1.2.3')) == Version.valueOf('1.2.3-milestone.3')
  }

  def 'if target contains stage and stage differs, start from 1'() {
    expect:
    strategy('rc').reckonTargetVersion(inventory, Version.valueOf('1.2.3')) == Version.valueOf('1.2.3-rc.1')
    strategy('rc').reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0-rc.1')
  }

  private StagePreReleaseStrategy strategy(String stage) {
    return new StagePreReleaseStrategy(['dev', 'milestone', 'rc', 'final'] as Set, { stage })
  }
}
