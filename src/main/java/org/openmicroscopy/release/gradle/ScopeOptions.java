package org.openmicroscopy.release.gradle;

import org.gradle.api.provider.Property;

public interface ScopeOptions {

    /**
     * Allows customisation of the default scope increment -
     * one of major, minor, or patch (defaults to minor) to specify which component of the previous
     * release should be incremented
     *
     * @return scope property
     */
    Property<String> getScope();

}
