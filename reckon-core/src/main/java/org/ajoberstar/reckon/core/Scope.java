/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.reckon.core;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Scope {
  MAJOR,
  MINOR,
  PATCH;

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
