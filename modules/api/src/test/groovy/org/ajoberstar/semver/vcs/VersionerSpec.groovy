package org.ajoberstar.semver.vcs

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification;

class VersionerSpec extends Specification {
    private VcsProvider provider = Mock()
    private Versioner versioner = new Versioner(provider)

    def 'rebuild fails if no current version'() {
        given:
        provider.currentVersion >> Optional.empty()
        when:
        versioner.rebuild()
        then:
        thrown(IllegalStateException)
    }

    def 'rebuild works if current versions present in unmodified repo'() {
        given:
        provider.currentVersion >> Optional.of(Version.forIntegers(1, 2, 3))
        expect:
        versioner.rebuild() == Version.forIntegers(1, 2, 3)
    }

    def 'infer fails if null arguments'(String scope, String stage) {
        when:
        versioner.infer(scope, stage, true)
        then:
        thrown(NullPointerException)
        where:
        scope   | stage
        null    | 'something'
        'major' | null
    }

    def 'infer determines correct version'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf(prevVersion))
        provider.previousRelease >> Optional.of(Version.valueOf(prevRelease))
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope   | stage   | fixed | prevVersion | prevRelease | expected
        'major' | 'final' | true  | '1.2.3'     | '1.2.3'     | '2.0.0'
        'minor' | 'final' | true  | '1.2.3'     | '1.2.3'     | '1.3.0'
        'patch' | 'final' | true  | '1.2.3'     | '1.2.3'     | '1.2.4'
    }
}
