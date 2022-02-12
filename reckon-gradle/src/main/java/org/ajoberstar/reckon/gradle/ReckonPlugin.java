package org.ajoberstar.reckon.gradle;

import org.ajoberstar.grgit.gradle.GrgitServiceExtension;
import org.ajoberstar.grgit.gradle.GrgitServicePlugin;
import org.ajoberstar.reckon.core.Version;
import org.ajoberstar.reckon.core.VersionTagParser;
import org.ajoberstar.reckon.core.VersionTagWriter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ReckonPlugin implements Plugin<Project> {
  public static final String TAG_TASK = "reckonTagCreate";
  public static final String PUSH_TASK = "reckonTagPush";

  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";

  @Override
  public void apply(Project project) {
    if (!project.equals(project.getRootProject())) {
      throw new IllegalStateException("org.ajoberstar.reckon can only be applied to the root project.");
    }
    project.getPluginManager().apply(GrgitServicePlugin.class);

    var grgitServiceExtension = project.getExtensions().getByType(GrgitServiceExtension.class);
    var grgitService = grgitServiceExtension.getService();

    var extension = project.getExtensions().create("reckon", ReckonExtension.class);
    extension.getGrgitService().set(grgitService);
    extension.setTagParser(VersionTagParser.getDefault());
    extension.setTagWriter(VersionTagWriter.getDefault());
    extension.getTagMessage().convention(extension.getVersion().map(Version::toString));

    // composite builds have a parent Gradle build and can't trust the values of these properties
    if (project.getGradle().getParent() == null) {
      extension.getScope().set(project.getProviders().gradleProperty(SCOPE_PROP).forUseAtConfigurationTime());
      extension.getStage().set(project.getProviders().gradleProperty(STAGE_PROP).forUseAtConfigurationTime());
    }

    var sharedVersion = new DelayedVersion(extension.getVersion());
    project.allprojects(prj -> {
      prj.setVersion(sharedVersion);
    });

    var tag = createTagTask(project, extension);
    var push = createPushTask(project, extension);
    push.configure(t -> t.dependsOn(tag));
  }

  private TaskProvider<ReckonCreateTagTask> createTagTask(Project project, ReckonExtension extension) {
    return project.getTasks().register(TAG_TASK, ReckonCreateTagTask.class, task -> {
      task.setDescription("Tag version inferred by reckon.");
      task.setGroup("publishing");
      task.getGrgitService().set(extension.getGrgitService());
      task.getVersion().set(extension.getVersion());
      task.getTagWriter().set(extension.getTagWriter());
      task.getTagMessage().set(extension.getTagMessage());
    });
  }

  private TaskProvider<ReckonPushTagTask> createPushTask(Project project, ReckonExtension extension) {
    return project.getTasks().register(PUSH_TASK, ReckonPushTagTask.class, task -> {
      task.setDescription("Push version tag created by reckon.");
      task.setGroup("publishing");
      task.getGrgitService().set(extension.getGrgitService());
      task.getVersion().set(extension.getVersion());
      task.getTagWriter().set(extension.getTagWriter());
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
