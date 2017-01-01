package org.ajoberstar.reckon.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.revwalk.filter.*;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;
import java.util.Objects;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.io.IOException;
import java.io.UncheckedIOException;

public class GitVcs implements Vcs {
    private static final Logger logger = LoggerFactory.getLogger(GitVcs.class);

    private final Repository repo;
    private final Function<Ref, Optional<Version>> tagParser;

    public GitVcs(Repository repo) {
        this(repo, ref -> {
            try {
                String tagName = Repository.shortenRefName(ref.getName());
                return Optional.of(Version.valueOf(tagName));
            } catch (ParseException e) {
                return Optional.empty();
            }
        });
    }

    public GitVcs(Repository repo, Function<Ref, Optional<Version>> tagParser) {
        this.repo = repo;
        this.tagParser = tagParser;
    }

    @Override
    public Optional<Version> getCurrentVersion() {
        return Optional.of(getBaseVersion())
            .filter(BaseVersion::isAnyHead)
            .map(BaseVersion::getAny);
    }

    @Override
    public Optional<Version> getPreviousRelease() {
        return Optional.of(getBaseVersion().getNormal());
    }

    @Override
    public Optional<Version> getPreviousVersion() {
        return Optional.of(getBaseVersion().getAny());
    }

    private BaseVersion getBaseVersion() {
        try (RevWalk walk = new RevWalk(repo)) {
            walk.setRetainBody(false);

            ObjectId headObjectId = repo.getRefDatabase().getRef("HEAD").getObjectId();
            RevCommit headCommit = walk.parseCommit(headObjectId);

            Set<GitVersion> versions = new HashSet<>();

            for (Ref ref : repo.getRefDatabase().getRefs(Constants.R_TAGS).values()) {
                Ref tag = repo.peel(ref);
                ObjectId objectId = tag.getPeeledObjectId();
                RevCommit revCommit = walk.parseCommit(objectId);
                Optional<GitVersion> maybeVersion = tagParser.apply(tag)
                    .map(version -> new GitVersion(version, revCommit));

                if (maybeVersion.isPresent()) {
                    GitVersion version = maybeVersion.get();
                    walk.reset();
                    walk.setRevFilter(RevFilter.MERGE_BASE);
                    walk.markStart(version.getRevCommit());
                    walk.markStart(headCommit);

                    RevCommit mergeBase = walk.next();

                    versions.add(version);
                    // if (!headCommit.equals(mergeBase)) {
                    //     Version tagged = version.getVersion();
                    //     Version normal = Version.forIntegers(tagged.getMajorVersion(), tagged.getMinorVersion(), tagged.getPatchVersion());
                    //     versions.add(new GitVersion(normal, mergeBase));
                    // }
                } else {
                    logger.debug("Tag {} could not be parsed as a version.", tag.getName());
                }
            }

            GitVersion any = findBase(walk, headCommit, versions.stream());
            GitVersion normal = findBase(walk, headCommit, versions.stream().filter(ver -> ver.getVersion().getPreReleaseVersion().isEmpty()));
            return new BaseVersion(normal.getVersion(), any.getVersion(), headCommit.equals(any.getRevCommit()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GitVersion findBase(RevWalk walk, RevCommit head, Stream<GitVersion> versions) {
        try {
            walk.reset();
            walk.setRevFilter(RevFilter.ALL);
            walk.markStart(head);

            Map<RevCommit, List<GitVersion>> versionsByRev = versions.collect(
                Collectors.groupingBy(GitVersion::getRevCommit));

            return StreamSupport.stream(walk.spliterator(), false)
                .flatMap(rev -> {
                    List<GitVersion> matches = versionsByRev.get(rev);
                    if (matches == null) {
                        return Stream.empty();
                    } else {
                        // Parents can't be "nearer". Exclude them to avoid extra walking.
                        Arrays.stream(rev.getParents()).forEach(parent -> {
                            try {
                                walk.markUninteresting(parent);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                        return matches.stream();
                    }
                }).max(Comparator.comparing(GitVersion::getVersion))
                .orElse(new GitVersion(Version.forIntegers(0, 0, 0), null));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class GitVersion {
        private final Version version;
        private final RevCommit revCommit;

        public GitVersion(Version version, RevCommit revCommit) {
            this.version = version;
            this.revCommit = revCommit;
        }

        public Version getVersion() {
            return version;
        }

        public RevCommit getRevCommit() {
            return revCommit;
        }

        public boolean equals(Object obj) {
            if (obj instanceof GitVersion) {
                GitVersion other = (GitVersion) obj;
                return Objects.equals(this.version, other.version)
                    && Objects.equals(this.revCommit, other.revCommit);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(version, revCommit);
        }
    }

    private static class BaseVersion {
        private final Version normal;
        private final Version any;
        private final boolean anyIsHead;

        public BaseVersion(Version normal, Version any, boolean anyIsHead) {
            this.normal = normal;
            this.any = any;
            this.anyIsHead = anyIsHead;
        }

        public Version getNormal() {
            return normal;
        }

        public Version getAny() {
            return any;
        }

        public boolean isAnyHead() {
            return anyIsHead;
        }

        public boolean equals(Object obj) {
            if (obj instanceof BaseVersion) {
                BaseVersion other = (BaseVersion) obj;
                return Objects.equals(this.normal, other.normal)
                    && Objects.equals(this.any, other.any)
                    && Objects.equals(this.anyIsHead, other.anyIsHead);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(normal, any, anyIsHead);
        }
    }
}
