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

    final Property<Boolean> snapshot;

    @Inject
    public DefaultSnapshotOptions(Project project, @Nullable Map<String, ?> args) {
        super(project, args);

        this.snapshot = project.getObjects().property(Boolean.class);

        // Set stages to be snapshot and final
        this.stages.set(Arrays.asList("snapshot", "final"));

        // Default to selecting the snapshot
        this.defaultStage.convention("snapshot");
        this.snapshot.convention(true);
    }

    @Override
    public Property<Boolean> getSnapshot() {
        return snapshot;
    }

    @Override
    public BiFunction<VcsInventory, Version, Optional<String>> evaluateStage() {
        return (inventory, targetNormal) -> {

            Optional<String> stageProp =
                    findProperty(ReckonPlugin.STAGE_PROP, defaultStage.get());

            Optional<String> snapshotProp =
                    findProperty(ReckonPlugin.SNAPSHOT_PROP, snapshot.get())
                            .map(Boolean::parseBoolean)
                            .map(isSnapshot -> isSnapshot ? "snapshot" : "final");

            snapshotProp.ifPresent(val -> {
                project.getLogger().warn("Property {} is deprecated and will be removed in 1.0.0. Use {} set to one of" +
                        " [snapshot, final].", ReckonPlugin.SNAPSHOT_PROP, ReckonPlugin.STAGE_PROP);
            });

            return stageProp.isPresent() ? stageProp : snapshotProp;
        };
    }
}
