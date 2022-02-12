package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.ajoberstar.grgit.Commit;
import org.ajoberstar.grgit.Grgit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReckonerIntegTest {
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1530724706), ZoneId.of("UTC"));
  private static final String TIMESTAMP = "20180704T171826Z";

  private Path repoDir;
  private Grgit grgit;
  private String initialBranch;
  private SecureRandom random = new SecureRandom();

  @Test
  @DisplayName("rebuild works with parallel branches")
  public void rebuildOnParallel() throws IOException {
    var feature1 = commit();
    tag("0.1.0");
    branch("release-0.1");
    var feature2 = commit();
    tag("0.2.0-rc.1");
    commit();
    checkout("release-0.1");
    commit();
    tag("0.1.1");

    assertEquals("0.1.1", reckonStage(null, null));
    checkout("0.1.0");
    assertEquals("0.1.0", reckonStage(null, null));
    checkout(initialBranch);
    tag("0.2.0");
    assertEquals("0.2.0", reckonStage(null, null));
    checkout("0.1.1");
    assertEquals("0.1.1", reckonStage(null, null));
    checkout("0.1.0");
    assertEquals("0.1.0", reckonStage(null, null));
  }

  @Disabled("This is't working")
  @Test
  @DisplayName("multiple release branches")
  public void multipleReleaseBranches() throws IOException {
    commit();
    branch("release/2.0.x");
    checkout("release/2.0.x");
    commit();
    tag("2.0.0");
    commit();
    tag("2.0.1");
    checkout("2.0.0");
    branch("release/2.1.x");
    checkout("release/2.1.x");
    commit();
    checkout("2.0.0");
    branch("release/3.0.x");
    checkout("release/3.0.x");
    commit();
    tag("3.0.0-dev.0");
    commit();
    tag("3.0.0-rc.1");
    checkout("2.0.0");
    branch("release/3.1.x");
    checkout("release/3.1.x");
    commit();
    tag("3.1.0-dev.0");
    commit();

    assertEquals("3.1.0-dev.1.1+" + TIMESTAMP, reckonStage(null, null));
  }

  @BeforeEach
  public void setupRepo() throws IOException {
    repoDir = Files.createTempDirectory("repo");
    grgit = Grgit.init(op -> {
      op.setDir(repoDir);
    });
    initialBranch = grgit.getBranch().current().getName();
  }

  @AfterEach
  public void cleanupRepo() {
    grgit.close();
  }

  private String reckonStage(Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .git(grgit.getRepository().getJgit().getRepository())
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .stages("beta", "milestone", "rc", "final")
        .build()
        .reckon()
        .toString();
  }

  private String reckonSnapshot(Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .git(grgit.getRepository().getJgit().getRepository())
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .snapshots()
        .build()
        .reckon()
        .toString();
  }

  private Commit commit() throws IOException {
    var bytes = new byte[128];
    random.nextBytes(bytes);
    var fileName = random.nextInt();
    Files.write(repoDir.resolve(fileName + ".txt"), bytes);
    grgit.add(op -> {
      op.setPatterns(Set.of(fileName + ".txt"));
    });
    var commit = grgit.commit(op -> {
      op.setMessage("do");
    });
    System.out.println("Created commit: " + commit.getAbbreviatedId());
    return commit;
  }

  private void branch(String name) {
    var currentHead = grgit.head();
    var currentBranch = grgit.getBranch().current();
    var newBranch = grgit.getBranch().add(op -> {
      op.setName(name);
    });
    var atCommit = grgit.getResolve().toCommit(newBranch.getFullName());
    System.out.println("Added new branch " + name + " at " + atCommit.getAbbreviatedId());
    assertEquals(grgit.getBranch().current(), currentBranch);
    assertEquals(atCommit, currentHead);
  }

  private void tag(String name) {
    tag(name, true);
  }

  private void tag(String name, boolean annotate) {
    var currentHead = grgit.head();
    var newTag = grgit.getTag().add(op -> {
      op.setName(name);
      op.setAnnotate(annotate);
    });
    var atCommit = grgit.getResolve().toCommit(newTag.getFullName());
    System.out.println("Added new tag " + name + " at " + atCommit.getAbbreviatedId());
    assertEquals(atCommit, currentHead);
  }

  private void checkout(String name) {
    var currentHead = grgit.head();
    grgit.checkout(op -> {
      op.setBranch(name);
    });
    var atCommit = grgit.getResolve().toCommit(name);
    System.out.println("Checked out " + name + " at " + atCommit.getAbbreviatedId());
  }

  private void merge(String name) {
    var currentBranch = grgit.getBranch().current().getName();
    grgit.merge(op -> {
      op.setHead(name);
    });
    System.out.println("Merged " + name + " into " + currentBranch);
    assertEquals(grgit.getBranch().current().getName(), currentBranch);
  }
}
