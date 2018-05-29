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
