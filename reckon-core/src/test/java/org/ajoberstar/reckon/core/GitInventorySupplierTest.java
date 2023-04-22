package org.ajoberstar.reckon.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GitInventorySupplierTest {
  private Path repoDir;

  private Git git;

  private SecureRandom random = new SecureRandom();

  private GitInventorySupplier supplier;

  @Test
  @DisplayName("commit id is abbreviated, not full")
  public void commitIdIsAbbreviated() throws IOException {
    assertEquals(Optional.of(git.getRepository().resolve("HEAD").abbreviate(7).name()), supplier.getInventory().getCommitId());
  }

  @Test
  @DisplayName("if HEAD has no tagged versions, current version is empty")
  public void headNotTaggedCurrentEmpty() throws IOException, GitAPIException {
    checkout("head-untagged");
    assertEquals(Optional.empty(), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if single tagged version on HEAD, it is current version")
  public void headOneTagIsCurrent() throws IOException, GitAPIException {
    checkout("head-single-tag");
    assertEquals(Version.parse("0.1.0-milestone.1"), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if multiple tagged version on HEAD, the max is current version")
  public void headMultiTagMaxIsCurrent() throws IOException, GitAPIException {
    checkout("head-multi-tag");
    assertEquals(Version.parse("0.1.0"), supplier.getInventory().getCurrentVersion());
  }

  @Test
  @DisplayName("if no tagged finals in HEAD\"s history, base normal is 0.0.0")
  public void noFinalTagsBaseNormalIdentity() throws IOException, GitAPIException {
    checkout("final-unreachable");
    assertEquals(Version.parse("0.0.0").get(), supplier.getInventory().getBaseNormal());
  }

  @Test
  @DisplayName("if tagged finals in HEAD\"s history, base normal is max of finals which have no other final between them and HEAD")
  public void finalTagsNormalIsNearestMax() throws IOException, GitAPIException {
    checkout("final-reachable");
    assertEquals(Version.parse("1.0.0").get(), supplier.getInventory().getBaseNormal());
  }

  @Test
  @DisplayName("if tagged finals on head, base normal and version are same as current version")
  public void headFinalTagBaseNormalAndVersionAreCurrent() throws IOException, GitAPIException {
    checkout("final-current");
    var inventory = supplier.getInventory();
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseNormal());
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseVersion());
  }

  @Test
  @DisplayName("if no tagged versions in HEAD\"s history, base version is 0.0.0")
  public void noTagsBaseIdentity() throws IOException, GitAPIException {
    checkout("version-unreachable");
    assertEquals(Version.parse("0.0.0").get(), supplier.getInventory().getBaseVersion());
  }

  @Test
  @DisplayName("if tagged versions in HEAD\"s history, base version is max of versions which have no other version between them and HEAD")
  public void taggedBaseVersionIsNearestMax() throws IOException, GitAPIException {
    checkout("version-reachable");
    assertEquals(Version.parse("0.3.0-milestone.1").get(), supplier.getInventory().getBaseVersion());
  }

  @Test
  @DisplayName("if tagged versions on head, base version is same as current version")
  public void headTaggedBaseIsCurrent() throws IOException, GitAPIException {
    checkout("version-current");
    var inventory = supplier.getInventory();
    assertEquals(inventory.getCurrentVersion().get(), inventory.getBaseVersion());
  }

  @Test
  @DisplayName("if current is tagged with final, commits since base is 0")
  public void currentIsFInalCommitsIs0() throws IOException, GitAPIException {
    checkout("final-current");
    assertEquals(0, supplier.getInventory().getCommitsSinceBase());
    assertEquals(List.of(), supplier.getInventory().getCommitMessages());
  }

  @Test
  @DisplayName("if no reachable tagged finals, commits since base is size of log from HEAD")
  public void noFinalTagsCommitsIsHistorySize() throws IOException, GitAPIException {
    checkout("final-unreachable");
    assertEquals(4, supplier.getInventory().getCommitsSinceBase());
    assertEquals(List.of("do", "do", "do", "do"), supplier.getInventory().getCommitMessages());
  }

  @Test
  @DisplayName("if reachable tagged finals, commits since base is size of log from HEAD excluding the base normal")
  public void finalTagsCommitsIsSizeSinceBase() throws IOException, GitAPIException {
    checkout("final-reachable");
    assertEquals(10, supplier.getInventory().getCommitsSinceBase());
  }

  @Test
  @DisplayName("if no branches share merge base with HEAD, no parallel versions returned")
  public void noMergeBasesNoParallel() throws IOException, GitAPIException {
    checkout("parallel-no-base");
    assertEquals(Collections.emptySet(), supplier.getInventory().getParallelNormals());
  }

  @Test
  @DisplayName("if tagged version between HEAD and merge base, no parallel versions returned")
  public void tagsNearerThanMergeBaseNoParallel() throws GitAPIException, IOException {
    checkout("parallel-tagged-since-merge");
    assertEquals(Collections.emptySet(), supplier.getInventory().getParallelNormals());
  }

  @Test
  @DisplayName("if no tagged version between HEAD and merge base, parallel versions returned")
  public void noTagNearerThanMergeBaseHasParallel() throws IOException, GitAPIException {
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
  public void noCommitsEmpty() throws IOException, GitAPIException {
    var emptyRepoDir = Files.createTempDirectory("repo");
    var emptyGit = Git.init()
        .setDirectory(emptyRepoDir.toFile())
        .call();

    var emptySupplier = new GitInventorySupplier(emptyGit.getRepository(), VersionTagParser.getDefault());
    assertEquals(VcsInventory.empty(true), emptySupplier.getInventory());
  }

  @BeforeAll
  public void initRepository() throws IOException, GitAPIException {
    repoDir = Files.createTempDirectory("repo");
    git = Git.init()
        .setDirectory(repoDir.toFile())
        .call();

    var initialBranch = git.getRepository().getBranch();

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
  public void cleanupRepo() {
    git.close();
  }

  @BeforeEach
  public void initSupplier() {
    supplier = new GitInventorySupplier(git.getRepository(), VersionTagParser.getDefault());
  }

  private void commit() throws IOException, GitAPIException {
    var bytes = new byte[128];
    random.nextBytes(bytes);
    var fileName = random.nextInt();
    Files.write(repoDir.resolve(fileName + ".txt"), bytes);

    git.add()
        .addFilepattern(fileName + ".txt")
        .call();

    var commit = git.commit()
        .setMessage("do")
        .call();

    System.out.println("Created commit: " + commit.getId().name());
  }

  private void branch(String name) throws IOException, GitAPIException {
    var currentHead = git.getRepository().resolve("HEAD");
    var currentBranch = git.getRepository().getBranch();
    var newBranch = git.branchCreate().setName(name).call();

    var atCommit = git.getRepository().resolve(newBranch.getName());

    System.out.println("Added new branch " + name + " at " + atCommit.name());
    assertEquals(git.getRepository().getBranch(), currentBranch);
    assertEquals(atCommit, currentHead);
  }

  private void tag(String name) throws GitAPIException, IOException {
    tag(name, true);
  }

  private void tag(String name, boolean annotate) throws IOException, GitAPIException {
    var currentHead = git.getRepository().resolve("HEAD");
    var newTag = git.tag().setName(name).setAnnotated(annotate).call();
    var atCommit = git.getRepository().resolve(newTag.getName() + "^{commit}");
    System.out.println("Added new tag " + name + " at " + atCommit.name());
    assertEquals(atCommit, currentHead);
  }

  private void checkout(String name) throws IOException, GitAPIException {
    git.checkout().setName(name).call();
    var atCommit = git.getRepository().resolve(name);
    System.out.println("Checked out " + name + " at " + atCommit.name());
    assertEquals(git.getRepository().getBranch(), name);
  }

  private void merge(String name) throws IOException, GitAPIException {
    var currentBranch = git.getRepository().getBranch();
    git.merge().include(git.getRepository().resolve(name)).call();
    System.out.println("Merged " + name + " into " + currentBranch);
    assertEquals(git.getRepository().getBranch(), currentBranch);
  }
}
