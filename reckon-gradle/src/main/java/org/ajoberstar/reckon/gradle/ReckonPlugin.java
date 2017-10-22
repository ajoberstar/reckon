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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.ajoberstar.grgit.Grgit;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class ReckonPlugin implements Plugin<Project> {
  private static final String TAG_TASK = "reckonTagCreate";
  private static final String PUSH_TASK = "reckonTagPush";

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
              if (grgit != null) {
                extension.setVcsInventory(extension.git(grgit));
              }
            });

    DelayedVersion sharedVersion = new DelayedVersion(extension::reckonVersion);
    project.allprojects(
        prj -> {
          prj.setVersion(sharedVersion);
        });

    Task tag = createTagTask(project, extension);
    Task push = createPushTask(project, extension, tag);
    push.dependsOn(tag);
  }

  private Task createTagTask(Project project, ReckonExtension extension) {
    Task task = project.getTasks().create(TAG_TASK);
    task.setDescription("Tag version inferred by reckon.");
    task.setGroup("publishing");
    task.onlyIf(
        t -> {
          String version = project.getVersion().toString();
          // using the presence of build metadata as the indicator of taggable versions
          boolean insignificant = version.contains("+");
          // rebuilds shouldn't trigger a new tag
          boolean alreadyTagged =
              extension
                  .getGrgit()
                  .getTag()
                  .list()
                  .stream()
                  .anyMatch(tag -> tag.getName().equals(version));
          return !(insignificant || alreadyTagged);
        });
    task.doLast(
        t -> {
          Map<String, Object> args = new HashMap<>();
          args.put("name", project.getVersion());
          args.put("message", project.getVersion());
          extension.getGrgit().getTag().add(args);
        });
    return task;
  }

  private Task createPushTask(Project project, ReckonExtension extension, Task create) {
    Task task = project.getTasks().create(PUSH_TASK);
    task.setDescription("Push version tag created by reckon.");
    task.setGroup("publishing");
    task.onlyIf(t -> create.getDidWork());
    task.doLast(
        t -> {
          Map<String, Object> args = new HashMap<>();
          args.put("refsOrSpecs", Arrays.asList("refs/tags/" + project.getVersion().toString()));
          extension.getGrgit().push(args);
        });
    return task;
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
