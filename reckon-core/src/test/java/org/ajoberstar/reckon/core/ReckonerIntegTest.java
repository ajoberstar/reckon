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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ReckonerIntegTest {
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1530724706), ZoneId.of("UTC"));

  private Path repoDir;
  private Git git;
  private String initialBranch;
  private SecureRandom random = new SecureRandom();

  @Test
  @DisplayName("rebuild works with parallel branches")
  public void rebuildOnParallel() throws IOException, GitAPIException {
    commit();
    tag("0.1.0");
    branch("release-0.1");
    commit();
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

  @BeforeEach
  public void setupRepo() throws IOException, GitAPIException {
    repoDir = Files.createTempDirectory("repo");
    git = Git.init().setDirectory(repoDir.toFile()).call();
    initialBranch = git.getRepository().getBranch();

    var config = git.getRepository().getConfig();
    config.setString("user", null, "name", "Some Person");
    config.setString("user", null, "email", "some.person@example.com");
    config.setString("commit", null, "gpgSign", "false");
    config.setString("tag", null, "gpgSign", "false");
    config.save();
  }

  @AfterEach
  public void cleanupRepo() {
    git.close();
  }

  private String reckonStage(Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .git(git.getRepository())
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .stages("beta", "milestone", "rc", "final")
        .defaultInferredScope(Scope.MINOR)
        .build()
        .reckon()
        .toString();
  }

  private String reckonSnapshot(Scope scope, String stage) {
    return Reckoner.builder()
        .clock(CLOCK)
        .git(git.getRepository())
        .scopeCalc(i -> Optional.ofNullable(scope))
        .stageCalc((i, v) -> Optional.ofNullable(stage))
        .defaultInferredScope(Scope.MINOR)
        .snapshots()
        .build()
        .reckon()
        .toString();
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
    var atCommit = git.getRepository().resolve(name + "^{commit}");
    System.out.println("Checked out " + name + " at " + atCommit.name());
  }

  private void merge(String name) throws IOException, GitAPIException {
    var currentBranch = git.getRepository().getBranch();
    git.merge().include(git.getRepository().resolve(name)).call();
    System.out.println("Merged " + name + " into " + currentBranch);
    assertEquals(git.getRepository().getBranch(), currentBranch);
  }
}
