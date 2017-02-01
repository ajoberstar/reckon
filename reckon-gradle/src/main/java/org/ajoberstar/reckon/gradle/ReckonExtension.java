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
package org.ajoberstar.reckon.gradle;

import org.ajoberstar.reckon.core.NormalStrategy;
import org.ajoberstar.reckon.core.PreReleaseStrategy;
import org.ajoberstar.reckon.core.Reckoner;
import org.ajoberstar.reckon.core.VcsInventorySupplier;

public class ReckonExtension {
  private VcsInventorySupplier vcsInventory;
  private NormalStrategy normal;
  private PreReleaseStrategy preRelease;

  public void setVcsInventory(VcsInventorySupplier vcsInventory) {
    this.vcsInventory = vcsInventory;
  }

  public void setNormal(NormalStrategy normal) {
    this.normal = normal;
  }

  public void setPreRelease(PreReleaseStrategy preRelease) {
    this.preRelease = preRelease;
  }

  String reckonVersion() {
    return Reckoner.reckon(vcsInventory, normal, preRelease);
  }
}
