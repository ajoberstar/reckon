package org.ajoberstar.reckon.core;

@FunctionalInterface
public interface VcsInventorySupplier {
  VcsInventory getInventory();
}
