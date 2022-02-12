package org.ajoberstar.reckon.core;

import java.util.Optional;
import java.util.function.BiFunction;

@FunctionalInterface
public interface StageCalculator {
  Optional<String> calculate(VcsInventory inventory, Version targetNormal);

  default StageCalculator or(StageCalculator otherCalc) {
    return (inventory, targetNormal) -> this.calculate(inventory, targetNormal)
        .or(() -> otherCalc.calculate(inventory, targetNormal));
  }

  static StageCalculator ofUserString(BiFunction<VcsInventory, Version, Optional<String>> stageCalc) {
    return (inventory, targetNormal) -> {
      var stageStr = stageCalc.apply(inventory, targetNormal);
      return stageStr.map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(String::toLowerCase);
    };
  }
}
