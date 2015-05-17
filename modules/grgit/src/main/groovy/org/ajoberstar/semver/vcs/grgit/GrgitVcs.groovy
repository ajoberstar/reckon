package org.ajoberstar.semver.vcs.grgit

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import groovy.transform.Immutable
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.semver.vcs.Vcs

import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream

class GrgitVcs implements Vcs {
    private final Grgit git
    private final Function<Tag, Optional<Version>> tagParser

    public GrgitVcs(Grgit git) {
        this(git, { tag ->
            String name = tag.name.replaceAll(/^v/, '')
            try {
                return Optional.of(Version.valueOf(name))
            } catch (ParseException e) {
                return Optional.empty()
            }
        })
    }

    public GrgitVcs(Grgit git, Function<Tag, Optional<Version>> tagParser) {
        this.git = git
        this.tagParser = tagParser
    }

    @Override
    Optional<Version> getCurrentVersion() {
        Commit head = git.head()
        return getVersions { tag -> tag.commit == head }
            .findFirst()
    }

    @Override
    Optional<Version> getPreviousRelease() {
        Commit head = git.head()
        return getVersions { tag -> tag.commit == head || git.isAncestorOf(tag, head) }
            .filter { it.preReleaseVersion.empty }
            .findFirst()
    }

    @Override
    Optional<Version> getPreviousVersion() {
        Commit head = git.head()
        return getVersions { tag -> tag.commit == head || git.isAncestorOf(tag, head) }
                .findFirst()
    }

    private Stream<Version> getVersions(Predicate<Tag> filter) {
        return git.tag.list().stream()
            .filter(filter)
            .peek { tag -> println "Included $tag" }
            .map { tag -> toVersionTag(tag) }
            .flatMap { opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty() }
            .peek { vtag -> println "Valid version tag: $vtag"}
            .sorted(byAncestryThenVersion)
            .peek { vtag -> println "Sorted version tag: $vtag"}
            .map { it.version }
    }

    Optional<VersionTag> toVersionTag(Tag tag) {
        return tagParser.apply(tag).map { version -> new VersionTag(tag, version) }
    }

    @Immutable(knownImmutableClasses=[Version])
    private class VersionTag {
        Tag tag
        Version version
    }

    private final Comparator<Tag> byAncestry = { a, b ->
        if (a.commit == b.commit) {
            0
        } else if (git.isAncestorOf(a, b)) {
            -1
        } else if (git.isAncestorOf(b, a)) {
            1
        } else {
            0
        }
    }

    private final Comparator<VersionTag> byAncestryThenVersion =
            Comparator.comparing({ it.tag }, byAncestry).thenComparing({ it.version } as Function).reversed()
}
