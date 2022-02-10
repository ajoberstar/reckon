package org.ajoberstar.reckon.gradle;

import java.util.List;

import javax.inject.Inject;

import org.ajoberstar.grgit.gradle.GrgitService;
import org.ajoberstar.reckon.core.Version;
import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

@UntrackedTask(because = "Git tracks the state")
public class ReckonPushTagTask extends DefaultTask {
  private Property<GrgitService> grgitService;
  private Property<Version> version;

  @Inject
  public ReckonPushTagTask(ObjectFactory objectFactory) {
    this.grgitService = objectFactory.property(GrgitService.class);
    this.version = objectFactory.property(Version.class);
  }

  @TaskAction
  public void create() {
    var git = grgitService.get().getGrgit();

    var tagExists = git.getTag().list().stream()
        .anyMatch(tag -> tag.getName().equals(version.get().toString()));

    if (tagExists) {
      git.push(op -> {
        op.setRefsOrSpecs(List.of("refs/tags/" + version.get()));
      });
      setDidWork(true);
    } else {
      setDidWork(false);
    }

  }

  @Internal
  public Property<GrgitService> getGrgitService() {
    return grgitService;
  }

  @Input
  public Property<Version> getVersion() {
    return version;
  }
}
