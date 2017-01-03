/*
 * Copyright 2015-2016 the original author or authors.
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
package org.ajoberstar.reckon.gradle

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag
import org.ajoberstar.reckon.gradle.SemverExtension
import org.ajoberstar.reckon.core.GrgitVcs
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.function.Function

/**
 * Plugin providng Grgit functionality for SemverVcs.
 */
class GrgitVcsPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.pluginManager.apply('org.ajoberstar.grgit')
    project.pluginManager.apply(SemverVcsPlugin)

    SemverExtension semver = project.extensions.getByName('semver')
    GrgitVcsExtension grgit = semver.extensions.create('grgit', GrgitVcsExtension)

    semver.vcsSupplier = {
      if (grgit.tagParser) {
        new GrgitVcs(project.grgit, grgit.tagParser)
      } else {
        new GrgitVcs(project.grgit)
      }
    }
  }

  public static class GrgitVcsExtension {
    /**
     * The logic to use when parsing Grgit tags into Versions.
     */
    Function<Tag, Optional<Version>> tagParser
  }
}
