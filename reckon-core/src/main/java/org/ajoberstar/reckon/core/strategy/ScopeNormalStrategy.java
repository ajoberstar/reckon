package org.ajoberstar.reckon.core.strategy;

import java.util.Optional;
import java.util.function.Function;

import com.github.zafarkhaja.semver.Version;
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
    Optional<Scope> providedScope = scopeCalc.apply(inventory).filter(value -> !value.isEmpty()).map(Scope::from);

    Scope scope;
    if (providedScope.isPresent()) {
      scope = providedScope.get();
    } else {
      Optional<Scope> inferredScope = Versions.inferScope(inventory.getBaseNormal(), inventory.getBaseVersion());
      scope = inferredScope.orElse(Scope.MINOR);
    }

    Version targetNormal = Versions.incrementNormal(inventory.getBaseNormal(), scope);

    // if a version's already being developed on a parallel branch we'll skip it
    if (inventory.getParallelNormals().contains(targetNormal)) {
      targetNormal = Versions.incrementNormal(targetNormal, scope);
    }

    if (inventory.getClaimedVersions().contains(targetNormal)) {
      throw new IllegalStateException("Reckoned target normal version " + targetNormal + " has already been released.");
    }

    return targetNormal;
  }
}
