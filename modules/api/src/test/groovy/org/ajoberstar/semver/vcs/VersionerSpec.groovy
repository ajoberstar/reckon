package org.ajoberstar.semver.vcs

import static ChangeScope.*

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification

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

    def 'infer fails if null arguments'(ChangeScope scope, String stage) {
        when:
        versioner.infer(scope, stage, true)
        then:
        thrown(NullPointerException)
        where:
        scope   | stage
        null    | 'something'
        MAJOR   | null
    }

    def 'infer determines correct version'() {
        given:
        provider.previousVersion >> Optional.of(Version.valueOf('1.3.0-rc.3'))
        provider.previousRelease >> Optional.of(Version.valueOf('1.2.3'))
        expect:
        versioner.infer(scope, stage, fixed) == Version.valueOf(expected)
        where:
        scope | stage   | fixed | expected
        MAJOR | 'final' | true  | '2.0.0'
        MINOR | 'final' | true  | '1.3.0'
        PATCH | 'final' | true  | '1.2.4'
        MAJOR | 'other' | true  | '2.0.0-other.1'
        PATCH | 'other' | true  | '1.2.4-other.1'
        MINOR | 'other' | true  | '1.3.0-other.1'
        MINOR | 'rc'    | true  | '1.3.0-rc.4'
        MINOR | 'other' | false | '1.3.0-rc.3.other.1'
    }
}
