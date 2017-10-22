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
