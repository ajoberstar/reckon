package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

@FunctionalInterface
public interface Versioner {
    Version infer(Version base, Vcs vcs);
}
