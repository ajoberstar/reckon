package org.ajoberstar.reckon.gradle;

import org.ajoberstar.grgit.gradle.GrgitService;
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
    Provider<GrgitService> grgitService = settings.getGradle().getSharedServices().registerIfAbsent("reckon-grgit", GrgitService.class, spec -> {
      spec.getParameters().getCurrentDirectory().set(settings.getSettingsDir());
      spec.getParameters().getInitIfNotExists().set(false);
      spec.getMaxParallelUsages().set(1);
    });

    var extension = settings.getExtensions().create("reckon", ReckonExtension.class);
    extension.getGrgitService().set(grgitService);
    extension.setTagParser(VersionTagParser.getDefault());
    extension.setTagWriter(VersionTagWriter.getDefault());
    extension.getTagMessage().convention(extension.getVersion().map(Version::toString));

    // composite builds have a parent Gradle build and can't trust the values of these properties
    if (settings.getGradle().getParent() == null) {
      extension.getScope().set(settings.getProviders().gradleProperty(SCOPE_PROP).forUseAtConfigurationTime());
      extension.getStage().set(settings.getProviders().gradleProperty(STAGE_PROP).forUseAtConfigurationTime());
    }

    var sharedVersion = new DelayedVersion(extension.getVersion());
    settings.getGradle().allprojects(prj -> {
      prj.setVersion(sharedVersion);
    });

    settings.getGradle().projectsLoaded(gradle -> {
      var tag = createTagTask(settings.getGradle().getRootProject(), extension);
      var push = createPushTask(settings.getGradle().getRootProject(), extension);
      push.configure(t -> t.dependsOn(tag));
    });
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
      task.getRemote().set(extension.getRemote());
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
