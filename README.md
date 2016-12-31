# reckon

[![Bintray](https://img.shields.io/bintray/v/ajoberstar/maven/org.ajoberstar%3Areckon.svg?style=flat-square)](https://bintray.com/ajoberstar/maven/org.ajoberstar%3Areckon/_latestVersion)
[![Travis](https://img.shields.io/travis/ajoberstar/reckon.svg?style=flat-square)](https://travis-ci.org/ajoberstar/reckon)
[![Quality Gate](https://sonarqube.ajoberstar.com/api/badges/gate?key=org.ajoberstar:reckon)](https://sonarqube.ajoberstar.com/dashboard/index/org.ajoberstar:reckon)
[![GitHub license](https://img.shields.io/github/license/ajoberstar/reckon.svg?style=flat-square)](https://github.com/ajoberstar/reckon/blob/master/LICENSE)

## Why do you care?

### Get that version number out of my build file!

Most build tools and release systems require you to hardcode a version number into
a file in your source repository. This results in commit messages like "Bumping
version number.". Even if you don't have to do this manually, your release plugin
probably modifies your build file and commits the new version.

Git already contains tags with a version number pointing to a
specific commit, illustrating that power of this with the `git describe`
command that creates a version number based on the amount of change since the
previous tag (e.g. `v0.1.0-22-g26f678e`).

Git also contains branches for specific stages of development or maintenance
for a specific subset of versions.

With this much information available, there's little the user
should have to provide to get the next version number. And it certainly
doesn't need to be hardcoded anywhere.

### What does this version number mean?

[Semantic versioning](http://semver.org) is the best answer to this question so far.
It specifies a pretty stringent meaning for what a consumer of an API should expect
based on the difference between two versions numbers.

Additionally, it describes methods for encoding pre-release and build-metadata and
how those should be sorted by tools.

With that specification and some conventions related to encoding your stage of
development into the pre-release information, you can end up with a very
easy to understand versioning scheme.

For example, this API's scheme includes four stages:

- **final** (e.g. `1.0.0`) the fully-tested version ready for end-user consumption
- **rc** (e.g. `1.1.0-rc.1`) release candidates, versions believed to be ready for release with final testing
- **milestone** (e.g. `1.1.0-milestone.4`) versions containing a significant piece of functionality on the road
to the next version
- **dev** (e.g. `1.1.0-dev.2` or `1.1.0-milestone.4.dev.6`) development versions happening in-between more
formally defined stages (this is a *floating* stage, in reckon parlance)

## What is it?

reckon is two things:

- an API to infer your next version from a Git repository
- applications of that API in various tools (initially, just Gradle)

### Version Components

As specified by [SemVer](http://semver.org), a version consists of three primary
components: `<normal>[-<prerelease>][+<buildmetadata>]`.

- **normal** your typical `<major>.<minor>.<patch>` scheme
- **pre-release** any set of dot-separated alphanumeric identifiers used to indicate
progress towards the normal
- **build-metadata** any set of dot-separated alphanumeric identifiers used to indicate
information about the current build, which should not be used to sort versions

### Scope

reckon describes changes in the normal as the **scope** of the change, being
one of `major`, `minor`, or `patch`. The scope indicates which component of the
version should be incremented, with the components to the right being zeroed out.
For example, with a previous version of `1.2.3` a `minor` scope change would result
in `1.3.0`.

### Stage

In order to provide more structure to pre-release information and simplify the
minutiae of pre-release precedence, the concept of a **stage** of development is
used. Stages can be of a few different flavors:

- **final** a special stage indicating no pre-release information should be used
- **fixed** indicating significant steps along the way to the final release and will
always consist of `<stage name>.<incremented count>` (e.g. `beta.2`).
- **floating** indicating intermediate steps after another stage.
To comply with SemVer precedence rules a floating stage can result in:
	- `<stage name>.<incremented count>` as long as the stage name is of higher precedence
	than the previous stage. For example, if `milestone` was a floating stage and
	the prior version was `1.0.0-dev.1` it would result in `1.0.0-milestone.1`.
	- `<previous stage>.<stage name>.<incremented count>` if the stage name is of lower
	precedence than the previous stage. For example, if `dev` was a floating stage
	and the prior version was `1.0.0-milestone.2` it would result in `1.0.0-milestone.2.dev.1`.

For compatibility with Maven workflows, reckon also provides a `SNAPSHOT` stage
with no incrementing count.

### Build Metadata

Currently, build metadata is not a first-level concept in reckon, but that will
be addressed in [#13](https://github.com/ajoberstar/reckon/issues/13).

### Versioners

Even though reckon promotes the concepts of *scope* and *stage*, the API does not
depend on them. The actual inference is performed by a **versioner**, which is just
a function that takes both the version determined so far and the VCS being used and
returns the next version to use.

reckon provides versioner implementations optimized for scope and stage
schemes, but you are free to provide your own versioner function that produces
a valid semantic version in any way you please.

## Usage

**NOTE:** *All* reckon modules require Java 8 (or higher).

* [Release Notes](https://github.com/ajoberstar/reckon/releases)

### Gradle

Apply the plugin:

```groovy
buildscript {
	repositories { jcenter() }
	dependencies { classpath 'org.ajoberstar:reckon-gradle:<version>' }
}

apply plugin: 'org.ajoberstar.reckon'

reckon {
	// optionally configure how the version will be calculated
}
```

See [SemverVcsExtension](http://ajoberstar.org/reckon/docs/reckon-gradle-base/groovydoc/org/ajoberstar/semver/vcs/gradle/SemverExtension.html)
for details on the configuration options.

When you run Gradle, pass in any of the following properties to influence the version being inferred:

* `semver.scope` - one of `major`, `minor`, or `patch` to specify which component of the previous release should be incremented
* `semver.stage` - (by default, one of `final`, `rc`, `milestone`, or `dev`) to specify what phase of development you are in
* `semver.base` - for your first version, if you don't want to start from `0.0.0`
* `semver.force` - if you're in a bind and just need a specific version to be used

For example if the previous release was `1.2.4`:

```
./gradlew build -Psemver.scope=minor -Psemver.stage=milestone
Inferred version 1.3.0-milestone.1
...
```

### Direct API Usage

The four basic steps are:

1. Construct a [Version](https://github.com/zafarkhaja/jsemver/blob/master/src/main/java/com/github/zafarkhaja/semver/Version.java)
to use as a base, if one isn't found in the VCS.
1. Construct a [Vcs](http://ajoberstar.org/reckon/docs/reckon-api/javadoc/org/ajoberstar/semver/vcs/Vcs.html)
using one of the available providers.
1. Build a [Versioner](http://ajoberstar.org/reckon/docs/reckon-api/javadoc/org/ajoberstar/semver/vcs/Versioner.html)
which is a function of `(Version, Vcs) -> Version`. This does all of the work of inferring the version. The
[Versioners](http://ajoberstar.org/reckon/docs/reckon-api/javadoc/org/ajoberstar/semver/vcs/Versioners.html) class
provides some common `Versioner` implementations that can be composed as needed.
1. Call the `Versioner` with the base `Version` and the `Vcs`.

For example:

```java
import com.github.zafarkhaja.semver.Version;
import org.ajoberstar.semver.vcs.*;
import org.ajoberstar.semver.vcs.grgit.GrgitVcs;
import org.ajoberstar.grgit.Grgit;

Version base = Version.forIntegers(0, 0, 0);
Vcs vcs = new GrgitVcs(Grgit.open()));
Versioner versioner = Versioners.forScopeAndStage(Scope.MINOR, Stage.finalStage());
Version inferred = versioner.infer(base, vcs);
```

## Implementing

### Modules

- [reckon-api](http://ajoberstar.org/reckon/docs/reckon-api/javadoc) - Base API that tooling should use.
- [reckon-grgit](http://ajoberstar.org/reckon/docs/reckon-grgit/groovydoc) - Implementation of a grgit backend.
- [reckon-gradle-base](http://ajoberstar.org/reckon/docs/reckon-gradle-base/groovydoc) - Base Gradle plugin that
will calculate the project's version (given a VCS impl).
- [reckon-gradle-grgit](http://ajoberstar.org/reckon/docs/reckon-gradle-grgit/groovydoc) - Extension of the base Gradle plugin to automatically configure a grgit VCS.

### Implementing a Vcs

A `Vcs` implementation will literally just need to implement the
[Vcs](http://ajoberstar.org/reckon/docs/reckon-api/javadoc/org/ajoberstar/semver/vcs/Vcs.html) interface.

See the Grgit module in this repo for further guidance.

### Implementing a Tool

See the example in *Direct API Usage* above for a basic example and the implementation of the
Gradle modules in this repo as further guidance.

## Questions, Bugs, and Features

Please use the repo's [issues](https://github.com/ajoberstar/reckon/issues)
for all questions, bug reports, and feature requests.

## Contributing

Contributions are very welcome and are accepted through pull requests.

Smaller changes can come directly as a PR, but larger or more complex
ones should be discussed in an issue first to flesh out the approach.

If you're interested in implementing a feature on the
[issues backlog](https://github.com/ajoberstar/reckon/issues), add a comment
to make sure it's not already in progress and for any needed discussion.

## Acknowledgements

Thanks to [everyone](https://github.com/ajoberstar/gradle-git/graphs/contributors)
who contributed to previous iterations of this library and to
[Zafar Khaja](https://github.com/zafarkhaja) for the very helpful
[jsemver](https://github.com/zafarkhaja/jsemver) library.
