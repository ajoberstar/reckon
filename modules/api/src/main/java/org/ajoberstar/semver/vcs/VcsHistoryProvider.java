package org.ajoberstar.semver.vcs;

public interface VcsHistoryProvider {
    int calculateDistanceBetween(VcsVersion from, VcsVersion to);
    int calculateDistanceFromCurrent(VcsVersion version);
}
