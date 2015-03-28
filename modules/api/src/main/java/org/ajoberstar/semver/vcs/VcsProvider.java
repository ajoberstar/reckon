package org.ajoberstar.semver.vcs;

public interface VcsProvider<R> {
    R getVcs();
}
