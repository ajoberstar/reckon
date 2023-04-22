package org.ajoberstar.reckon.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;

public final class Gits {
  private static final SecureRandom random = new SecureRandom();

  public static Git clone(File dir, Git origin) throws GitAPIException {
    var uri = origin.getRepository().getDirectory().getAbsolutePath();
    return Git.cloneRepository().setDirectory(dir).setURI(uri).setCloneAllBranches(true).setNoCheckout(false).call();
  }

  public static Path repoFile(Git git, String path) throws IOException {
    var root = git.getRepository().getWorkTree().toPath();
    var result = root.resolve(path);
    Files.createDirectories(result.getParent());
    return result;
  }

  public static void commit(Git git, String message) throws IOException, GitAPIException {
    var bytes = new byte[128];
    random.nextBytes(bytes);
    var fileName = random.nextInt();
    Files.write(repoFile(git, fileName + ".txt"), bytes);

    git.add().addFilepattern(fileName + ".txt").call();
    var commit = git.commit().setMessage(message).call();

    System.out.println("Created commit: " + commit.getId().name());
  }

  public static void commitAll(Git git) throws IOException, GitAPIException {
    commitAll(git, "do");
  }

  public static void commitAll(Git git, String message) throws IOException, GitAPIException {
    git.add().addFilepattern(".").call();
    var commit = git.commit().setMessage(message).call();
    System.out.println("Created commit: " + commit.getId().name());
  }

  public static void branch(Git git, String name) throws IOException, GitAPIException {
    var currentHead = git.getRepository().resolve("HEAD");
    var currentBranch = git.getRepository().getBranch();
    var newBranch = git.branchCreate().setName(name).call();

    var atCommit = git.getRepository().resolve(newBranch.getName());

    System.out.println("Added new branch " + name + " at " + atCommit.name());
  }

  public static void tag(Git git, String name) throws GitAPIException, IOException {
    tag(git, name, true);
  }

  public static void tag(Git git, String name, boolean annotate) throws IOException, GitAPIException {
    var currentHead = git.getRepository().resolve("HEAD");
    var newTag = git.tag().setName(name).setAnnotated(annotate).call();
    var atCommit = git.getRepository().resolve(newTag.getName() + "^{commit}");
    System.out.println("Added new tag " + name + " at " + atCommit.name());
  }

  public static void checkout(Git git, String name) throws IOException, GitAPIException {
    git.checkout().setName(name).call();
    var atCommit = git.getRepository().resolve(name);
    System.out.println("Checked out " + name + " at " + atCommit.name());
  }

  public static void merge(Git git, String name) throws IOException, GitAPIException {
    var currentBranch = git.getRepository().getBranch();
    git.merge().include(git.getRepository().resolve(name)).call();
    System.out.println("Merged " + name + " into " + currentBranch);
  }

  public static void remoteAdd(Git git, String name, File uri) throws URISyntaxException, GitAPIException {
    git.remoteAdd().setName(name).setUri(new URIish(uri.getAbsolutePath())).call();
    git.fetch().setRemote(name).call();
  }

  public static boolean hasTag(Git git, String name) throws GitAPIException {
    var tagName = "refs/tags/" + name;
    return git.tagList().call().stream()
        .anyMatch(tag -> tagName.equals(tag.getName()));
  }
}
