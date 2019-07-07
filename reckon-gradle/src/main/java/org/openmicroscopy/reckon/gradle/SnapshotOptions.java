package org.openmicroscopy.reckon.gradle;

import org.gradle.api.provider.Property;

public interface SnapshotOptions {

    /**
     * Used to determine whether a snapshot should be made one of true or false (defaults to true)
     * to determine whether a snapshot should be made
     *
     * @return the snapshot boolean
     */
    Property<Boolean> getSnapshot();

}
