package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

@FunctionalInterface
public interface PreReleaseStrategy {
  Version reckonTargetVersion(VcsInventory inventory, Version targetNormal);
}
