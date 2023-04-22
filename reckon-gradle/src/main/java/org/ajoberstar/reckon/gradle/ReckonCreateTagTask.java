package org.ajoberstar.reckon.gradle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;

@UntrackedTask(because = "Git tracks the state")
public abstract class ReckonCreateTagTask extends DefaultTask {
  @TaskAction
  public void create() {
    if (!getTagName().isPresent()) {
      setDidWork(false);
      return;
    }

    tag(null, null, true);
  }

  @Inject
  protected abstract ExecOperations getExecOperations();

  @Internal
  public abstract DirectoryProperty getRepoDirectory();

  @Input
  @Optional
  public abstract Property<String> getTagName();

  @Input
  public abstract Property<String> getTagMessage();

  private void tag(String userEmail, String userName, boolean allowRetry) {
    var input = new ByteArrayInputStream(getTagMessage().get().getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();
    var error = new ByteArrayOutputStream();

    var cmd = new ArrayList<String>();
    cmd.add("git");

    // provide a fallback for CI-like environments that may not have an identity set
    if (userEmail != null && userName != null) {
      cmd.add("-c");
      cmd.add("user.email=" + userEmail);
      cmd.add("-c");
      cmd.add("user.name=" + userName);
    }

    cmd.add("tag");
    cmd.add("--annotate");

    // take message from STDIN
    cmd.add("--file");
    cmd.add("-");

    cmd.add(getTagName().get());

    var result = getExecOperations().exec(spec -> {
      spec.setWorkingDir(getRepoDirectory());
      spec.setCommandLine(cmd);
      spec.setStandardInput(input);
      spec.setStandardOutput(output);
      spec.setErrorOutput(error);
      spec.setIgnoreExitValue(true);
    });

    if (result.getExitValue() != 0) {
      var errorStr = error.toString(StandardCharsets.UTF_8);
      if (errorStr.contains(String.format("fatal: tag '%s' already exists", getTagName().get()))) {
        setDidWork(false);
      } else if (allowRetry && errorStr.contains(String.format("Committer identity unknown"))) {
        var email = getRecentUserEmail();
        var name = getRecentUserName();
        System.err.println(String.format("Tagging as recent committer %s <%s>, as this machine has no git identity set.", name, email));
        tag(email, name, false);
      } else {
        System.err.println(errorStr);
        result.assertNormalExitValue();
      }
    }
  }

  private String getRecentUserEmail() {
    var output = new ByteArrayOutputStream();
    var result = getExecOperations().exec(spec -> {
      spec.setWorkingDir(getRepoDirectory());
      spec.setCommandLine("git", "log", "-n", "1", "--pretty=format:%ae");
      spec.setStandardOutput(output);
    });
    return output.toString();
  }

  private String getRecentUserName() {
    var output = new ByteArrayOutputStream();
    var result = getExecOperations().exec(spec -> {
      spec.setWorkingDir(getRepoDirectory());
      spec.setCommandLine("git", "log", "-n", "1", "--pretty=format:%an");
      spec.setStandardOutput(output);
    });
    return output.toString();
  }
}
