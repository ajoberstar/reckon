package org.ajoberstar.reckon.core;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface ScopeCalculator {
  Optional<Scope> calculate(VcsInventory inventory);

  default ScopeCalculator or(ScopeCalculator otherCalc) {
    return inventory -> this.calculate(inventory)
        .or(() -> otherCalc.calculate(inventory));
  }

  /**
   * Creates a scope calculator that calculates the scope from a user string, handling empty strings
   * and mixed case.
   * 
   * @param scopeCalc a scope calculator returning a string instead of a Scope
   * @return a legit scope calculator
   */
  static ScopeCalculator ofUserString(Function<VcsInventory, Optional<String>> scopeCalc) {
    return inventory -> {
      var scopeStr = scopeCalc.apply(inventory);
      return scopeStr.filter(value -> !value.isEmpty()).map(Scope::from);
    };
  }

  /**
   * Creates a scope calculator that uses the given function to parse the inventory's commit messages
   * for the presence os scope indicators. If any are found, the most significant scope is returned.
   * 
   * @param messageScope function that parses a single commit message for a scope indicator
   * @return a legit scope calculator
   */
  static ScopeCalculator ofCommitMessage(Function<String, Optional<Scope>> messageScope) {
    return inventory -> inventory.getCommitMessages().stream()
        .map(messageScope)
        .flatMap(Optional::stream)
        .max(Comparator.naturalOrder());
  }
}
