package org.openmicroscopy.reckon.gradle;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public interface StageOptions {

    /**
     * Allows customisation of a set of publish stages -
     * defaults to alpha, beta, rc and final.
     *
     * @return a configurable {@code ListProperty} of strings
     */
    ListProperty<String> getStages();

    /**
     * Sets the default stage to select when no stage is specified via the command line.
     * <p>
     * See {@link #getStages()}
     *
     * @return a configurable string {@code Property}
     */
    Property<String> getDefaultStage();

}
