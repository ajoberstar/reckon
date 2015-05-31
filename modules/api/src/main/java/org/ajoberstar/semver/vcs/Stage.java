/*
 * Copyright 2015 the original author or authors.
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
package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Arrays;
import java.util.Objects;

public final class Stage implements Comparable<Stage> {
    private static final String FINAL_NAME = "final";

    private final String name;
    private final Versioner versioner;

    public Stage(String name, Versioner versioner) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(versioner);
        this.name = name;
        this.versioner = versioner;
    }

    public String getName() {
        return name;
    }

    public Versioner getVersioner() {
        return versioner;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Stage) {
            Stage that = (Stage) obj;
            return this.getName().equals(that.getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public int compareTo(Stage that) {
        if (this.getName().equals(that.getName())) {
            return 0;
        } else if (this.getName().equals(FINAL_NAME)) {
            return 1;
        } else if (that.getName().equals(FINAL_NAME)) {
            return -1;
        } else {
            return this.getName().compareTo(that.getName());
        }
    }

    public static Stage finalStage() {
        return new Stage(FINAL_NAME, (base, vcs) -> Version.valueOf(base.getNormalVersion()));
    }

    public static Stage fixedStage(String name) {
        return new Stage(name, (base, vcs) -> {
            Version sanitized = sanitizeStage(base, name, true);
            String previousStage = sanitized.getPreReleaseVersion().split("\\.")[0];
            if (name.equals(previousStage)) {
                return sanitized.incrementPreReleaseVersion();
            } else {
                return base.setPreReleaseVersion(name).incrementPreReleaseVersion();
            }
        });
    }

    public static Stage floatingStage(String name) {
        return new Stage(name, (base, vcs) -> {
            Version sanitized = sanitizeStage(base, name, false);
            String[] prevStages = sanitized.getPreReleaseVersion().split("\\.");
            if (Arrays.binarySearch(prevStages, name) >= 0) {
                return sanitized.incrementPreReleaseVersion();
            } else if (name.compareTo(prevStages[0]) > 0) {
                return sanitized.setPreReleaseVersion(name).incrementPreReleaseVersion();
            } else {
                return sanitized.setPreReleaseVersion(sanitized.getPreReleaseVersion() + "." + name).incrementPreReleaseVersion();
            }
        });
    }

    public static Stage snapshotStage() {
        return new Stage("snapshot", (base, vcs) -> base.setPreReleaseVersion("SNAPSHOT"));
    }

    private static Version sanitizeStage(Version inferred, String stage, boolean fixed) {
        String[] rawIdents = inferred.getPreReleaseVersion().split("\\.");
        int rawEndIndex = getEndIndex(rawIdents, 0);
        int limit;
        if (stage.equals(rawIdents[0])) {
            limit = 2;
        } else if (!fixed) {
            if (rawIdents.length > 2 && stage.equals(rawIdents[2])) {
                limit = 4;
            } else {
                limit = 2;
            }
        } else {
            return Version.valueOf(inferred.getNormalVersion());
        }
        int endIndex = Math.min(rawEndIndex, limit);
        String[] validIdents = Arrays.copyOfRange(rawIdents, 0, endIndex);
        String preRelease = String.join(".", validIdents);
        if (preRelease == null || preRelease.isEmpty()) {
            return inferred;
        } else {
            return inferred.setPreReleaseVersion(preRelease);
        }
    }

    private static int getEndIndex(String[] array, int index) {
        if (array.length > index) {
            boolean isValid = index % 2 == 0 || array[index].chars().allMatch(Character::isDigit);
            return isValid ? getEndIndex(array, index + 1) : index;
        } else {
            return index;
        }
    }
}
