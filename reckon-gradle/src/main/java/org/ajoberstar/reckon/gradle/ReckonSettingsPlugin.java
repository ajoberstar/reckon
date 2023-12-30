package org.ajoberstar.reckon.gradle;

import org.ajoberstar.reckon.core.Version;
import org.ajoberstar.reckon.core.VersionTagParser;
import org.ajoberstar.reckon.core.VersionTagWriter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ReckonSettingsPlugin implements Plugin<Settings> {
  public static final String TAG_TASK = "reckonTagCreate";
  public static final String PUSH_TASK = "reckonTagPush";

  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";

  @Override
  public void apply(Settings settings) {
    var extension = settings.getExtensions().create("reckon", ReckonExtension.class);
    extension.getRepoDirectory().set(settings.getSettingsDir());
    extension.getRemote().convention("origin");
    extension.setTagParser(VersionTagParser.getDefault());
    extension.setTagWriter(VersionTagWriter.getDefault());
    extension.getTagMessage().convention(extension.getVersion().map(Version::toString));

    // composite builds have a parent Gradle build and can't trust the values of these properties
    if (settings.getGradle().getParent() == null) {
      extension.getScope().set(settings.getProviders().gradleProperty(SCOPE_PROP));
      extension.getStage().set(settings.getProviders().gradleProperty(STAGE_PROP));
    }

    var sharedVersion = new DelayedVersion(extension.getVersion());
    settings.getGradle().allprojects(prj -> {
      prj.setVersion(sharedVersion);
    });

    settings.getGradle().rootProject(rootProject -> {
      var tag = createTagTask(rootProject, extension);
      var push = createPushTask(rootProject, extension);
      push.configure(t -> t.dependsOn(tag));
    });
  }

  private TaskProvider<ReckonCreateTagTask> createTagTask(Project project, ReckonExtension extension) {
    return project.getTasks().register(TAG_TASK, ReckonCreateTagTask.class, task -> {
      task.setDescription("Tag version inferred by reckon.");
      task.setGroup("publishing");
      task.getRepoDirectory().set(extension.getRepoDirectory());
      task.getTagName().set(extension.getTagName());
      task.getTagMessage().set(extension.getTagMessage());
    });
  }

  private TaskProvider<ReckonPushTagTask> createPushTask(Project project, ReckonExtension extension) {
    return project.getTasks().register(PUSH_TASK, ReckonPushTagTask.class, task -> {
      task.setDescription("Push version tag created by reckon.");
      task.setGroup("publishing");
      task.getRepoDirectory().set(extension.getRepoDirectory());
      task.getRemote().set(extension.getRemote());
      task.getTagName().set(extension.getTagName());
    });
  }

  private static class DelayedVersion {
    private final Provider<Version> versionProvider;

    public DelayedVersion(Provider<Version> versionProvider) {
      this.versionProvider = versionProvider;
    }

    @Override
    public String toString() {
      return versionProvider.get().toString();
    }
  }
}
