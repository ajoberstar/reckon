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
package org.ajoberstar.reckon.core.git;

import com.github.zafarkhaja.semver.Version;
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
import org.ajoberstar.reckon.core.VcsInventory;
import org.ajoberstar.reckon.core.VcsInventorySupplier;
import org.ajoberstar.reckon.core.Versions;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GitInventorySupplier implements VcsInventorySupplier {
  private static final Logger logger = LoggerFactory.getLogger(GitInventorySupplier.class);

  private final Repository repo;
  private final Function<Ref, Optional<Version>> tagParser;

  GitInventorySupplier(Repository repo) {
    this(repo, tagName -> Optional.of(tagName.replaceAll("^v", "")));
  }

  GitInventorySupplier(Repository repo, Function<String, Optional<String>> tagSelector) {
    this.repo = repo;
    this.tagParser =
        ref -> {
          String tagName = Repository.shortenRefName(ref.getName());
          return tagSelector.apply(tagName).flatMap(Versions::valueOf);
        };
  }

  @Override
  public VcsInventory getInventory() {
    try (RevWalk walk = new RevWalk(repo)) {
      walk.setRetainBody(false);

      ObjectId headObjectId = repo.getRefDatabase().getRef("HEAD").getObjectId();

      if (headObjectId == null) {
        logger.debug("No HEAD commit. Presuming repo is empty.");
        return new VcsInventory(null, null, null, null, 0, null, null);
      }

      logger.debug("Found HEAD commit {}", headObjectId);

      RevCommit headCommit = walk.parseCommit(headObjectId);

      Set<TaggedVersion> taggedVersions = getTaggedVersions(walk);

      logger.debug("Found tagged versions: {}", taggedVersions);

      Version currentVersion =
          findCurrent(headCommit, taggedVersions.stream())
              .map(TaggedVersion::getVersion)
              .orElse(null);
      TaggedVersion baseNormal =
          findBase(walk, headCommit, taggedVersions.stream().filter(TaggedVersion::isNormal));
      TaggedVersion baseVersion = findBase(walk, headCommit, taggedVersions.stream());

      int commitsSinceBase = RevWalkUtils.count(walk, headCommit, baseNormal.getCommit());

      Set<TaggedVersion> parallelCandidates =
          findParallelCandidates(walk, headCommit, taggedVersions);

      Set<RevCommit> taggedCommits =
          taggedVersions.stream().map(TaggedVersion::getCommit).collect(Collectors.toSet());
      Set<Version> parallelVersions =
          parallelCandidates
              .stream()
              .map(version -> findParallel(walk, headCommit, version, taggedCommits))
              // TODO Java 9 Optional::stream
              .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
              .collect(Collectors.toSet());

      Set<Version> claimedVersions =
          taggedVersions.stream().map(TaggedVersion::getVersion).collect(Collectors.toSet());

      return new VcsInventory(
          headObjectId.getName(),
          currentVersion,
          baseVersion.getVersion(),
          baseNormal.getVersion(),
          commitsSinceBase,
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
        .orElse(new TaggedVersion(Versions.VERSION_0, null));
  }

  private Set<TaggedVersion> findParallelCandidates(
      RevWalk walk, RevCommit head, Set<TaggedVersion> candidates) {
    return candidates
        .stream()
        .filter(candidate -> !doMergedInto(walk, head, candidate.getCommit()))
        .filter(candidate -> !doMergedInto(walk, candidate.getCommit(), head))
        .collect(Collectors.toSet());
  }

  private boolean doMergedInto(RevWalk walk, RevCommit base, RevCommit tip) {
    // TODO consider something like JGit's 24 hour clock skew to eliminate candidates
    // you know can't be see RevWalkUtils#findBranchesReachableFrom
    try {
      return walk.isMergedInto(base, tip);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Optional<Version> findParallel(
      RevWalk walk, RevCommit head, TaggedVersion candidate, Set<RevCommit> tagged) {
    try {
      walk.reset();
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(head);
      walk.markStart(candidate.getCommit());

      RevCommit mergeBase = walk.next();

      walk.reset();
      walk.setRevFilter(RevFilter.ALL);
      boolean taggedSinceMergeBase =
          RevWalkUtils.find(walk, head, mergeBase)
              .stream()
              .filter(tagged::contains)
              .findAny()
              .isPresent();

      if (mergeBase != null
          && !taggedSinceMergeBase
          && !mergeBase.equals(head)
          && !mergeBase.equals(candidate.getCommit())) {
        return Optional.of(Versions.getNormal(candidate.getVersion()));
      } else {
        return Optional.empty();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static class TaggedVersion {
    private final Version version;
    private final RevCommit commit;

    public TaggedVersion(Version version, RevCommit commit) {
      this.version = version;
      this.commit = commit;
    }

    public Version getVersion() {
      return version;
    }

    public RevCommit getCommit() {
      return commit;
    }

    public boolean isNormal() {
      return Versions.isNormal(version);
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
