package org.ajoberstar.reckon.core;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Scope is an indication of how large the change between SemVer normal versions is.
 */
public enum Scope {
  // always keep these in ascending order of significance
  PATCH, MINOR, MAJOR;

  public Scope increment() {
    switch (this) {
      case MAJOR:
        throw new IllegalStateException("Cannot increment MAJOR");
      case MINOR:
        return MAJOR;
      case PATCH:
        return MINOR;
      default:
        throw new AssertionError("Invalid scope: " + this);
    }
  }

  /**
   * Parses a String version of a Scope. This is an alternative to {@code valueOf} which only supports
   * literal matches. This method supports mixed case String representations, like Major or minor,
   * instead of just PATCH. Additionally, it provides a better error message when an invalid scope is
   * provided.
   *
   * @param value the string to parse as a scope
   * @return the matching scope
   * @throws IllegalArgumentException if no match was found
   */
  public static Scope from(String value) {
    try {
      return Scope.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      var scopes = Arrays.stream(Scope.values())
          .map(Scope::name)
          .map(String::toLowerCase)
          .collect(Collectors.joining(", "));
      var message = String.format("Scope \"%s\" is not one of: %s", value, scopes);
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Infers the scope between two versions. It looks left-to-right through the components of the
   * normal versions looking for the first difference of 1 that it finds. Anything else is considered
   * an invalid difference. For example, 1.0.0 to 2.0.0 is MAJOR, 1.0.0 to 1.1.0 is MINOR, 1.0.0 to
   * 1.0.1 is PATCH, 1.0.0 to 3.0.0 is invalid.
   *
   * @param before the earlier version to compare
   * @param after the later version to compare
   * @return the scope of the change between the two versions, or empty if they are the same
   * @throws IllegalStateException if they have an invalid increment
   */
  public static Optional<Scope> infer(Version before, Version after) {
    var major = after.getVersion().getMajorVersion() - before.getVersion().getMajorVersion();
    var minor = after.getVersion().getMinorVersion() - before.getVersion().getMinorVersion();
    var patch = after.getVersion().getPatchVersion() - before.getVersion().getPatchVersion();
    if (major == 1 && after.getVersion().getMinorVersion() == 0 && after.getVersion().getPatchVersion() == 0) {
      return Optional.of(Scope.MAJOR);
    } else if (major == 0 && minor == 1 && after.getVersion().getPatchVersion() == 0) {
      return Optional.of(Scope.MINOR);
    } else if (major == 0 && minor == 0 && patch == 1) {
      return Optional.of(Scope.PATCH);
    } else if (major == 0 && minor == 0 && patch == 0) {
      return Optional.empty();
    } else {
      throw new IllegalStateException("Scope between bases " + before + " and " + after + " must be same or 1 MAJOR/MINOR/PATCH increment apart and are not. Cannot determine correct action.");
    }
  }
}
