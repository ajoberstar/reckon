package org.ajoberstar.reckon.core;

import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface ScopeCalculator {
  Optional<Scope> calculate(VcsInventory inventory);

  static ScopeCalculator ofUserString(Function<VcsInventory, Optional<String>> scopeCalc) {
    return inventory -> {
      var scopeStr = scopeCalc.apply(inventory);
      return scopeStr.filter(value -> !value.isEmpty()).map(Scope::from);
    };
  }
}
