package org.ajoberstar.reckon.core;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

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
    return inventory -> {
      var scope = inventory.getCommitMessages().stream()
          .map(messageScope)
          .flatMap(Optional::stream)
          .max(Comparator.naturalOrder());

      // if we're still below 1.0, don't let a commit message push you there
      if (Optional.of(Scope.MAJOR).equals(scope) && inventory.getBaseNormal().compareTo(Version.valueOf("1.0.0")) < 0) {
        return Optional.of(Scope.MINOR);
      } else {
        return scope;
      }
    };
  }

  /**
   * Creates a scope calculator that checks commit messages for a prefix of either: "major: ", "minor:
   * ", or "patch: " enforcing lower case. Any other commit messages are ignored. Conventionally, you
   * would prefix other commits with "chore: ".
   *
   * @return a legit scope calculator
   */
  static ScopeCalculator ofCommitMessages() {
    var pattern = Pattern.compile("^(major|minor|patch)(?:\\(.*?\\))?: .+");
    return ScopeCalculator.ofCommitMessage(msg -> {
      var matcher = pattern.matcher(msg);
      if (matcher.find()) {
        return Optional.of(Scope.from(matcher.group(1)));
      } else {
        return Optional.empty();
      }
    });
  }
}
