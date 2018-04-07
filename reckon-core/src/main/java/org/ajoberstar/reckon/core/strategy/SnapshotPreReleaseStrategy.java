package org.ajoberstar.reckon.core.strategy;

import com.github.zafarkhaja.semver.Version;
import java.util.Optional;
import java.util.function.BiFunction;
import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.VcsInventory;

public final class SnapshotPreReleaseStrategy implements PreReleaseStrategy {
  private final BiFunction<VcsInventory, Version, Optional<Boolean>> snapshotCalc;

  public SnapshotPreReleaseStrategy(BiFunction<VcsInventory, Version, Optional<Boolean>> snapshotCalc) {
    this.snapshotCalc = snapshotCalc;
  }

  @Override
  public Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    Optional<Boolean> maybeSnapshot = snapshotCalc.apply(inventory, targetNormal);

    if (inventory.isClean() && inventory.getCurrentVersion().isPresent() && !maybeSnapshot.isPresent()) {
      // rebuild
      return inventory.getCurrentVersion().get();
    } else {
      if (maybeSnapshot.orElse(true)) {
        return targetNormal.setPreReleaseVersion("SNAPSHOT");
      } else if (!inventory.isClean()) {
        throw new IllegalStateException("Cannot release a final version without a clean repo.");
      } else {
        return targetNormal;
      }
    }
  }
}
