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

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.eclipse.jgit.lib.Repository;

public final class Reckoner {
  public static final String FINAL_STAGE = "final";

  private final Repository repo;
  private final Function<String, Optional<String>> tagSelector;
  private final Set<String> stages;
  private final String defaultStage;

  public Reckoner(
      Repository repo, Function<String, Optional<String>> tagSelector, Set<String> stages) {
    this.repo = repo;
    this.tagSelector = tagSelector;
    this.stages = Collections.unmodifiableSet(stages);
    this.defaultStage =
        stages
            .stream()
            .sorted()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No stages provided."));
  }

  public String reckonTagged(Scope scope, String stage) {
    Objects.requireNonNull(scope, "Scope must be non-null.");
    if (!stages.contains(stage)) {
      String message = String.format("Stage \"%s\" is not one of: %s", stage, stages);
      throw new IllegalArgumentException(message);
    }
    return reckon(scope, (base, inventory) -> base.incrementTaggedStage(stage));
  }

  public String reckonUntagged(Scope scope) {
    Objects.requireNonNull(scope, "Scope must be non-null.");
    return reckon(
        scope,
        (base, inventory) ->
            base.incrementUntaggedStage(
                defaultStage, inventory.getCommitsSinceBase(), inventory.getCommitId()));
  }

  private String reckon(
      Scope scope, BiFunction<ReckonVersion, Inventory, ReckonVersion> reckonStage) {
    Inventory inventory = new InventoryService(repo, tagSelector).get();

    ReckonVersion targetNormal = reckonNormal(inventory, scope);
    ReckonVersion targetBase =
        targetNormal.equals(inventory.getBaseVersion().getNormal())
            ? inventory.getBaseVersion()
            : targetNormal;
    ReckonVersion version = reckonStage.apply(targetBase, inventory);

    if (inventory.getClaimedVersions().contains(version)) {
      throw new IllegalStateException(
          "Reckoned version " + version + " has already been released.");
    }

    return version.toString();
  }

  private ReckonVersion reckonNormal(Inventory inventory, Scope scope) {
    ReckonVersion targetNormal = inventory.getBaseNormal().incrementNormal(scope);

    // if a version's already being developed on a parallel branch we'll skip it
    if (inventory.getParallelNormals().contains(targetNormal)) {
      targetNormal = targetNormal.incrementNormal(scope);
    }

    if (inventory.getClaimedVersions().contains(targetNormal)) {
      throw new IllegalStateException(
          "Reckoned target normal version " + targetNormal + " has already been released.");
    }
    return targetNormal;
  }
}
