package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

public final class Reckoner {
  private Reckoner() {
    // do not instantiate
  }

  public static String reckon(VcsInventorySupplier inventorySupplier, NormalStrategy normalStrategy, PreReleaseStrategy preReleaseStrategy) {
    VcsInventory inventory = inventorySupplier.getInventory();
    Version targetNormal = normalStrategy.reckonNormal(inventory);
    Version reckoned = preReleaseStrategy.reckonTargetVersion(inventory, targetNormal);

    if (inventory.getClaimedVersions().contains(reckoned) && !inventory.getCurrentVersion().map(reckoned::equals).orElse(false)) {
      throw new IllegalStateException("Reckoned version " + reckoned + " has already been released.");
    }

    if (inventory.getBaseVersion().compareTo(reckoned) > 0) {
      throw new IllegalStateException("Reckoned version " + reckoned + " is (and cannot be) less than base version " + inventory.getBaseVersion());
    }

    inventory.getCurrentVersion().ifPresent(current -> {
      if (inventory.isClean() && Versions.isNormal(current) && !Versions.isNormal(reckoned)) {
        throw new IllegalStateException("Cannot re-release a final version " + current + " as a pre-release: " + reckoned);
      }
    });

    return reckoned.toString();
  }
}
