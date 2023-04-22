package org.ajoberstar.reckon.gradle;

import java.util.ArrayList;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;

@UntrackedTask(because = "Git tracks the state")
public abstract class ReckonPushTagTask extends DefaultTask {
  @TaskAction
  public void create() {
    if (!getTagName().isPresent()) {
      setDidWork(false);
      return;
    }

    getExecOperations().exec(spec -> {
      spec.setWorkingDir(getRepoDirectory());
      var cmd = new ArrayList<String>();
      cmd.add("git");
      cmd.add("push");
      cmd.add(getRemote().get());
      cmd.add("refs/tags/" + getTagName().get());
      spec.setCommandLine(cmd);
    });
  }

  @Inject
  protected abstract ExecOperations getExecOperations();

  @Internal
  public abstract DirectoryProperty getRepoDirectory();

  @Input
  public abstract Property<String> getRemote();

  @Input
  @Optional
  public abstract Property<String> getTagName();
}
