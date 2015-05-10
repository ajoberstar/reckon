package org.ajoberstar.semver.vcs

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import spock.lang.Specification

class VersionersSpec extends Specification {
    private Vcs vcs = Mock()

    private Version doInfer(Versioner versioner) {
        return versioner.infer(Versioners.VERSION_0, vcs)
    }

    def 'rebuild fails if no current version'() {
        given:
        vcs.currentVersion >> Optional.empty()
        when:
        doInfer(Versioners.rebuild())
        then:
        thrown(IllegalStateException)
    }

    def 'rebuild works if current versions present in unmodified repo'() {
        given:
        vcs.currentVersion >> Optional.of(Version.forIntegers(1, 2, 3))
        expect:
        doInfer(Versioners.rebuild()) == Version.forIntegers(1, 2, 3)
    }

    def 'force fails if version is not a valid semver version'() {
        when:
        Versioners.force("blah blah")
        then:
        thrown(ParseException)
    }

    def 'force works for valid semver version'() {
        expect:
        doInfer(Versioners.force("1.2.3-beta.1")) == Version.valueOf("1.2.3-beta.1")
    }
}
