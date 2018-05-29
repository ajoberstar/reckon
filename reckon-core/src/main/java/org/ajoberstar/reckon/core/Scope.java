package org.ajoberstar.reckon.core;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum Scope {
  MAJOR, MINOR, PATCH;

  public static Scope from(String value) {
    try {
      return Scope.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      String scopes = Arrays.stream(Scope.values())
          .map(Scope::name)
          .map(String::toLowerCase)
          .collect(Collectors.joining(", "));
      String message = String.format("Scope \"%s\" is not one of: %s", value, scopes);
      throw new IllegalArgumentException(message, e);
    }
  }

  public static Optional<Scope> infer(Version before, Version after) {
    int major = after.getVersion().getMajorVersion() - before.getVersion().getMajorVersion();
    int minor = after.getVersion().getMinorVersion() - before.getVersion().getMinorVersion();
    int patch = after.getVersion().getPatchVersion() - before.getVersion().getPatchVersion();
    if (major == 1 && after.getVersion().getMinorVersion() == 0 && after.getVersion().getPatchVersion() == 0) {
      return Optional.of(Scope.MAJOR);
    } else if (major == 0 && minor == 1 && after.getVersion().getPatchVersion() == 0) {
      return Optional.of(Scope.MINOR);
    } else if (major == 0 && minor == 0 && patch == 1) {
      return Optional.of(Scope.PATCH);
    } else {
      return Optional.empty();
    }
  }
}
