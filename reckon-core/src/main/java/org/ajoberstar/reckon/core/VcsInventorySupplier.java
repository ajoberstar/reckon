package org.ajoberstar.reckon.core;

/**
 * This is intentionally package private.
 */
@FunctionalInterface
interface VcsInventorySupplier {
  VcsInventory getInventory();
}
