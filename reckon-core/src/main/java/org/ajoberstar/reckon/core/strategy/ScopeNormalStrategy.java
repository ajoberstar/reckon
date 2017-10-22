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
import java.util.function.Function;
import org.ajoberstar.reckon.core.NormalStrategy;
import org.ajoberstar.reckon.core.Scope;
import org.ajoberstar.reckon.core.VcsInventory;
import org.ajoberstar.reckon.core.Versions;

public class ScopeNormalStrategy implements NormalStrategy {
  private final Function<VcsInventory, Optional<String>> scopeCalc;

  public ScopeNormalStrategy(Function<VcsInventory, Optional<String>> scopeCalc) {
    this.scopeCalc = scopeCalc;
  }

  @Override
  public Version reckonNormal(VcsInventory inventory) {
    Optional<Scope> providedScope =
        scopeCalc
            .apply(inventory)
            .filter(value -> !value.isEmpty())
            .map(String::toUpperCase)
            .map(Scope::valueOf);

    Scope scope;
    if (providedScope.isPresent()) {
      scope = providedScope.get();
    } else {
      Optional<Scope> inferredScope =
          Versions.inferScope(inventory.getBaseNormal(), inventory.getBaseVersion());
      scope = inferredScope.orElse(Scope.MINOR);
    }

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
