package org.ajoberstar.reckon.core

import java.nio.file.Files
import java.security.SecureRandom
import org.ajoberstar.grgit.Grgit
import spock.lang.Shared
import spock.lang.Specification

class GitInventorySupplierTest extends Specification {
  @Shared File repoDir

  @Shared Grgit grgit

  @Shared SecureRandom random = new SecureRandom()

  GitInventorySupplier supplier

  def 'commit id is abbreviated, not full'() {
    expect:
    supplier.getInventory().commitId == Optional.of(grgit.head().abbreviatedId)
  }

  def 'if HEAD has no tagged versions, current version is empty'() {
    given:
    checkout('head-untagged')
    expect:
    supplier.getInventory().currentVersion == Optional.empty()
  }

  def 'if single tagged version on HEAD, it is current version'() {
    given:
    checkout('head-single-tag')
    expect:
    supplier.getInventory().currentVersion == Version.parse('0.1.0-milestone.1')
  }

  def 'if multiple tagged version on HEAD, the max is current version'() {
    given:
    checkout('head-multi-tag')
    expect:
    supplier.getInventory().currentVersion == Version.parse('0.1.0')
  }

  def 'if no tagged finals in HEAD\'s history, base normal is 0.0.0'() {
    given:
    checkout('final-unreachable')
    expect:
    supplier.getInventory().baseNormal == Version.parse('0.0.0').get()
  }

  def 'if tagged finals in HEAD\'s history, base normal is max of finals which have no other final between them and HEAD'() {
    given:
    checkout('final-reachable')
    expect:
    supplier.getInventory().baseNormal == Version.parse('1.0.0').get()
  }

  def 'if tagged finals on head, base normal and version are same as current version'() {
    given:
    checkout('final-current')
    when:
    def inventory = supplier.getInventory()
    then:
    inventory.baseNormal == inventory.currentVersion.get()
    inventory.baseVersion == inventory.currentVersion.get()
  }

  def 'if no tagged versions in HEAD\'s history, base version is 0.0.0'() {
    given:
    checkout('version-unreachable')
    expect:
    supplier.getInventory().baseVersion == Version.parse('0.0.0').get()
  }

  def 'if tagged versions in HEAD\'s history, base version is max of versions which have no other version between them and HEAD'() {
    given:
    checkout('version-reachable')
    expect:
    supplier.getInventory().baseVersion == Version.parse('0.3.0-milestone.1').get()
  }

  def 'if tagged versions on head, base version is same as current version'() {
    given:
    checkout('version-current')
    when:
    def inventory = supplier.getInventory()
    then:
    inventory.baseVersion == inventory.currentVersion.get()
  }

  def 'if current is tagged with final, commits since base is 0'() {
    given:
    checkout('final-current')
    expect:
    supplier.getInventory().commitsSinceBase == 0
  }

  def 'if no reachable tagged finals, commits since base is size of log from HEAD'() {
    given:
    checkout('final-unreachable')
    expect:
    supplier.getInventory().commitsSinceBase == 4
  }

  def 'if reachable tagged finals, commits since base is size of log from HEAD excluding the base normal'() {
    given:
    checkout('final-reachable')
    expect:
    supplier.getInventory().commitsSinceBase == 10
  }

  def 'if no branches share merge base with HEAD, no parallel versions returned'() {
    given:
    checkout('parallel-no-base')
    expect:
    supplier.getInventory().parallelNormals == Collections.emptySet()
  }

  def 'if tagged version between HEAD and merge base, no parallel versions returned'() {
    given:
    checkout('parallel-tagged-since-merge')
    expect:
    supplier.getInventory().parallelNormals == Collections.emptySet()
  }

  def 'if no tagged version between HEAD and merge base, parallel versions returned'() {
    given:
    checkout('parallel-untagged-since-merge')
    expect:
    supplier.getInventory().parallelNormals == [Version.parse('0.2.0').get()] as Set
  }

  def 'all tagged versions treated as claimed versions'() {
    expect:
    supplier.getInventory().claimedVersions == [
      Version.parse('0.1.0-milestone.1').get(),
      Version.parse('0.1.0-rc.1').get(),
      Version.parse('0.1.0').get(),
      Version.parse('0.2.0-rc.1').get(),
      Version.parse('0.3.0-milestone.1').get(),
      Version.parse('0.3.0').get(),
      Version.parse('1.0.0').get()
    ] as Set
  }

  def 'if no commits, all results are empty'() {
    given:
    def emptyGrgit = Grgit.init(dir: Files.createTempDirectory('repo2').toFile())
    def emptySupplier = new GitInventorySupplier(emptyGrgit.repository.jgit.repository, VersionTagParser.getDefault())
    expect:
    emptySupplier.getInventory() == new VcsInventory(null, true, null, null, null, 0, null, null)
  }

  def setupSpec() {
    repoDir = Files.createTempDirectory('repo').toFile()
    grgit = Grgit.init(dir: repoDir)
    def initialBranch = grgit.branch.current().name

    commit()
    commit()
    commit()
    branch('version-unreachable')
    branch('head-untagged')

    commit()
    tag('0.1.0-milestone.1')
    branch('head-single-tag')
    branch('version-current')
    branch('final-unreachable')
    tag('not-a-version')

    commit()
    commit()
    tag('0.1.0-rc.1')
    tag('0.1.0')
    branch('final-current')
    branch('head-multi-tag')
    branch('parallel-no-base')

    commit()
    branch('RB_0.2')
    checkout('RB_0.2')
    commit()
    commit()
    tag('0.2.0-rc.1')
    commit()

    checkout(initialBranch)
    commit()
    branch('parallel-untagged-since-merge')
    commit()
    tag('0.3.0-milestone.1', false)
    commit()
    branch('parallel-tagged-since-merge')
    commit()
    commit()
    commit()
    commit()
    commit()
    commit()
    commit()
    merge('RB_0.2')
    branch('version-reachable')

    commit()
    branch('RB_1.0')
    checkout('RB_1.0')
    commit()
    tag('1.0.0')
    commit()
    commit()
    commit()
    commit()
    commit()
    commit()
    commit()

    checkout(initialBranch)
    commit()
    tag('0.3.0')
    commit()
    merge('RB_1.0')
    branch('final-reachable')

  }

  def cleanupSpec() {
    grgit.close()
    assert !repoDir.exists() || repoDir.deleteDir()
  }

  def setup() {
    supplier = new GitInventorySupplier(grgit.repository.jgit.repository, VersionTagParser.getDefault())
  }

  private void commit() {
    byte[] bytes = new byte[128]
    random.nextBytes(bytes)
    int fileName = random.nextInt();
    new File(grgit.repository.rootDir, "${fileName}.txt") << bytes
    grgit.add(patterns: ["${fileName}.txt"])
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

  private void tag(String name, boolean annotate = true) {
    def currentHead = grgit.head()
    def newTag = grgit.tag.add(name: name, annotate: annotate)
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

  private void merge(String name) {
    def currentBranch = grgit.branch.current.name
    grgit.merge(head: name)
    println "Merged ${name} into ${currentBranch}"
    assert currentBranch == grgit.branch.current.name
  }
}
