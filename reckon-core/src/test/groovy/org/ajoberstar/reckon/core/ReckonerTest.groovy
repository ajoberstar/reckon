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
import org.ajoberstar.reckon.core.strategy.ScopeNormalStrategy;
import org.ajoberstar.reckon.core.strategy.StagePreReleaseStrategy;
import org.ajoberstar.reckon.core.strategy.SnapshotPreReleaseStrategy;

class ReckonerTest extends Specification {
  def 'if version is claimed, throw'() {
    given:
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
    inventorySupplier.getInventory() >> inventory
    NormalStrategy normal = Mock()
    normal.reckonNormal(inventory) >> Version.valueOf('2.0.0')
    PreReleaseStrategy preRelease = Mock()
    preRelease.reckonTargetVersion(inventory, Version.valueOf('2.0.0')) >> Version.valueOf('2.0.0-rc.1')
    when:
    Reckoner.reckon(inventorySupplier, normal, preRelease)
    then:
    thrown(IllegalStateException)
  }

  def 'if version is not greater than base, throw'() {
    given:
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
    inventorySupplier.getInventory() >> inventory
    NormalStrategy normal = Mock()
    normal.reckonNormal(inventory) >> Version.valueOf('1.0.0')
    PreReleaseStrategy preRelease = Mock()
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
    expect:
    reckonStage(inventory2, 'major', 'final') == '2.0.0'
  }

  def 'if current version is present and pre-release, repo is clean, and no input provided, this is a rebuild'() {
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
    expect:
    reckonStage(inventory2, null, null) == '1.2.3-milestone.1'
  }

  def 'if current version is present and normal, repo is clean, and no input provided, this is a rebuild'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1')] as Set
    )
    expect:
    reckonStage(inventory2, null, null) == '1.2.3'
    reckonSnapshot(inventory2, null, null) == '1.2.3'
  }

  def 'if current version is present and pre-release, repo is dirty, and no input provided, this is not a rebuild'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      false,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [Version.valueOf('1.3.0')] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1')] as Set
    )
    expect:
    reckonStage(inventory2, null, null) == '1.2.3-milestone.1.1+abcdef.uncommitted'
  }

  def 'if current version is present and normal, repo is dirty, and no input provided, this is not a rebuild'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      false,
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3')] as Set
    )
    expect:
    reckonStage(inventory2, null, null) == '1.3.0-beta.0.1+abcdef.uncommitted'
    reckonSnapshot(inventory2, null, null) == '1.3.0-SNAPSHOT'
  }

  def 'if current version is present and normal, repo is clean, allowed to release an incremented final'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3')] as Set
    )
    expect:
    reckonStage(inventory2, 'minor', 'final') == '1.3.0'
    reckonSnapshot(inventory2, 'major', false) == '2.0.0'
  }

  def 'if current version is present and normal, repo is clean, not allowed to release an incremented pre-release stage'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3')] as Set
    )
    when:
    reckonStage(inventory2, null, 'rc')
    then:
    thrown(IllegalStateException)
  }

  def 'if current version is present and normal, repo is clean, not allowed to release an incremented snapshot'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      Version.valueOf('1.2.3'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3')] as Set
    )
    when:
    reckonSnapshot(inventory2, null, true)
    then:
    thrown(IllegalStateException)
  }

  def 'if current version is present and pre-release, repo is clean, allowed to release a higher normal pre-release'() {
    given:
    VcsInventory inventory2 = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.3-milestone.1'),
      Version.valueOf('1.2.2'),
      1,
      [] as Set,
      [Version.valueOf('1.2.2'), Version.valueOf('1.2.3-milestone.1')] as Set
    )
    expect:
    reckonStage(inventory2, 'minor', 'rc') == '1.3.0-rc.1'
  }

  private String reckonStage(inventory, scope, stage) {
    ScopeNormalStrategy normal = new ScopeNormalStrategy({ i -> Optional.ofNullable(scope) })
    StagePreReleaseStrategy preRelease = new StagePreReleaseStrategy(['beta', 'rc', 'final'] as Set, { i, v -> Optional.ofNullable(stage) })
    return Reckoner.reckon({ -> inventory }, normal, preRelease)
  }

  private String reckonSnapshot(inventory, scope, snapshot) {
    ScopeNormalStrategy normal = new ScopeNormalStrategy({ i -> Optional.ofNullable(scope) })
    SnapshotPreReleaseStrategy preRelease = new SnapshotPreReleaseStrategy({ i, v -> Optional.ofNullable(snapshot) })
    return Reckoner.reckon({ -> inventory }, normal, preRelease)
  }
}
