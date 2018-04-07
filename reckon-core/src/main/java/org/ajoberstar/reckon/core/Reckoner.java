package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

public final class Reckoner {
  private Reckoner() {
    // do not instantiate
  }

  public static String reckon(VcsInventorySupplier inventorySupplier, NormalStrategy normalStrategy, PreReleaseStrategy preReleaseStrategy) {
    VcsInventory inventory = inventorySupplier.getInventory();
    Version targetNormal = normalStrategy.reckonNormal(inventory);
    Version targetVersion = preReleaseStrategy.reckonTargetVersion(inventory, targetNormal);

    // unless it looks like an intentional increment, we should consider this a rebuild
    Version reckoned = inventory.getCurrentVersion()
        .filter(current -> !(Versions.getNormal(current).equals(Versions.getNormal(targetVersion)) || Versions.isNormal(targetVersion)))
        .orElse(targetVersion);

    if (inventory.getClaimedVersions().contains(reckoned) && !inventory.getCurrentVersion().map(reckoned::equals).orElse(false)) {
      throw new IllegalStateException("Reckoned version " + reckoned + " has already been released.");
    }

    if (inventory.getBaseVersion().compareTo(reckoned) > 0) {
      throw new IllegalStateException("Reckoned version " + reckoned + " is (and cannot be) less than base version " + inventory.getBaseVersion());
    }

    return reckoned.toString();
  }
}
