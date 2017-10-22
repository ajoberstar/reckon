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
package org.ajoberstar.reckon.core.strategy

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.reckon.core.VcsInventory
import spock.lang.Specification

class SnapshotPreReleaseStrategyTest extends Specification {
  def 'if snapshot is false, return the target normal'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new SnapshotPreReleaseStrategy({ i, v -> false }).reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0')
  }

  def 'if snapshot is true, set pre-release to snapshto'() {
    given:
    def inventory = new VcsInventory(
        'abcdef',
        null,
        Version.valueOf('1.2.3-milestone.1'),
        Version.valueOf('1.2.2'),
        1,
        [] as Set,
        [] as Set
        )
    expect:
    new SnapshotPreReleaseStrategy({ i, v -> true }).reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0-SNAPSHOT')
    new SnapshotPreReleaseStrategy({ i, v -> null }).reckonTargetVersion(inventory, Version.valueOf('2.0.0')) == Version.valueOf('2.0.0-SNAPSHOT')
  }
}
