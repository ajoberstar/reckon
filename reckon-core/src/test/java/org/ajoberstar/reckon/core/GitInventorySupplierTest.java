package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.ajoberstar.grgit.Grgit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GitInventorySupplierTest {
  private Path repoDir;

  private Grgit grgit;

  private SecureRandom random = new SecureRandom();

  private GitInventorySupplier supplier;

  @Test
  @DisplayName("commit id is abbreviated, not full")
  public void commitIdIsAbbreviated() {
    assertEquals(Optional.of(grgit.head().getAbbreviatedId()), supplier.getInventory().getCommitId());
  }

  @Test
  @DisplayName("if HEAD has no tagged versions, current version is empty")
  public void headNotTaggedCurrentEmpty() {
    checkout("head-untagged");
    assertEquals(Optional.empty(), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if single tagged version on HEAD, it is current version")
  public void headOneTagIsCurrent() {
    checkout("head-single-tag");
    assertEquals(Version.parse("0.1.0-milestone.1"), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if multiple tagged version on HEAD, the max is current version")
  public void headMultiTagMaxIsCurrent() {
    checkout("head-multi-tag");
    assertEquals(Version.parse("0.1.0"), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if no tagged finals in HEAD\"s history, base normal is 0.0.0")
  public void noFinalTagsBaseNormalIdentity() {
    checkout("final-unreachable");
    assertEquals(Version.parse("0.0.0").get(), supplier.getInventory().getBaseNormal());
  }

  @Test
  @DisplayName("if tagged finals in HEAD\"s history, base normal is max of finals which have no other final between them and HEAD")
  public void finalTagsNormalIsNearestMax() {
    checkout("final-reachable");
    assertEquals(Version.parse("1.0.0").get(), supplier.getInventory().getBaseNormal());
  }

  @Test
  @DisplayName("if tagged finals on head, base normal and version are same as current version")
  public void headFinalTagBaseNormalAndVersionAreCurrent() {
    checkout("final-current");
    var inventory = supplier.getInventory();
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseNormal());
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseVersion());
  }

  @Test
  @DisplayName("if no tagged versions in HEAD\"s history, base version is 0.0.0")
  public void noTagsBaseIdentity() {
    checkout("version-unreachable");
    assertEquals(Version.parse("0.0.0").get(), supplier.getInventory().getBaseVersion());
  }

  @Test
  @DisplayName("if tagged versions in HEAD\"s history, base version is max of versions which have no other version between them and HEAD")
  public void taggedBaseVersionIsNearestMax() {
    checkout("version-reachable");
    assertEquals(Version.parse("0.3.0-milestone.1").get(), supplier.getInventory().getBaseVersion());
  }

  @Test
  @DisplayName("if tagged versions on head, base version is same as current version")
  public void headTaggedBaseIsCurrent() {
    checkout("version-current");
    var inventory = supplier.getInventory();
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseVersion());
  }

  @Test
  @DisplayName("if current is tagged with final, commits since base is 0")
  public void currentIsFInalCommitsIs0() {
    checkout("final-current");
    assertEquals(0, supplier.getInventory().getCommitsSinceBase());
    assertEquals(List.of(), supplier.getInventory().getCommitMessages());
  }

  @Test
  @DisplayName("if no reachable tagged finals, commits since base is size of log from HEAD")
  public void noFinalTagsCommitsIsHistorySize() {
    checkout("final-unreachable");
    assertEquals(4, supplier.getInventory().getCommitsSinceBase());
    assertEquals(List.of("do", "do", "do", "do"), supplier.getInventory().getCommitMessages());
  }

  @Test
  @DisplayName("if reachable tagged finals, commits since base is size of log from HEAD excluding the base normal")
  public void finalTagsCommitsIsSizeSinceBase() {
    checkout("final-reachable");
    assertEquals(10, supplier.getInventory().getCommitsSinceBase());
  }

  @Test
  @DisplayName("if no branches share merge base with HEAD, no parallel versions returned")
  public void noMergeBasesNoParallel() {
    checkout("parallel-no-base");
    assertEquals(Collections.emptySet(), supplier.getInventory().getParallelNormals());
  }

  @Test
  @DisplayName("if tagged version between HEAD and merge base, no parallel versions returned")
  public void tagsNearerThanMergeBaseNoParallel() {
    checkout("parallel-tagged-since-merge");
    assertEquals(Collections.emptySet(), supplier.getInventory().getParallelNormals());
  }

  @Test
  @DisplayName("if no tagged version between HEAD and merge base, parallel versions returned")
  public void noTagNearerThanMergeBaseHasParallel() {
    checkout("parallel-untagged-since-merge");
    assertEquals(Set.of(Version.parse("0.2.0").get()), supplier.getInventory().getParallelNormals());
  }

  @Test
  @DisplayName("all tagged versions treated as claimed versions")
  public void allTagsClaimed() {
    assertEquals(Set.of(Version.parse("0.1.0-milestone.1").get(),
        Version.parse("0.1.0-rc.1").get(),
        Version.parse("0.1.0").get(),
        Version.parse("0.2.0-rc.1").get(),
        Version.parse("0.3.0-milestone.1").get(),
        Version.parse("0.3.0").get(),
        Version.parse("1.0.0").get()), supplier.getInventory().getClaimedVersions());
  }

  @Test
  @DisplayName("if no commits, all results are empty")
  public void noCommitsEmpty() {
    var emptyGrgit = Grgit.init(op -> {
      try {
        op.setDir(Files.createTempDirectory("repo2").toFile());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    var emptySupplier = new GitInventorySupplier(emptyGrgit.getRepository().getJgit().getRepository(), VersionTagParser.getDefault());
    assertEquals(new VcsInventory(null, true, null, null, null, 0, null, null, null), emptySupplier.getInventory());
  }

  @BeforeAll
  public void initRepository() throws IOException {
    repoDir = Files.createTempDirectory("repo");
    grgit = Grgit.init(op -> {
      op.setDir(repoDir);
    });
    var initialBranch = grgit.getBranch().current().getName();

    commit();
    commit();
    commit();
    branch("version-unreachable");
    branch("head-untagged");

    commit();
    tag("0.1.0-milestone.1");
    branch("head-single-tag");
    branch("version-current");
    branch("final-unreachable");
    tag("not-a-version");

    commit();
    commit();
    tag("0.1.0-rc.1");
    tag("0.1.0");
    branch("final-current");
    branch("head-multi-tag");
    branch("parallel-no-base");

    commit();
    branch("RB_0.2");
    checkout("RB_0.2");
    commit();
    commit();
    tag("0.2.0-rc.1");
    commit();

    checkout(initialBranch);
    commit();
    branch("parallel-untagged-since-merge");
    commit();
    tag("0.3.0-milestone.1", false);
    commit();
    branch("parallel-tagged-since-merge");
    commit();
    commit();
    commit();
    commit();
    commit();
    commit();
    commit();
    merge("RB_0.2");
    branch("version-reachable");

    commit();
    branch("RB_1.0");
    checkout("RB_1.0");
    commit();
    tag("1.0.0");
    commit();
    commit();
    commit();
    commit();
    commit();
    commit();
    commit();

    checkout(initialBranch);
    commit();
    tag("0.3.0");
    commit();
    merge("RB_1.0");
    branch("final-reachable");
  }

  @AfterAll
  public void cleanupRepo() throws IOException {
    grgit.close();
  }

  @BeforeEach
  public void initSupplier() {
    supplier = new GitInventorySupplier(grgit.getRepository().getJgit().getRepository(), VersionTagParser.getDefault());
  }

  private void commit() throws IOException {
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
    assertEquals(grgit.getBranch().current().getName(), name);
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
