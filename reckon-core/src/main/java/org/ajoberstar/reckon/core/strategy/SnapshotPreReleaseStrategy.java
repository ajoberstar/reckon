package org.ajoberstar.reckon.core.strategy;

import com.github.zafarkhaja.semver.Version;
import java.util.Optional;
import java.util.function.BiFunction;
import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.VcsInventory;

public final class SnapshotPreReleaseStrategy implements PreReleaseStrategy {
  private final BiFunction<VcsInventory, Version, Boolean> snapshotCalc;

  public SnapshotPreReleaseStrategy(BiFunction<VcsInventory, Version, Boolean> snapshotCalc) {
    this.snapshotCalc = snapshotCalc;
  }

  @Override
  public Version reckonTargetVersion(VcsInventory inventory, Version targetNormal) {
    boolean isSnapshot = Optional.ofNullable(snapshotCalc.apply(inventory, targetNormal)).orElse(true);
    if (isSnapshot) {
      return targetNormal.setPreReleaseVersion("SNAPSHOT");
    } else {
      return targetNormal;
    }
  }
}
