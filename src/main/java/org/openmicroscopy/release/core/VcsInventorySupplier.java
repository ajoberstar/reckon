package org.openmicroscopy.release.core;

/**
 * This is intentionally package private.
 */
@FunctionalInterface
interface VcsInventorySupplier {
    VcsInventory getInventory();
}
