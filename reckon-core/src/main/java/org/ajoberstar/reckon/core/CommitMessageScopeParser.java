package org.ajoberstar.reckon.core;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A functional interface for parsing Git commit messages for Reckon scopes. The implementation can
 * decide what convention within the message denotes each scope value.
 */
@FunctionalInterface
public interface CommitMessageScopeParser {
  Optional<Scope> parse(String messageBody, boolean preV1);

  /**
   * Returns a parser that checks the message subject for a prefixed like so:
   * {@code <scope>(<area>): subject}. If the project is currently pre-v1, a prefix of {@code major: }
   * will be downgraded to {@code minor}, unless you use {@code major!: } with an exclamation point.
   * 
   * @return parser that reads scopes from subject prefixes
   */
  static CommitMessageScopeParser subjectPrefix() {
    var pattern = Pattern.compile("^(major!|major|minor|patch)(?:\\(.*?\\))?: .+");
    return (msg, preV1) -> {
      var matcher = pattern.matcher(msg);

      if (!matcher.find()) {
        return Optional.empty();
      }

      Scope scope;
      switch (matcher.group(1)) {
        // the ! forces use of major, ignoring preV1 checks
        case "major!":
          scope = Scope.MAJOR;
          break;
        // otherwise we don't allow pre-v1 to bump to major
        case "major":
          scope = preV1 ? Scope.MINOR : Scope.MAJOR;
          break;
        case "minor":
          scope = Scope.MINOR;
          break;
        case "patch":
          scope = Scope.PATCH;
          break;
        default:
          throw new AssertionError("Unhandled scope value matched by regex: " + matcher.group("scope"));
      };
      return Optional.of(scope);
    };
  }

  /**
   * Adapter for legacy message parsers always prevent bumping to v1.
   * 
   * @param parser legacy parser function
   * @return parser that prevents v1 bumps
   */
  static CommitMessageScopeParser ofLegacy(Function<String, Optional<Scope>> parser) {
    return (messageBody, preV1) -> {
      return parser.apply(messageBody).map(scope -> {
        if (preV1 && scope == Scope.MAJOR) {
          return Scope.MINOR;
        } else {
          return scope;
        }
      });
    };
  }
}
