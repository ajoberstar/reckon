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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public final class Inventory {
  private final ReckonVersion currentVersion;
  private final int commitsSinceBase;
  private final ReckonVersion baseVersion;
  private final ReckonVersion baseNormal;
  private final Set<ReckonVersion> parallelNormals;
  private final Set<ReckonVersion> claimedVersions;

  public Inventory(
      ReckonVersion currentVersion,
      int commitsSinceBase,
      ReckonVersion baseVersion,
      ReckonVersion baseNormal,
      Set<ReckonVersion> parallelNormals,
      Set<ReckonVersion> claimedVersions) {
    this.currentVersion = currentVersion;
    this.commitsSinceBase = commitsSinceBase;
    this.baseVersion = baseVersion;
    this.baseNormal = baseNormal;
    this.parallelNormals = parallelNormals == null ? Collections.emptySet() : parallelNormals;
    this.claimedVersions = claimedVersions == null ? Collections.emptySet() : claimedVersions;
  }

  public Optional<ReckonVersion> getCurrentVersion() {
    return Optional.ofNullable(currentVersion);
  }

  public int getCommitsSinceBase() {
    return commitsSinceBase;
  }

  public Optional<ReckonVersion> getBaseVersion() {
    return Optional.ofNullable(baseVersion);
  }

  public Optional<ReckonVersion> getBaseNormal() {
    return Optional.ofNullable(baseNormal);
  }

  public Set<ReckonVersion> getParallelNormals() {
    return parallelNormals;
  }

  public Set<ReckonVersion> getClaimedVersions() {
    return claimedVersions;
  }
}
