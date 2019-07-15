package org.openmicroscopy.release.gradle;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.ajoberstar.grgit.Grgit;
import org.ajoberstar.grgit.Repository;
import org.eclipse.jgit.api.Git;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskProvider;
import org.openmicroscopy.release.core.Reckoner;
import org.openmicroscopy.release.core.Version;
import org.openmicroscopy.release.gradle.internal.DefaultReleaseExtension;
import org.openmicroscopy.release.gradle.internal.DefaultScopeOptions;
import org.openmicroscopy.release.gradle.internal.DefaultStageOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReleasePlugin implements Plugin<Project> {

    public static final String RECKON_EXTENSION = "release";

    public static final String TAG_TASK = "reckonTagCreate";
    public static final String PUSH_TASK = "reckonTagPush";

    public static final String SCOPE_PROP = "release.scope";
    public static final String STAGE_PROP = "release.stage";
    public static final String SNAPSHOT_PROP = "release.snapshot";

    private static final Logger logger = Logging.getLogger(ReleasePlugin.class);

    private Project project;

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new IllegalStateException("org.openmicroscopy.release can only be applied to the root project.");
        }
        this.project = project;

        project.getPluginManager().apply("org.ajoberstar.grgit");

        DefaultReleaseExtension reckon =
                (DefaultReleaseExtension) project.getExtensions()
                        .create(ReleaseExtension.class, RECKON_EXTENSION, DefaultReleaseExtension.class, project);

        Grgit grgit = (Grgit) project.findProperty("grgit");


        reckonVersion(grgit, reckon);

        TaskProvider<Task> tag = createTagTask(project, grgit);
        TaskProvider<Task> push = createPushTask(project, grgit, tag);
    }

    private TaskProvider<Task> createTagTask(Project project, Grgit grgit) {
        TaskProvider<Task> provider = project.getTasks().register(TAG_TASK);
        provider.configure(task -> {
            task.setDescription("Tag version inferred by release.");
            task.setGroup("publishing");
            task.onlyIf(t -> {
                Version version = ((DelayedVersion) project.getVersion()).getVersion();

                // rebuilds shouldn't trigger a new tag
                boolean alreadyTagged = grgit.getTag().list().stream()
                        .anyMatch(tag -> tag.getName().equals(version.toString()));

                return version.isSignificant() && !alreadyTagged;
            });
            task.doLast(t -> {
                Map<String, Object> args = new HashMap<>();
                args.put("name", project.getVersion());
                args.put("message", project.getVersion());
                grgit.getTag().add(args);
            });
        });
        return provider;
    }

    private TaskProvider<Task> createPushTask(Project project, Grgit grgit, TaskProvider<Task> create) {
        TaskProvider<Task> provider = project.getTasks().register(PUSH_TASK);
        provider.configure(task -> {
            task.setDescription("Push version tag created by release.");
            task.setGroup("publishing");
            task.dependsOn(create);
            task.onlyIf(t -> create.get().getDidWork());
            task.doLast(t -> {
                Map<String, Object> args = new HashMap<>();
                args.put("refsOrSpecs", Collections.singletonList("refs/tags/" + project.getVersion().toString()));
                grgit.push(args);
            });
        });
        return provider;
    }

    private void reckonVersion(Grgit grgit, DefaultReleaseExtension reckonExt) {
        project.afterEvaluate(project -> {
            org.eclipse.jgit.lib.Repository repo = Optional.ofNullable(grgit)
                    .map(Grgit::getRepository)
                    .map(Repository::getJgit)
                    .map(Git::getRepository)
                    .orElse(null);

            DefaultStageOptions stageOptions = reckonExt.getStageOptions();
            DefaultScopeOptions scopeOptions = reckonExt.getScopeOptions();

            Reckoner reckoner = Reckoner.builder()
                    .git(repo)
                    .stages(stageOptions.getStages().get())
                    .defaultStage(stageOptions.getDefaultStage().get())
                    .stageCalc(stageOptions.evaluateStage())
                    .scopeCalc(scopeOptions.evaluateScope())
                    .build();

            Version version = reckoner.reckon();
            logger.warn("Reckoned version: {}", version);

            DelayedVersion sharedVersion = new DelayedVersion(() -> version);
            project.allprojects(prj -> prj.setVersion(sharedVersion));
        });
    }

    private static class DelayedVersion {
        private final Supplier<Version> reckoner;

        public DelayedVersion(Supplier<Version> reckoner) {
            this.reckoner = Suppliers.memoize(reckoner);
        }

        public Version getVersion() {
            return reckoner.get();
        }

        @Override
        public String toString() {
            return reckoner.get().toString();
        }
    }
}
