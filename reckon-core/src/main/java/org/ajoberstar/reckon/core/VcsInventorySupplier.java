package org.ajoberstar.reckon.core;

@FunctionalInterface
interface VcsInventorySupplier {
  VcsInventory getInventory();
}
