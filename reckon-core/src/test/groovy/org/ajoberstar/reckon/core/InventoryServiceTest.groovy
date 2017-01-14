/*
 * Copyright 2015-2017 the original author or authors.
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
import org.ajoberstar.grgit.Grgit
import spock.lang.Shared
import spock.lang.Specification

class InventoryServiceTest extends Specification {
  @Shared File repoDir

  @Shared Grgit grgit

  @Shared SecureRandom random = new SecureRandom()

  InventoryService service

  def 'if HEAD has no tagged versions, current version is empty'() {
    given:
    checkout('head-untagged')
    expect:
    service.get().currentVersion == Optional.empty()
  }

  def 'if single tagged version on HEAD, it is current version'() {
    given:
    checkout('head-single-tag')
    expect:
    service.get().currentVersion == Optional.of(ReckonVersion.valueOf('9.8.7'))
  }

  def 'if multiple tagged version on HEAD, the max is current version'() {
    given:
    checkout('head-multi-tag')
    expect:
    service.get().currentVersion == Optional.of(ReckonVersion.valueOf('9.8.7'))
  }

  def 'if no tagged finals in HEAD\'s history, base normal is empty'() {
    given:
    checkout('final-unreachable')
    expect:
    service.get().baseNormal == Optional.empty()
  }

  def 'if tagged finals in HEAD\'s history, base normal is max of finals which have no other final between them and HEAD'() {
    given:
    checkout('final-reachable')
    expect:
    service.get().baseNormal == Optional.of(ReckonVersion.valueOf('9.8.7'))
  }

  def 'if tagged finals on head, base normal and version are same as current version'() {
    given:
    checkout('final-current')
    when:
    def inventory = service.get()
    then:
    inventory.baseNormal == inventory.currentVersion
    inventory.baseVersion == inventory.currentVersion
  }

  def 'if no tagged versions in HEAD\'s history, base version is empty'() {
    given:
    checkout('version-unreachable')
    expect:
    service.get().baseVersion == Optional.empty()
  }

  def 'if tagged versions in HEAD\'s history, base version is max of versions which have no other version between them and HEAD'() {
    given:
    checkout('version-reachable')
    expect:
    service.get().baseVersion == Optional.of(ReckonVersion.valueOf('9.8.7'))
  }

  def 'if tagged versions on head, base version is same as current version'() {
    given:
    checkout('version-current')
    when:
    def inventory = service.get()
    then:
    inventory.baseVersion == inventory.currentVersion
  }

  def 'if current is tagged with final, commits since base is 0'() {
    given:
    checkout('final-current')
    expect:
    service.get().commitsSinceBase == 0
  }

  def 'if no reachable tagged finals, commits since base is size of log from HEAD'() {
    given:
    checkout('final-unreachable')
    expect:
    service.get().commitsSinceBase == 987
  }

  def 'if reachable tagged finals, commits since base is size of log from HEAD excluding the base normal'() {
    given:
    checkout('final-reachable')
    expect:
    service.get().commitsSinceBase == 987
  }

  def 'if no branches share merge base with HEAD, no parallel versions returned'() {
    given:
    checkout('parallel-no-base')
    expect:
    service.get().parallelNormals == Collections.emptySet()
  }

  def 'if tagged version between HEAD and merge base, no parallel versions returned'() {
    given:
    checkout('parallel-tagged-since-merge')
    expect:
    service.get().parallelNormals == Collections.emptySet()
  }

  def 'if no tagged version between HEAD and merge base, parallel versions returned'() {
    given:
    checkout('parallel-untagged-since-merge')
    expect:
    service.get().parallelNormals == [ReckonVersion.valueOf('9.8.7')] as Set
  }

  def 'all tagged versions treated as claimed versions'() {
    expect:
    service.get() == [ReckonVersion.valueOf('9.8.7')] as Set
  }

  def 'if no commits, all results are empty'() {
    given:
    def emptyGrgit = Grgit.init(dir: Files.createTempDirectory('repo2').toFile())
    def emptyService = new InventoryService(emptyGrgit.repository.jgit.repository)
    expect:
    emptyService.get() == new Inventory(null, 0, null, null, null, null)
  }

  def setupSpec() {
    repoDir = Files.createTempDirectory('repo').toFile()
    grgit = Grgit.init(dir: repoDir)

    commit()
    commit()
    commit()
    branch('version-unreachable')
    branch('head-untagged')
  }

  def cleanupSpec() {
    grgit.close()
    assert !repoDir.exists() || repoDir.deleteDir()
  }

  def setup() {
    service = new InventoryService(grgit.repository.jgit.repository)
  }

  private void commit() {
    byte[] bytes = new byte[128]
    random.nextBytes(bytes)
    new File(grgit.repository.rootDir, '1.txt') << bytes
    grgit.add(patterns: ['1.txt'])
    def commit = grgit.commit(message: 'do')
    println "Created commit: ${commit.abbreviatedId}"
  }

  private void branch(String name) {
    def currentHead = grgit.head()
    def currentBranch = grgit.branch.current
    def newBranch = grgit.branch.add(name: name)
    def atCommit = grgit.resolve.toCommit(newBranch.fullName)
    println "Added new branch ${name} at ${atCommit.abbreviatedId}"
    assert currentBranch == grgit.branch.current
    assert currentHead == atCommit
  }

  private void tag(String name) {
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
    println "Checkout out ${name}, which is at ${atCommit.abbreviatedId}"
    assert name == grgit.branch.current.name
  }
}
