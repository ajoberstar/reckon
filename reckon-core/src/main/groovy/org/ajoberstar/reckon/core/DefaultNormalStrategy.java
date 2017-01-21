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
import java.util.function.Supplier;

class DefaultNormalStrategy implements NormalStrategy {
  private final Supplier<Scope> scopeSupplier;

  public DefaultNormalStrategy(Supplier<Scope> scopeSupplier) {
    this.scopeSupplier = scopeSupplier;
  }

  @Override
  public Version reckonNormal(VcsInventory inventory) {
    Scope scope = scopeSupplier.get();
    Version targetNormal = Versions.incrementNormal(inventory.getBaseNormal(), scope);

    // if a version's already being developed on a parallel branch we'll skip it
    if (inventory.getParallelNormals().contains(targetNormal)) {
      targetNormal = Versions.incrementNormal(targetNormal, scope);
    }

    if (inventory.getClaimedVersions().contains(targetNormal)) {
      throw new IllegalStateException(
          "Reckoned target normal version " + targetNormal + " has already been released.");
    }

    return targetNormal;
  }
}
