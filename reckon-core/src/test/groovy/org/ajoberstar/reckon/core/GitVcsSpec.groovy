/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ajoberstar.reckon.core

import java.nio.file.Files
import java.security.SecureRandom

import com.github.zafarkhaja.semver.Version

import org.ajoberstar.grgit.Grgit

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class GitVcsSpec extends Specification {
    @Shared File repoDir

    @Shared Grgit grgit

    @Shared SecureRandom random = new SecureRandom()

    def setupSpec() {
        repoDir = Files.createTempDirectory('repo').toFile()
        grgit = Grgit.init(dir: repoDir)

        commit()
        commit()
        addBranch('unreachable')

        commit()
        addTag('0.0.1-beta.3')
        addBranch('no-normal')

        commit()
        addTag('0.1.0')

        commit()
        addBranch('RB_0.1')

        commit()
        commit()
        addTag('0.2.0')

        checkout('RB_0.1')

        commit()
        addTag('0.1.1+2010.01.01.12.00.00')

        commit()
        commit()
        commit()
        commit()
        addTag('0.1.2-beta.1')
        addTag('0.1.2-alpha.1')

        commit()
        commit()
        commit()
        checkout('master')

        commit()
        addTag('1.0.0')
        addTag('1.0.0-rc.3')
        addBranch('RB_1.0')

        commit()
        addTag('1.1.0-rc.1+abcde')
    }

    def cleanupSpec() {
        assert !repoDir.exists() || repoDir.deleteDir()
    }

    @Unroll('getCurrentVersion on #head is #version')
    def 'getCurrentVersion'() {
        given:
        grgit.checkout(branch: head)
        expect:
        new GitVcs(grgit.repository.jgit.repository).currentVersion == version
        where:
        head          | version
        'master'      | Optional.of(Version.valueOf('1.1.0-rc.1+abcde'))
        'RB_0.1'      | Optional.empty()
        'RB_1.0'      | Optional.of(Version.valueOf('1.0.0'))
        'no-normal'   | Optional.of(Version.valueOf('0.0.1-beta.3'))
        'unreachable' | Optional.empty()
    }

    @Unroll('getPreviousVersion on #head is #version')
    def 'getPreviousVersion'() {
        given:
        grgit.checkout(branch: head)
        expect:
        new GitVcs(grgit.repository.jgit.repository).previousVersion == version
        where:
        head          | version
        'master'      | Optional.of(Version.valueOf('1.1.0-rc.1+abcde'))
        'RB_0.1'      | Optional.of(Version.valueOf('0.1.2-beta.1'))
        'RB_1.0'      | Optional.of(Version.valueOf('1.0.0'))
        'no-normal'   | Optional.of(Version.valueOf('0.0.1-beta.3'))
        'unreachable' | Optional.of(Version.valueOf('0.0.0'))
    }

    @Unroll('getPreviousRelease on #head is #version')
    def 'getPreviousRelease'() {
        given:
        grgit.checkout(branch: head)
        expect:
        new GitVcs(grgit.repository.jgit.repository).previousRelease == version
        where:
        head          | version
        'master'      | Optional.of(Version.valueOf('1.0.0'))
        'RB_0.1'      | Optional.of(Version.valueOf('0.1.1+2010.01.01.12.00.00'))
        'RB_1.0'      | Optional.of(Version.valueOf('1.0.0'))
        'no-normal'   | Optional.of(Version.valueOf('0.0.0'))
        'unreachable' | Optional.of(Version.valueOf('0.0.0'))
    }

    private void commit() {
        byte[] bytes = new byte[128]
        random.nextBytes(bytes)
        new File(grgit.repository.rootDir, '1.txt') << bytes
        grgit.add(patterns: ['1.txt'])
        def commit = grgit.commit(message: 'do')
        println "Created commit: ${commit.abbreviatedId}"
    }

    private void addBranch(String name) {
        def currentHead = grgit.head()
        def currentBranch = grgit.branch.current
        def newBranch = grgit.branch.add(name: name)
        def atCommit = grgit.resolve.toCommit(newBranch.fullName)
        println "Added new branch ${name} at ${atCommit.abbreviatedId}"
        assert currentBranch == grgit.branch.current
        assert currentHead == atCommit
    }

    private void addTag(String name) {
        def currentHead = grgit.head()
        def newTag = grgit.tag.add(name: name)
        def atCommit = grgit.resolve.toCommit(newTag.fullName)
        println "Added new tag ${name} at ${atCommit.abbreviatedId}"
        assert currentHead == atCommit
    }

    private void checkout(String name) {
        def currentHead = grgit.head()
        grgit.checkout(branch: name)
        def atCommit = grgit.resolve.toCommit(name)
        def newHead = grgit.head()
        println "Checkout out ${name}, which is at ${atCommit.abbreviatedId}"
        assert currentHead != grgit.head()
        assert atCommit == newHead
        assert name == grgit.branch.current.name
    }
}
