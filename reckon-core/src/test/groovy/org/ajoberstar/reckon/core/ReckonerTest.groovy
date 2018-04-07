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

class ReckonerTest extends Specification {
  VcsInventory inventory = new VcsInventory(
  'abcdef',
  true,
  null,
  Version.valueOf('1.2.3-milestone.1'),
  Version.valueOf('1.2.2'),
  1,
  [Version.valueOf('1.3.0')] as Set,
  [Version.valueOf('2.0.0-rc.1')] as Set
  )

  VcsInventorySupplier inventorySupplier = Mock()
  NormalStrategy normal = Mock()
  PreReleaseStrategy preRelease = Mock()

  def setup() {
    inventorySupplier.getInventory() >> inventory
  }

  def 'if version is claimed, throw'() {
    given:
    normal.reckonNormal(inventory) >> Version.valueOf('2.0.0')
    preRelease.reckonTargetVersion(inventory, Version.valueOf('2.0.0')) >> Version.valueOf('2.0.0-rc.1')
    when:
    Reckoner.reckon(inventorySupplier, normal, preRelease)
    then:
    thrown(IllegalStateException)
  }

  def 'if version is not greater than base, throw'() {
    given:
    normal.reckonNormal(inventory) >> Version.valueOf('1.0.0')
    preRelease.reckonTargetVersion(inventory, Version.valueOf('1.0.0')) >> Version.valueOf('1.0.0-rc.1')
    when:
    Reckoner.reckon(inventorySupplier, normal, preRelease)
    then:
    thrown(IllegalStateException)
  }

  def 'if target version is normal, current version is ignored'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
        'abcdef',
        true,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('1.3.0')] as Set,
        [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1')] as Set
        )
    VcsInventorySupplier inventorySupplier2 = Mock()
    inventorySupplier2.getInventory() >> inventory2
    normal.reckonNormal(inventory2) >> Version.valueOf('2.0.0')
    preRelease.reckonTargetVersion(inventory2, Version.valueOf('2.0.0')) >> Version.valueOf('2.0.0')
    expect:
    Reckoner.reckon(inventorySupplier2, normal, preRelease) == '2.0.0'
  }

  def 'if target version has same normal as current, current version is ignored'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
        'abcdef',
        true,
        Version.valueOf('2.0.0-milestone.1'),
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('1.3.0')] as Set,
        [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1'), Version.valueOf('2.0.0-milestone.1')] as Set
        )
    VcsInventorySupplier inventorySupplier2 = Mock()
    inventorySupplier2.getInventory() >> inventory2
    normal.reckonNormal(inventory2) >> Version.valueOf('2.0.0')
    preRelease.reckonTargetVersion(inventory2, Version.valueOf('2.0.0')) >> Version.valueOf('2.0.0-rc.1')
    expect:
    Reckoner.reckon(inventorySupplier2, normal, preRelease) == '2.0.0-rc.1'
  }

  def 'if target version has different normal than current but is not normal, current version is used'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
        'abcdef',
        true,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('1.3.0')] as Set,
        [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1')] as Set
        )
    VcsInventorySupplier inventorySupplier2 = Mock()
    inventorySupplier2.getInventory() >> inventory2
    normal.reckonNormal(inventory2) >> Version.valueOf('2.0.0')
    preRelease.reckonTargetVersion(inventory2, Version.valueOf('2.0.0')) >> Version.valueOf('2.0.0-rc.1')
    expect:
    Reckoner.reckon(inventorySupplier2, normal, preRelease) == '1.2.3-milestone.1'
  }
}
