/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.reckon.core;

import com.github.zafarkhaja.semver.Version;

public final class Reckoner {
  private Reckoner() {
    // do not instantiate
  }

  public static String reckon(
      VcsInventorySupplier inventorySupplier,
      NormalStrategy normalStrategy,
      PreReleaseStrategy preReleaseStrategy) {
    VcsInventory inventory = inventorySupplier.getInventory();
    Version targetNormal = normalStrategy.reckonNormal(inventory);
    Version targetVersion = preReleaseStrategy.reckonTargetVersion(inventory, targetNormal);

    // unless it looks like an intentional increment, we should consider this a rebuild
    Version reckoned =
        inventory
            .getCurrentVersion()
            .filter(
                current ->
                    !Versions.getNormal(current).equals(Versions.getNormal(targetVersion))
                        && !Versions.isNormal(targetVersion))
            .orElse(targetVersion);

    if (inventory.getClaimedVersions().contains(reckoned)
        && !inventory.getCurrentVersion().map(reckoned::equals).orElse(false)) {
      throw new IllegalStateException(
          "Reckoned version " + reckoned + " has already been released.");
    }

    if (inventory.getBaseVersion().compareTo(reckoned) > 0) {
      throw new IllegalStateException(
          "Reckoned version "
              + reckoned
              + " is (and cannot be) less than base version "
              + inventory.getBaseVersion());
    }

    return reckoned.toString();
  }
}
