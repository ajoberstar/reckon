package org.openmicroscopy.release.gradle.internal;

import org.gradle.api.Project;
import org.openmicroscopy.release.gradle.SnapshotOptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

public class DefaultSnapshotOptions extends DefaultStageOptions implements SnapshotOptions {

    @Inject
    public DefaultSnapshotOptions(Project project, @Nullable Map<String, ?> args) {
        super(project, args);

        // Set stages to be snapshot and final
        this.stages.set(Arrays.asList("snapshot", "rc", "final"));

        // Default to selecting the snapshot
        this.defaultStage.convention("snapshot");
    }

}
