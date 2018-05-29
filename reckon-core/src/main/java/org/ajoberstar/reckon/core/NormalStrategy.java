package org.ajoberstar.reckon.core;

@FunctionalInterface
public interface NormalStrategy {
  Version reckonNormal(VcsInventory inventory);
}
