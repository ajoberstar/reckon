package org.openmicroscopy.reckon.gradle.internal;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.openmicroscopy.reckon.core.VcsInventory;
import org.openmicroscopy.reckon.core.Version;
import org.openmicroscopy.reckon.gradle.ReckonPlugin;
import org.openmicroscopy.reckon.gradle.SnapshotOptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultSnapshotOptions extends DefaultStageOptions implements SnapshotOptions {

    @Inject
    public DefaultSnapshotOptions(Project project, @Nullable Map<String, ?> args) {
        super(project, args);

        // Set stages to be snapshot and final
        this.stages.set(Arrays.asList("snapshot", "rc", "final"));

        // Default to selecting the snapshot
        this.defaultStage.convention("snapshot");
    }

    @Override
    public BiFunction<VcsInventory, Version, Optional<String>> evaluateStage() {
        return (inventory, targetNormal) -> findProperty(ReckonPlugin.STAGE_PROP, null);
    }
}
