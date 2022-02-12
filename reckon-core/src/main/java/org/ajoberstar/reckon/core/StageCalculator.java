package org.ajoberstar.reckon.core;

import java.util.Optional;
import java.util.function.BiFunction;

@FunctionalInterface
public interface StageCalculator {
  Optional<String> calculate(VcsInventory inventory, Version baseVersion);

  static StageCalculator ofUserString(BiFunction<VcsInventory, Version, Optional<String>> stageCalc) {
    return (inventory, base) -> {
      var stageStr = stageCalc.apply(inventory, base);
      return stageStr.map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(String::toLowerCase);
    };
  }
}
