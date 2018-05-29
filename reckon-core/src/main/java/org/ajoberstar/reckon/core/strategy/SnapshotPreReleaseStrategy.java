package org.ajoberstar.reckon.core.strategy;

import java.util.Optional;
import java.util.function.BiFunction;

import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.VcsInventory;
import org.ajoberstar.reckon.core.Version;

public final class SnapshotPreReleaseStrategy implements PreReleaseStrategy {
  public static final String FINAL_STAGE = "final";
  public static final String SNAPSHOT_STAGE = "snapshot";

  private final BiFunction<VcsInventory, Version, Optional<String>> stageCalc;

  public SnapshotPreReleaseStrategy(BiFunction<VcsInventory, Version, Optional<String>> stageCalc) {
    this.stageCalc = stageCalc;
  }

  @Override
  public Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    Optional<String> maybeStage = stageCalc.apply(inventory, targetNormal);

    if (inventory.isClean() && inventory.getCurrentVersion().isPresent() && !maybeStage.isPresent()) {
      // rebuild
      return inventory.getCurrentVersion().get();
    } else {
      String stage = maybeStage.orElse(SNAPSHOT_STAGE);
      if (stage.equals(SNAPSHOT_STAGE)) {
        return Version.valueOf(targetNormal + "-SNAPSHOT");
      } else if (!stage.equals(FINAL_STAGE)) {
        throw new IllegalArgumentException(String.format("Stage \"%s\" is not one of: [snapshot, final]", stage));
      } else if (!inventory.isClean()) {
        throw new IllegalStateException("Cannot release a final version without a clean repo.");
      } else {
        return targetNormal;
      }
    }
  }
}
