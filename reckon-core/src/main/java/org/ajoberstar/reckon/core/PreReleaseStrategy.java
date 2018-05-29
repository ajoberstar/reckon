package org.ajoberstar.reckon.core;

@FunctionalInterface
public interface PreReleaseStrategy {
  Version reckonTargetVersion(VcsInventory inventory, Version targetNormal);
}
