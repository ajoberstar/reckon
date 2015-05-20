package org.ajoberstar.semver.vcs;

import com.github.zafarkhaja.semver.Version;

import java.util.Objects;

@FunctionalInterface
public interface Versioner {
    Version infer(Version base, Vcs vcs);

    default Versioner compose(Versioner before) {
        Objects.requireNonNull(before);
        return (base, vcs) -> infer(before.infer(base, vcs), vcs);
    }

    default Versioner andThen(Versioner after) {
        Objects.requireNonNull(after);
        return after.compose(this);
    }
}
