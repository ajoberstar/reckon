package org.ajoberstar.reckon.gradle;

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
public class ReckonCreateTagTask extends DefaultTask {
  private Property<GrgitService> grgitService;
  private Property<Version> version;

  @Inject
  public ReckonCreateTagTask(ObjectFactory objectFactory) {
    this.grgitService = objectFactory.property(GrgitService.class);
    this.version = objectFactory.property(Version.class);
  }

  @TaskAction
  public void create() {
    var git = grgitService.get().getGrgit();;
    var v = version.get().toString();

    // rebuilds shouldn't trigger a new tag
    boolean alreadyTagged = git.getTag().list().stream()
        .anyMatch(tag -> tag.getName().equals(v));

    if (alreadyTagged || !version.get().isSignificant()) {
      setDidWork(false);
    } else {
      git.getTag().add(op -> {
        op.setName(version.get().toString());
        op.setMessage(version.get().toString());
      });
      setDidWork(true);
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
