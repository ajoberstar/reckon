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
   * Creates a scope calculator that uses the given parser to parse the inventory's commit messages
   * for the presence os scope indicators. If any are found, the most significant scope is returned.
   *
   * @param parser the chosen way to read scopes from commit messages
   * @return a legit scope calculator
   */
  static ScopeCalculator ofCommitMessageParser(CommitMessageScopeParser parser) {
    return inventory -> {
      var preV1 = inventory.getBaseNormal().compareTo(Version.valueOf("1.0.0")) < 0;
      return inventory.getCommitMessages().stream()
          .flatMap(msg -> parser.parse(msg, preV1).stream())
          .max(Comparator.naturalOrder());
    };
  }

  /**
   * Creates a scope calculator that uses the given function to parse the inventory's commit messages
   * for the presence os scope indicators. If any are found, the most significant scope is returned.
   * <br/>
   * Before v1, MAJOR is always ignored and MINOR is substituted. If that's not desirable, see
   * {@link #ofCommitMessageParser(CommitMessageScopeParser)}.
   *
   * @param messageScope function that parses a single commit message for a scope indicator
   * @return a legit scope calculator
   */
  static ScopeCalculator ofCommitMessage(Function<String, Optional<Scope>> messageScope) {
    var parser = CommitMessageScopeParser.ofLegacy(messageScope);
    return ofCommitMessageParser(parser);
  }

  /**
   * Creates a scope calculator that checks commit messages for a prefix of either: "major: ", "minor:
   * ", or "patch: " enforcing lower case. Any other commit messages are ignored. Conventionally, you
   * would prefix other commits with "chore: ".
   *
   * @return a legit scope calculator
   */
  static ScopeCalculator ofCommitMessages() {
    return ScopeCalculator.ofCommitMessageParser(CommitMessageScopeParser.subjectPrefix());
  }
}
