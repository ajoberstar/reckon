package org.ajoberstar.reckon.core;

import java.util.Arrays;
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
}
