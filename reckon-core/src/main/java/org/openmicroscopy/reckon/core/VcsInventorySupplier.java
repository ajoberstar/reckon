package org.openmicroscopy.reckon.core;

/**
 * This is intentionally package private.
 */
@FunctionalInterface
interface VcsInventorySupplier {
  VcsInventory getInventory();
}
