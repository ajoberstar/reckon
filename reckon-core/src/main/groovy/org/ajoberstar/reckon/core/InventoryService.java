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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;

public final class InventoryService {
  private final Repository repo;
  private final Function<Ref, Optional<ReckonVersion>> tagParser;

  InventoryService(Repository repo, Function<String, Optional<String>> tagSelector) {
    this.repo = repo;
    this.tagParser =
        ref -> {
          String tagName = Repository.shortenRefName(ref.getName());
          return tagSelector.apply(tagName).flatMap(ReckonVersion::valueOf);
        };
  }

  public Inventory get() {
    try (RevWalk walk = new RevWalk(repo)) {
      walk.setRetainBody(false);

      ObjectId headObjectId = repo.getRefDatabase().getRef("HEAD").getObjectId();
      RevCommit headCommit = walk.parseCommit(headObjectId);

      Set<TaggedVersion> taggedVersions = getTaggedVersions(walk);

      ReckonVersion currentVersion =
          findCurrent(headCommit, taggedVersions.stream())
              .map(TaggedVersion::getVersion)
              .orElse(null);
      TaggedVersion baseRelease =
          findBase(walk, headCommit, taggedVersions.stream().filter(TaggedVersion::isNormal));
      TaggedVersion baseVersion = findBase(walk, headCommit, taggedVersions.stream());

      int commitsSinceBase = RevWalkUtils.count(walk, headCommit, baseVersion.getCommit());

      Set<ReckonVersion> parallelVersions =
          taggedVersions
              .stream()
              .map(version -> findParallel(walk, headCommit, version, taggedVersions))
              // TODO Java 9 Optional::stream
              .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
              .collect(Collectors.toSet());

      Set<ReckonVersion> claimedVersions =
          taggedVersions.stream().map(TaggedVersion::getVersion).collect(Collectors.toSet());

      return new Inventory(
          currentVersion,
          commitsSinceBase,
          baseRelease.getVersion(),
          baseVersion.getVersion(),
          parallelVersions,
          claimedVersions);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Set<TaggedVersion> getTaggedVersions(RevWalk walk) throws IOException {
    Set<TaggedVersion> versions = new HashSet<>();

    for (Ref ref : repo.getRefDatabase().getRefs(Constants.R_TAGS).values()) {
      Ref tag = repo.peel(ref);
      ObjectId objectId = tag.getPeeledObjectId();
      RevCommit commit = walk.parseCommit(objectId);
      tagParser
          .apply(tag)
          .ifPresent(
              version -> {
                versions.add(new TaggedVersion(version, commit));
              });
    }
    return versions;
  }

  private Optional<TaggedVersion> findCurrent(RevCommit head, Stream<TaggedVersion> versions) {
    return versions
        .filter(version -> version.getCommit().equals(head))
        .max(Comparator.comparing(TaggedVersion::getVersion));
  }

  private TaggedVersion findBase(RevWalk walk, RevCommit head, Stream<TaggedVersion> versions)
      throws IOException {
    walk.reset();
    walk.setRevFilter(RevFilter.ALL);
    walk.markStart(head);

    Map<RevCommit, List<TaggedVersion>> versionsByCommit =
        versions.collect(Collectors.groupingBy(TaggedVersion::getCommit));

    Stream.Builder<List<TaggedVersion>> builder = Stream.builder();

    for (RevCommit commit : walk) {
      List<TaggedVersion> matches = versionsByCommit.get(commit);
      if (matches != null) {
        // Parents can't be "nearer". Exclude them to avoid extra walking.
        for (RevCommit parent : commit.getParents()) {
          walk.markUninteresting(parent);
        }
        builder.accept(matches);
      }
    }

    return builder
        .build()
        .flatMap(List::stream)
        .max(Comparator.comparing(TaggedVersion::getVersion))
        .orElse(new TaggedVersion(ReckonVersion.VERSION_0, null));
  }

  private Optional<ReckonVersion> findParallel(
      RevWalk walk, RevCommit head, TaggedVersion candidate, Set<TaggedVersion> versions) {
    try {
      walk.reset();
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(head);
      walk.markStart(candidate.getCommit());

      // TODO I don't think this is right
      for (TaggedVersion other : versions) {
        if (!candidate.equals(other)) {
          walk.markUninteresting(other.getCommit());
        }
      }

      RevCommit mergeBase = walk.next();

      if (mergeBase != null
          && !mergeBase.equals(head)
          && !mergeBase.equals(candidate.getCommit())) {
        return Optional.of(candidate.getVersion().getNormal());
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static class TaggedVersion {
    private final ReckonVersion version;
    private final RevCommit commit;

    public TaggedVersion(ReckonVersion version, RevCommit commit) {
      this.version = version;
      this.commit = commit;
    }

    public ReckonVersion getVersion() {
      return version;
    }

    public RevCommit getCommit() {
      return commit;
    }

    public boolean isNormal() {
      return version.isNormal();
    }

    @Override
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
