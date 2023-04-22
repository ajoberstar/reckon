package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ReckonerTest {
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1530724706), ZoneId.of("UTC"));
  private static final String TIMESTAMP = "20180704T171826Z";

  @Test
  @DisplayName("if snapshot provided to stages builder, throw")
  public void snapshotInStagesThrows() {
    assertThrows(IllegalArgumentException.class, () -> Reckoner.builder().stages("snapshot", "beta", "final"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"BeTa", "Milestone", "rc", "fINal"})
  @DisplayName("stages are lowercased")
  public void stagesLowercased(String stage) {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-beta.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("1.3.0")),
        Set.of(Version.valueOf("2.0.0-rc.1")),
        List.of());

    Reckoner.builder()
        .clock(CLOCK)
        .vcs(() -> inventory)
        .scopeCalc(i -> Optional.empty())
        .stages("beTA", "miLEStone", "RC", "Final")
        .stageCalc(StageCalculator.ofUserString((i, v) -> Optional.of(stage)))
        .build()
        .reckon();
  }

  @Test
  @DisplayName("if version is claimed, throw")
  public void claimedVersionThrows() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("1.3.0")),
        Set.of(Version.valueOf("2.0.0-rc.1")),
        List.of());

    assertThrows(IllegalStateException.class, () -> reckonStage(inventory, Scope.MAJOR, "rc"));
  }

  @Test
  @DisplayName("if target version is normal, current version is ignored")
  public void targetNormalCurrentIgnored() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("1.3.0")),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());

    assertEquals("2.0.0", reckonStage(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if current version is present and pre-release, repo is clean, and no input provided, this is a rebuild")
  public void currentPrereleaseNoInputRebuild() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("1.3.0")),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());

    assertEquals("1.2.3-milestone.1", reckonStage(inventory, null, null));
  }

  @Test
  @DisplayName("if current version is present and normal, repo is clean, and no input provided, this is a rebuild")
  public void currentNormalNoInputRebuild() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        1,
        Set.of(),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());
    assertEquals("1.2.3", reckonStage(inventory, null, null));
    assertEquals("1.2.3", reckonSnapshot(inventory, null, null));
  }

  @Test
  @DisplayName("if current version is present and pre-release, repo is dirty, and no input provided, this is not a rebuild")
  public void currentPrereleaseDirtyNoInputNoRebuild() {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("1.3.0")),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());

    assertEquals("1.2.3-milestone.1.1+" + TIMESTAMP, reckonStage(inventory, null, null));
  }

  @Test
  @DisplayName("if current version is present and normal, repo is dirty, and no input provided, this is not a rebuild")
  public void currentNormalDirtyNoInputNoRebuild() {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        1,
        Set.of(),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());

    assertEquals("1.3.0-beta.0.1+" + TIMESTAMP, reckonStage(inventory, null, null));
    assertEquals("1.3.0-SNAPSHOT", reckonSnapshot(inventory, null, null));
  }

  @Test
  @DisplayName("if current version is present and normal, repo is clean, allowed to release an incremented final")
  public void currentNormalCanFinal() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        1,
        Set.of(),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());

    assertEquals("1.3.0", reckonStage(inventory, Scope.MINOR, "final"));
    assertEquals("2.0.0", reckonSnapshot(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if current version is present and normal, repo is clean, allowed to release an incremented pre-release stage")
  public void currentNormalCanPrerelease() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        Version.valueOf("1.2.3"),
        1,
        Set.of(),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3")),
        List.of());
    assertEquals("1.3.0-rc.1", reckonStage(inventory, null, "rc"));
    assertEquals("1.3.0-SNAPSHOT", reckonSnapshot(inventory, null, "snapshot"));
  }

  @Test
  @DisplayName("if current version is present and pre-release, repo is clean, allowed to release a higher normal pre-release")
  public void currentPreReleaseCanIncNormalPreRelease() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(Version.valueOf("1.2.2"), Version.valueOf("1.2.3-milestone.1")),
        List.of());
    assertEquals("1.3.0-rc.1", reckonStage(inventory, Scope.MINOR, "rc"));
  }

  @Test
  @DisplayName("rebuilding claimed version succeeds, if repo is clean")
  public void claimedRebuild() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        Version.valueOf("0.1.0"),
        Version.valueOf("0.0.0"),
        Version.valueOf("0.0.0"),
        1,
        Set.of(),
        Set.of(Version.valueOf("0.1.0"), Version.valueOf("0.1.1"), Version.valueOf("0.2.0")),
        List.of());
    assertEquals("0.1.0", reckonStage(inventory, null, null));
  }

  @Test
  @DisplayName("if target normal claimed, but building insignificant, succeed")
  public void claimedCanInsignificant() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("0.0.0"),
        Version.valueOf("0.0.0"),
        1,
        Set.of(),
        Set.of(Version.valueOf("0.1.0"), Version.valueOf("0.1.1"), Version.valueOf("0.2.0")),
        List.of());
    assertEquals("0.1.0-beta.0.1+abcdef", reckonStage(inventory, null, null));
  }

  @Test
  @DisplayName("if target normal claimed, and building significant, throw")
  public void claimedCannotSignificant() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("0.0.0"),
        Version.valueOf("0.0.0"),
        1,
        Set.of(),
        Set.of(Version.valueOf("0.1.0"), Version.valueOf("0.1.1"), Version.valueOf("0.2.0")),
        List.of());
    assertThrows(IllegalStateException.class, () -> reckonStage(inventory, null, "rc"));
  }

  @Test
  @DisplayName("if supplier returns empty, scope defaults to minor if base version is base normal")
  public void emptyScopeDefaults() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.2"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.3.0", reckonStage(inventory, null, "final"));
  }

  @Test
  @DisplayName("if supplier returns empty, scope defaults to provided default inferred scope if base version is base normal")
  public void emptyScopeOverriddenDefault() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.2"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    var actual = Reckoner.builder()
        .clock(CLOCK)
        .vcs(() -> inventory)
        .scopeCalc(i -> Optional.empty())
        .stageCalc((i, v) -> Optional.of("final"))
        .defaultInferredScope(Scope.PATCH)
        .stages("beta", "milestone", "rc", "final")
        .build()
        .reckon()
        .toString();
    assertEquals("1.2.3", actual);
  }

  @Test
  @DisplayName("if supplier returns empty, scope defaults to scope used by base version")
  public void emptyScopeInferred() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3", reckonStage(inventory, null, "final"));
  }

  @Test
  @DisplayName("if no conflict with parallel or claimed, incremented version is returned")
  public void noConflictingParallelOrClaimedIncrements() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("2.0.0", reckonStage(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if incremented version is in the parallel normals, increment again")
  public void conflictingParallelDoubleIncrements() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(Version.valueOf("2.0.0")),
        Set.of(),
        List.of());
    assertEquals("3.0.0", reckonStage(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if incremented via paralel still in parallel, increment with higher scope")
  public void doubleConflictingParallelIncrementsHigherScope() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.1.0"),
        Version.valueOf("1.1.0"),
        1,
        Set.of(Version.valueOf("1.1.1"), Version.valueOf("1.1.2")),
        Set.of(Version.valueOf("1.1.0"), Version.valueOf("1.1.1"), Version.valueOf("1.1.2")),
        List.of());

    var reckoner = Reckoner.builder()
        .clock(CLOCK)
        .vcs(() -> inventory)
        .parallelBranchScope(Scope.MINOR)
        .scopeCalc(i -> Optional.ofNullable(Scope.PATCH))
        .stageCalc((i, v) -> Optional.ofNullable("final"))
        .stages("beta", "milestone", "rc", "final")
        .build();

    assertEquals("1.2.0", reckoner.reckon().toString());
  }

  @Test
  @DisplayName("if target normal is in the claimed versions, throw")
  public void claimedTargetThrows() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(Version.valueOf("2.0.0")),
        List.of());
    assertThrows(IllegalStateException.class, () -> reckonStage(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if stage supplier returns an invalid stage, throw")
  public void stageInvalidThrows() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertThrows(IllegalArgumentException.class, () -> reckonStage(inventory, Scope.MAJOR, "not"));
  }

  @Test
  @DisplayName("final stage will return the target normal")
  public void finalIsTargetNormal() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("2.0.0", reckonStage(inventory, Scope.MAJOR, "final"));
    assertEquals("2.0.0", reckonSnapshot(inventory, Scope.MAJOR, "final"));
  }

  @Test
  @DisplayName("if target does not contain stage and stage is null, use the default stage and add num commits and commit id")
  public void noStageTargetEmptyStageUsesDefaultInsignificant() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("2.0.0-beta.0.5+abcdef", reckonStage(inventory, Scope.MAJOR, null));
  }

  @Test
  @DisplayName("if target does not contain stage and stage is present, add num commits and commit id")
  public void noStageTargetHasStageInsignificant() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3-milestone.2.5+abcdef", reckonStage(inventory, null, null));
  }

  @Test
  @DisplayName("if target contains stage and stage matches, increment")
  public void hasStageTargetAndMatchStageIncrement() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3-milestone.3", reckonStage(inventory, null, "milestone"));
  }

  @Test
  @DisplayName("if target contains stage and stage differs, start from 1")
  public void hasStageTargetAndDifferStageUse1() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3-rc.1", reckonStage(inventory, null, "rc"));
    assertEquals("2.0.0-rc.1", reckonStage(inventory, Scope.MAJOR, "rc"));
  }

  @Test
  @DisplayName("if repo has no commits, show build metadata as uncommitted")
  public void noCommitsShows() {
    var inventory = new VcsInventory(
        null,
        false,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3-milestone.2.5+" + TIMESTAMP, reckonStage(inventory, null, null));

  }

  @Test
  @DisplayName("if repo has uncommitted changes, show build metadata as uncommitted")
  public void uncommittedChangesShows() {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.2.3-milestone.2.5+" + TIMESTAMP, reckonStage(inventory, null, null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"rc", "final"})
  @DisplayName("if repo has uncommitted changes, fail when calculating a {1} stage")
  public void uncommittedNoSignificant(String stage) {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        null,
        Version.valueOf("1.2.3-milestone.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertThrows(IllegalStateException.class, () -> reckonStage(inventory, null, stage));
  }

  @Test
  @DisplayName("if repo has uncommitted changes, succeed when calculating a snapshot")
  public void uncommittedAllowSnapshot() {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        null,
        Version.valueOf("1.2.2"),
        Version.valueOf("1.2.2"),
        5,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("1.3.0-SNAPSHOT", reckonSnapshot(inventory, null, "snapshot"));
  }

  @Test
  @DisplayName("if stage is snapshot or null, set pre-release to snapshot")
  public void snapshotOrEmptyUsesSnapshot() {
    var inventory = new VcsInventory(
        "abcdef",
        true,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    assertEquals("2.0.0-SNAPSHOT", reckonSnapshot(inventory, Scope.MAJOR, "snapshot"));
    assertEquals("2.0.0-SNAPSHOT", reckonSnapshot(inventory, Scope.MAJOR, null));
  }

  @Test
  @DisplayName("if repo has uncommitted changes, fail if stage is final")
  public void uncommittedNoFinal() {
    var inventory = new VcsInventory(
        "abcdef",
        false,
        null,
        Version.valueOf("1.2.3-milestone.1"),
        Version.valueOf("1.2.2"),
        1,
        Set.of(),
        Set.of(),
        List.of());
    assertThrows(IllegalStateException.class, () -> reckonSnapshot(inventory, Scope.MAJOR, "final"));
  }

  private String reckonStage(VcsInventory inventory, Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .vcs(() -> inventory)
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .stages("beta", "milestone", "rc", "final")
        .build()
        .reckon()
        .toString();
  }

  private String reckonSnapshot(VcsInventory inventory, Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .vcs(() -> inventory)
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .snapshots()
        .build()
        .reckon()
        .toString();
  }
}
