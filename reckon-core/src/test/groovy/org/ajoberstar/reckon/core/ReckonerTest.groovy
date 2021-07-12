package org.ajoberstar.reckon.core

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import spock.lang.Specification
import spock.lang.Unroll

class ReckonerTest extends Specification {
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1530724706), ZoneId.of('UTC'))
  private static final String TIMESTAMP = '20180704T171826Z'

  @Unroll
  def 'stages are lowercased'(String stage) {
    given:
    VcsInventory inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('1.2.3-beta.1'),
      Version.valueOf('1.2.2'),
      1,
      [Version.valueOf('1.3.0')] as Set,
      [Version.valueOf('2.0.0-rc.1')] as Set
    )
    when:
    Reckoner.builder()
      .clock(CLOCK)
      .vcs { -> inventory }
      .scopeCalc { i -> Optional.empty() }
      .stages('beTA', 'miLEStone', 'RC', 'Final')
      .stageCalc { i, v -> Optional.ofNullable(stage) }
      .build()
      .reckon()
    then:
    notThrown(IllegalArgumentException)
    where:
    stage << ['BeTa', 'Milestone', 'rc', 'fINal']
  }

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
    when:
    reckonStage(inventory, 'major', 'rc')
    then:
    thrown(IllegalStateException)
  }

  def 'if target version is normal, current version is ignored'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, 'major', 'final') == '2.0.0'
  }

  def 'if current version is present and pre-release, repo is clean, and no input provided, this is a rebuild'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == '1.2.3-milestone.1'
  }

  def 'if current version is present and normal, repo is clean, and no input provided, this is a rebuild'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == '1.2.3'
    reckonSnapshot(inventory, null, null) == '1.2.3'
  }

  def 'if current version is present and pre-release, repo is dirty, and no input provided, this is not a rebuild'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == "1.2.3-milestone.1.1+${TIMESTAMP}"
  }

  def 'if current version is present and normal, repo is dirty, and no input provided, this is not a rebuild'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == "1.3.0-beta.0.1+${TIMESTAMP}"
    reckonSnapshot(inventory, null, null) == '1.3.0-SNAPSHOT'
  }

  def 'if current version is present and normal, repo is clean, allowed to release an incremented final'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, 'minor', 'final') == '1.3.0'
    reckonSnapshot(inventory, 'major', 'final') == '2.0.0'
  }

  def 'if current version is present and normal, repo is clean, allowed to release an incremented pre-release stage'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, null, 'rc') == '1.3.0-rc.1'
    reckonSnapshot(inventory, null, 'snapshot') == '1.3.0-SNAPSHOT'
  }

  def 'if current version is present and pre-release, repo is clean, allowed to release a higher normal pre-release'() {
    given:
    VcsInventory inventory = new VcsInventory(
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
    reckonStage(inventory, 'minor', 'rc') == '1.3.0-rc.1'
  }

    def 'rebuilding claimed version succeeds, if repo is clean'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      Version.valueOf('0.1.0'),
      Version.valueOf('0.0.0'),
      Version.valueOf('0.0.0'),
      1,
      [] as Set,
      [Version.valueOf('0.1.0'), Version.valueOf('0.1.1'), Version.valueOf('0.2.0')] as Set
      )
    expect:
    reckonStage(inventory, null, null) == '0.1.0'
  }

   def 'if target normal claimed, but building insignificant, succeed'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('0.0.0'),
      Version.valueOf('0.0.0'),
      1,
      [] as Set,
      [Version.valueOf('0.1.0'), Version.valueOf('0.1.1'), Version.valueOf('0.2.0')] as Set
      )
    expect:
    reckonStage(inventory, null, null) == '0.1.0-beta.0.1+abcdef'
  }

  def 'if target normal claimed, and building significant, throw'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      true,
      null,
      Version.valueOf('0.0.0'),
      Version.valueOf('0.0.0'),
      1,
      [] as Set,
      [Version.valueOf('0.1.0'), Version.valueOf('0.1.1'), Version.valueOf('0.2.0')] as Set
      )
    when:
    reckonStage(inventory, null, 'rc')
    then:
    thrown(IllegalStateException)
  }

  def 'if scope supplier returns invalid scope, throw'() {
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
    reckonStage(inventory, 'general', 'beta')
    then:
    def e = thrown(IllegalArgumentException)
    e.getMessage() == 'Scope "general" is not one of: major, minor, patch'
  }

  def 'if supplier returns empty, scope defaults to minor if base version is base normal'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.2'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    reckonStage(inventory, null, 'final') == '1.3.0'
  }

  def 'if supplier returns empty, scope defaults to scope used by base version'() {
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
    reckonStage(inventory, null, 'final') == '1.2.3'
  }

  def 'if no conflict with parallel or claimed, incremented version is returned'() {
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
    reckonStage(inventory, 'major', 'final') == '2.0.0'
  }

  def 'if incremented version is in the parallel normals, increment again'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [Version.valueOf('2.0.0')] as Set,
        [] as Set
        )
    expect:
    reckonStage(inventory, 'major', 'final') == '3.0.0'
  }

  def 'if target normal is in the claimed versions, throw'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        true,
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [Version.valueOf('2.0.0')] as Set
        )
    when:
    reckonStage(inventory, 'major', 'final')
    then:
    thrown(IllegalStateException)
  }

  def 'if stage supplier returns an invalid stage, throw'() {
    given:
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
    when:
    reckonStage(inventory, 'major', 'not')
    then:
    thrown(IllegalArgumentException)
  }

  def 'final stage will return the target normal'() {
    given:
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
    expect:
    reckonStage(inventory, 'major', 'final') == '2.0.0'
  }

  def 'if target does not contain stage and stage is null, use the default stage and add num commits and commit id'() {
    given:
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
    expect:
    reckonStage(inventory, 'major', null) == '2.0.0-beta.0.5+abcdef'
  }

  def 'if target does not contain stage and stage is an empty string, use the default stage and add num commits and commit id'() {
    given:
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
    expect:
    reckonStage(inventory, 'major', '') == '2.0.0-beta.0.5+abcdef'
  }

  def 'if target does not contain stage and stage is present, add num commits and commit id'() {
    given:
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
    expect:
    reckonStage(inventory, null, null) == '1.2.3-milestone.2.5+abcdef'
  }

  def 'if target contains stage and stage matches, increment'() {
    given:
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
    expect:
    reckonStage(inventory, null, 'milestone') == '1.2.3-milestone.3'
  }

  def 'if target contains stage and stage differs, start from 1'() {
    given:
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
    expect:
    reckonStage(inventory, null, 'rc') == '1.2.3-rc.1'
    reckonStage(inventory, 'major', 'rc') == '2.0.0-rc.1'
  }

  def 'if repo has no commits, show build metadata as uncommitted'() {
    given:
    def inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == "1.2.3-milestone.2.5+${TIMESTAMP}"

  }

  def 'if repo has uncommitted changes, show build metadata as uncommitted'() {
    given:
    def inventory = new VcsInventory(
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
    reckonStage(inventory, null, null) == "1.2.3-milestone.2.5+${TIMESTAMP}"
  }

  @Unroll
  def 'if repo has uncommitted changes, fail when calculating a #stage stage'(String stage) {
    given:
    def inventory = new VcsInventory(
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
    reckonStage(inventory, null, stage)
    then:
    thrown(IllegalStateException)
    where:
    stage << ['rc', 'final']
  }

  def 'if repo has uncommitted changes, succeed when calculating a snapshot'() {
    given:
    def inventory = new VcsInventory(
      'abcdef',
      false,
      null,
      Version.valueOf('1.2.2'),
      Version.valueOf('1.2.2'),
      5,
      [] as Set,
      [] as Set
    )
    expect:
    reckonSnapshot(inventory, null, 'snapshot') == '1.3.0-SNAPSHOT'
  }

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
    reckonSnapshot(inventory, 'major', 'not')
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
    reckonSnapshot(inventory, 'major', 'final') == '2.0.0'
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
    reckonSnapshot(inventory, 'major', 'snapshot') == '2.0.0-SNAPSHOT'
    reckonSnapshot(inventory, 'major', null) == '2.0.0-SNAPSHOT'
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
    reckonSnapshot(inventory, 'major', 'final')
    then:
    thrown(IllegalStateException)
  }

  private String reckonStage(inventory, scope, stage) {
    return Reckoner.builder()
      .clock(CLOCK)
      .vcs { -> inventory }
      .scopeCalc { i -> Optional.ofNullable(scope) }
      .stages('beta', 'milestone', 'rc', 'final')
      .stageCalc { i, v -> Optional.ofNullable(stage) }
      .build()
      .reckon();
  }

  private String reckonSnapshot(inventory, scope, stage) {
    return Reckoner.builder()
      .clock(CLOCK)
      .vcs { -> inventory }
      .scopeCalc { i -> Optional.ofNullable(scope) }
      .snapshots()
      .stageCalc { i, v -> Optional.ofNullable(stage) }
      .build()
      .reckon();
  }
}
