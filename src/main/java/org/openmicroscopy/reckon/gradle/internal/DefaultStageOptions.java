package org.openmicroscopy.reckon.gradle.internal;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.util.ConfigureUtil;
import org.openmicroscopy.reckon.core.VcsInventory;
import org.openmicroscopy.reckon.core.Version;
import org.openmicroscopy.reckon.gradle.ReckonPlugin;
import org.openmicroscopy.reckon.gradle.StageOptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultStageOptions extends BaseStageOptions implements StageOptions {

    @Inject
    public DefaultStageOptions(Project project, @Nullable Map<String, ?> args) {
        super(project);

        this.stages.convention(Arrays.asList("alpha", "beta", "rc", "final"));
        this.defaultStage.convention(getDefaultStageConvention());

        if (args != null) {
            ConfigureUtil.configureByMap(args, this);
        }
    }

    @Override
    public ListProperty<String> getStages() {
        return stages;
    }

    @Override
    public Property<String> getDefaultStage() {
        return defaultStage;
    }

    @Override
    public BiFunction<VcsInventory, Version, Optional<String>> evaluateStage() {
        return (inventory, targetNormal) -> findProperty(ReckonPlugin.STAGE_PROP, null);
    }

    /**
     * Default to selecting the first stage alphabetically
     *
     * @return first stage alphabetically
     */
    private Provider<String> getDefaultStageConvention() {
        return this.stages.map(strings ->
                strings.stream().sorted().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No stages supplied.")));
    }

}
