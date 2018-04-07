package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

@FunctionalInterface
public interface NormalStrategy {
  Version reckonNormal(VcsInventory inventory);
}
