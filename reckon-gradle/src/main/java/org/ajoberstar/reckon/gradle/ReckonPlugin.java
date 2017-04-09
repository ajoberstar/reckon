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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.ajoberstar.grgit.Grgit;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ReckonPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    if (!project.equals(project.getRootProject())) {
      throw new IllegalStateException(
          "org.ajoberstar.reckon can only be applied to the root project.");
    }
    ReckonExtension extension =
        project.getExtensions().create("reckon", ReckonExtension.class, project);

    project
        .getPluginManager()
        .withPlugin(
            "org.ajoberstar.grgit",
            plugin -> {
              Grgit grgit = (Grgit) project.findProperty("grgit");
              extension.setVcsInventory(extension.git(grgit));
            });

    DelayedVersion sharedVersion = new DelayedVersion(extension::reckonVersion);
    project.allprojects(
        prj -> {
          prj.setVersion(sharedVersion);
        });
  }

  private static class DelayedVersion {
    private final Supplier<String> reckoner;

    public DelayedVersion(Supplier<String> reckoner) {
      this.reckoner = Suppliers.memoize(reckoner);
    }

    @Override
    public String toString() {
      return reckoner.get();
    }
  }
}
