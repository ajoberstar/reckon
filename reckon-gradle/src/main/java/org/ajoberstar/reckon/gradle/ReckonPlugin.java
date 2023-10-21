package org.ajoberstar.reckon.gradle;

import java.util.concurrent.atomic.AtomicBoolean;

import org.ajoberstar.reckon.core.Version;
import org.ajoberstar.reckon.core.VersionTagParser;
import org.ajoberstar.reckon.core.VersionTagWriter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ReckonPlugin implements Plugin<Project> {
  private static Logger logger = Logging.getLogger(ReckonPlugin.class);

  public static final String TAG_TASK = "reckonTagCreate";
  public static final String PUSH_TASK = "reckonTagPush";

  private static final String SCOPE_PROP = "reckon.scope";
  private static final String STAGE_PROP = "reckon.stage";

  @Override
  public void apply(Project project) {
    if (!project.equals(project.getRootProject())) {
      throw new IllegalStateException("org.ajoberstar.reckon can only be applied to the root project.");
    }

    var extension = project.getExtensions().create("reckon", ReckonExtension.class);
    extension.getRepoDirectory().set(project.getLayout().getProjectDirectory());
    extension.getRemote().convention("origin");
    extension.setTagParser(VersionTagParser.getDefault());
    extension.setTagWriter(VersionTagWriter.getDefault());
    extension.getTagMessage().convention(extension.getVersion().map(Version::toString));

    // composite builds have a parent Gradle build and can't trust the values of these properties
    if (project.getGradle().getParent() == null) {
      extension.getScope().set(project.getProviders().gradleProperty(SCOPE_PROP));
      extension.getStage().set(project.getProviders().gradleProperty(STAGE_PROP));
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
    private final AtomicBoolean warned;

    public DelayedVersion(Provider<Version> versionProvider) {
      this.versionProvider = versionProvider;
      this.warned = new AtomicBoolean(false);
    }

    @Override
    public String toString() {
      try {
        return versionProvider.get().toString();
      } catch (Exception e) {
        var isConfigError = false;
        for (Throwable ex = e; ex != null; ex = ex.getCause()) {
          if (ex instanceof ReckonConfigurationException) {
            isConfigError = true;
          }
        }

        if (!isConfigError) {
          throw e;
        }

        if (warned.compareAndSet(false, true)) {
          logger.warn("Project version evaluated before reckon was configured. Run with --info to see cause.");
        }
        logger.info("Project version evaluated before reckon was configured.", e);
        return "unspecified";
      }
    }
  }
}
