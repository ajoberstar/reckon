package org.ajoberstar.reckon.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supplies an inventory of a Git repository.
 * <p>
 * This is intentionally package private.
 */
final class GitInventorySupplier implements VcsInventorySupplier {
  private static final Logger logger = LoggerFactory.getLogger(GitInventorySupplier.class);

  private final Repository repo;
  private final VersionTagParser tagParser;

  public GitInventorySupplier(Repository repo, VersionTagParser tagParser) {
    this.repo = repo;
    this.tagParser = tagParser;
  }

  @Override
  public VcsInventory getInventory() {
    // share this walk throughout to benefit from its caching
    try (var reader = repo.newObjectReader(); RevWalk walk = new RevWalk(reader)) {
      // saves on some performance as we don't really need the commit bodys
      walk.setRetainBody(false);

      var headObjectId = repo.getRefDatabase().findRef("HEAD").getObjectId();

      if (headObjectId == null) {
        logger.debug("No HEAD commit. Presuming repo is empty.");
        return new VcsInventory(null, isClean(), null, null, null, 0, null, null, null);
      }

      logger.debug("Found HEAD commit {}", headObjectId);

      var headCommit = walk.parseCommit(headObjectId);

      var taggedVersions = getTaggedVersions(walk);

      logger.debug("Found tagged versions: {}", taggedVersions);

      var currentVersion = findCurrent(headCommit, taggedVersions.stream())
          .map(TaggedVersion::getVersion)
          .orElse(null);
      var baseNormal = findBase(walk, headCommit, taggedVersions.stream().filter(TaggedVersion::isNormal));
      var baseVersion = findBase(walk, headCommit, taggedVersions.stream());

      var commitsSinceBase = RevWalkUtils.count(walk, headCommit, baseNormal.getCommit());

      var parallelCandidates = findParallelCandidates(walk, headCommit, taggedVersions);

      var taggedCommits = taggedVersions.stream().map(TaggedVersion::getCommit).collect(Collectors.toSet());
      var parallelVersions = parallelCandidates.stream()
          .map(version -> findParallel(walk, headCommit, version, taggedCommits))
          .flatMap(Optional::stream)
          .collect(Collectors.toSet());

      var claimedVersions = taggedVersions.stream().map(TaggedVersion::getVersion).collect(Collectors.toSet());

      var commitMessages = findCommitMessages(walk, headCommit, baseNormal.getCommit());

      return new VcsInventory(
          reader.abbreviate(headObjectId).name(),
          isClean(),
          currentVersion,
          baseVersion.getVersion(),
          baseNormal.getVersion(),
          commitsSinceBase,
          parallelVersions,
          claimedVersions,
          commitMessages);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean isClean() {
    try {
      var status = new Git(repo).status().call();
      if (!status.isClean()) {
        logger.info("Git repository is not clean: added={}, changed={}, removed={}, untracked={}, modified={}, missing={}",
            status.getAdded(),
            status.getChanged(),
            status.getRemoved(),
            status.getUntracked(),
            status.getModified(),
            status.getMissing());
      }
      return status.isClean();
    } catch (GitAPIException e) {
      logger.error("Failed to determine status of repository. Assuming not clean.", e);
      // TODO should this throw up?
      return false;
    }
  }

  private Set<TaggedVersion> getTaggedVersions(RevWalk walk) throws IOException {
    var versions = new HashSet<TaggedVersion>();

    for (var ref : repo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS)) {

      var tag = repo.getRefDatabase().peel(ref);
      // only annotated tags return a peeled object id
      var objectId = tag.getPeeledObjectId() == null ? tag.getObjectId() : tag.getPeeledObjectId();
      var commit = walk.parseCommit(objectId);

      var tagName = Repository.shortenRefName(ref.getName());
      tagParser.parse(tagName).ifPresent(version -> {
        versions.add(new TaggedVersion(version, commit));
      });
    }
    return versions;
  }

  private Optional<TaggedVersion> findCurrent(RevCommit head, Stream<TaggedVersion> versions) {
    return versions
        .filter(version -> version.getCommit().equals(head))
        // if multiple tags on the head commit, we want the highest precedence one
        .max(Comparator.comparing(TaggedVersion::getVersion));
  }

  private TaggedVersion findBase(RevWalk walk, RevCommit head, Stream<TaggedVersion> versions) throws IOException {
    walk.reset();
    walk.setRevFilter(RevFilter.ALL);
    walk.markStart(head);

    var versionsByCommit = versions.collect(Collectors.groupingBy(TaggedVersion::getCommit));

    Stream.Builder<List<TaggedVersion>> builder = Stream.builder();

    for (var commit : walk) {
      var matches = versionsByCommit.get(commit);
      if (matches != null) {
        // Parents can't be "nearer". Exclude them to avoid extra walking.
        for (var parent : commit.getParents()) {
          walk.markUninteresting(parent);
        }
        builder.accept(matches);
      }
    }

    return builder.build()
        .flatMap(List::stream)
        // if multiple versions are topologically equivalent (no version tag between them and the head on
        // their branch of history) ensure we pick the highest precedence one. Since we include its history,
        // inference must consider that the base
        .max(Comparator.comparing(TaggedVersion::getVersion))
        .orElse(new TaggedVersion(Version.IDENTITY, null));
  }

  private Set<TaggedVersion> findParallelCandidates(RevWalk walk, RevCommit head, Set<TaggedVersion> candidates) {
    return candidates.stream()
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

  private Optional<Version> findParallel(RevWalk walk, RevCommit head, TaggedVersion candidate, Set<RevCommit> tagged) {
    try {
      walk.reset();
      walk.setRevFilter(RevFilter.MERGE_BASE);
      walk.markStart(head);
      walk.markStart(candidate.getCommit());

      var mergeBase = walk.next();

      walk.reset();
      walk.setRevFilter(RevFilter.ALL);
      var taggedSinceMergeBase = RevWalkUtils.find(walk, head, mergeBase).stream()
          .anyMatch(tagged::contains);

      if (mergeBase != null
          && !taggedSinceMergeBase
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

  private List<String> findCommitMessages(RevWalk walk, RevCommit head, RevCommit base) {
    try {
      walk.reset();
      walk.setRevFilter(RevFilter.ALL);
      var messages = new ArrayList<String>();
      for (var commit : RevWalkUtils.find(walk, head, base)) {
        walk.parseBody(commit);
        messages.add(commit.getFullMessage());
      }
      return messages;
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
      return version.isFinal();
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
