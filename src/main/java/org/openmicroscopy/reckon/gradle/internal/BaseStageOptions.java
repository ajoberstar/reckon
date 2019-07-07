package org.openmicroscopy.reckon.gradle.internal;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.openmicroscopy.reckon.core.VcsInventory;
import org.openmicroscopy.reckon.core.Version;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.BiFunction;

abstract class BaseStageOptions extends BaseOptions {

    final Property<String> defaultStage;

    final ListProperty<String> stages;

    @Inject
    BaseStageOptions(Project project) {
        super(project);
        this.defaultStage = project.getObjects().property(String.class);
        this.stages = project.getObjects().listProperty(String.class);
    }

    abstract public BiFunction<VcsInventory, Version, Optional<String>> evaluateStage();

}
