# reckon

[![CI](https://github.com/ajoberstar/reckon/actions/workflows/ci.yaml/badge.svg)](https://github.com/ajoberstar/reckon/actions/workflows/ci.yaml)

**NOTE:** As of 0.13.1, reckon is published to Maven Central

## Getting Help or Contributing

**IMPORANT:** Nowadays, I'm not spending a lot of time on maintenance due to other time commitments. While, I will eventually get to issues or PRs raised, **do not** expect a timely response. I'm not trying to be rude or dismissive, I only get back to this project periodically (on the order of _months_, in many cases). Please set your expectations appropriately as you file issues or open PRs.

Please use the repo's [issues](https://github.com/ajoberstar/reckon/issues) for all questions, bug reports, and feature requests.

## Why do you care?

### Get that version number out of my build file!

Most build tools and release systems require you to hardcode a version number into
a file in your source repository. This results in commit messages like "Bumping
version number". Even if you don't have to do this manually, your release plugin
probably modifies your build file and commits the new version.

Git already contains tags with a version number pointing to a
specific commit, illustrating that power of this with the `git describe`
command that creates a version number based on the amount of change since the
previous tag (e.g. v0.1.0-22-g26f678e).

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

For example, this API's scheme includes 3 stages:

- **final** (e.g. 1.0.0) the fully-tested version ready for end-user consumption
- **rc** (e.g. 1.1.0-rc.1) release candidates, versions believed to be ready for release after final testing
- **beta** (e.g. 1.1.0-beta.4) versions containing a significant piece of functionality on the road
  to the next version

## What is it?

Reckon is two things:

- an API to infer your next version from a Git repository
- applications of that API in various tools (initially, just Gradle)

Two schemes are provided to manage pre-release information.

- _Stages_ for a more structured approach which is a subset of [SemVer](http://semver.org).
- _Snapshots_ for the classic Maven approach to pre-release versions.

### Stage Version Scheme

There are three types of stages:

| Type              | Scheme                                                                | Example                | Description                                                                                                                     |
| ----------------- | --------------------------------------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **final**         | `<major>.<minor>.<patch>`                                             | `1.2.3`                | A version ready for end-user consumption                                                                                        |
| **significant**   | `<major>.<minor>.<patch>-<stage>.<num>`                               | `1.3.0-rc.1`           | A version indicating an important stage has been reached on the way to the next final release (e.g. alpha, beta, rc, milestone) |
| **insignificant** | `<major>.<minor>.<patch>-<stage>.<num>.<commits>+<hash or timestamp>` | `1.3.0-rc.1.8+3bb4161` | A general build in-between significant releases.                                                                                |

- `<major>` a postive integer incremented when incompatible API changes are made
- `<minor>` a positive integer incremented when functionality is added while preserving backwards-compatibility
- `<patch>` a positive integer incremented when fixes are made that preserve backwards-compatibility
- `<stage>` an alphabetical identifier indicating a level of maturity on the way to a final release. They should make logical sense to a human, but alphabetical order **must** be the indicator of maturity to ensure they sort correctly. (e.g. milestone, rc, snapshot would not make sense because snapshot would sort after rc)
- `<num>` a positive integer incremented when a significant release is made
- `<commits>` a positive integer indicating the number of commits since the last final release was made
- `<hash or timestamp>` if the repo is clean, an abbreviated commit hash of the current HEAD, otherwise a UTC timestamp

> **NOTE:** This approach is tuned to ensure it sorts correctly both with SemVer rules and Gradle's built in version sorting (which are subtly different).
>
> The version format is intentionally **not** configurable.

### Snapshot Version Scheme

Reckon can alternately use SNAPSHOT versions instead of the stage concept.

| Type         | Scheme                             | Example          | Description                                                |
| ------------ | ---------------------------------- | ---------------- | ---------------------------------------------------------- |
| **final**    | `<major>.<minor>.<patch>`          | `1.2.3`          | A version ready for end-user consumption                   |
| **snapshot** | `<major>.<minor>.<patch>-SNAPSHOT` | `1.3.0-SNAPSHOT` | An intermediate version before the final release is ready. |

## How do I use it?

**NOTE:** Check the [Release Notes](https://github.com/ajoberstar/reckon/releases) for details on compatibility and changes.

### Gradle

#### Apply the plugin

**IMPORTANT:** It is recommended to apply reckon as a Settings plugin (in settings.gradle/settings.gradle.kts) to ensure it is configured before any other plugin tries to use the project version.

```groovy

// if applying in settings.gradle(.kts)
plugins {
  id 'org.ajoberstar.reckon.settings' version '<version>'
}

// if applying in build.gradle(.kts)
plugins {
  id 'org.ajoberstar.reckon' version '<version>'
}

// in either case
reckon {
  // START As of 0.16.0
  // what stages are allowed
  stages('milestone', 'rc', 'final')
  // or use snapshots
  snapshots()
  
  // how do you calculate the scope/stage
  scopeCalc = calcScopeFromProp().or(calcScopeFromCommitMessages()) // fall back to commit message (see below) if no explicit prop set
  stageCalc = calcStageFromProp()
  // these can also be arbitrary closures (see code for details)
  scopeCalc = { inventory -> Optional.of(Scope.MAJOR) }
  stageCalc = { inventory, targetNormal -> Optional.of('beta') }
  
  // END As of 0.16.0
  
  // START LEGACY
  scopeFromProp()
  stageFromProp('milestone', 'rc', 'final')

  // alternative to stageFromProp
  // snapshotFromProp()
  // END LEGACY
  
  // omit this to use the default of 'minor'
  defaultInferredScope = 'patch'
  
  // omit to use default remote
  remote = 'other-remote'

  // omit this to use the default of parsing tag names of the form 1.2.3 or v1.2.3
  // this is a String to Optional<Version> function
  // return an empty optional for tags you don't consider a relevant version
  tagParser = tagName -> java.util.Optional.of(tagName)
    .filter(name -> name.startsWith("project-a/"))
    .map(name -> name.replace("project-a/", ""))
    .flatMap(name -> org.ajoberstar.reckon.core.Version.parse(name))

  // omit this to use the default of writing tag names of the form 1.2.3
  // this is a Version to String function
  tagWriter = version -> "project-a/" + version

  // omit this to use the default of tag messages including just the raw version, e.g. "1.2.3"
  tagMessage = version.map(v -> "Version " + v)
}
```

**NOTE:** Reckon overrides the `project.version` property in Gradle

#### Passing scope/stage as props

- `reckon.scope` (allowed if `scopeCalc` includes `calcStageFromProp()` or if you called `scopeFromProp()`)
  Valid values: `major`, `minor`, `patch` (if not set the scope is inferred by other means)
- `reckon.stage` (allowed if `stageCalc` includes `calcStageFromProp()`or if you used `stageFromProp()` or `snapshotFromProp()`)
  - For users of `stages()` or `stageFromProp()`:
    Valid values are any stage you listed via those methods. (if not set the stage is inferred by other means)
  - For users of `snapshots()` or `snapshotFromProp()`:
    Valid values: `snapshot` or `final`

When Gradle executes, the version will be inferred as soon as something tries to access it. This will be output to the console (as below).

```
./gradlew build -Preckon.scope=minor -Preckon.stage=milestone
Reckoned version 1.3.0-milestone.1
```

#### Reading scope from commit messages

If you want the scope to inferred in a more automated way, consider making use of a commit message convention. This sections describes the out-of-the-box convention supported by Reckon. Others are possible by customizing the `scopeCalc` further.

If your `scopeCalc` includes `calcScopeFromCommitMessages()`, the commit messages between your "base normal" (previous final release) and the current `HEAD` are parsed for SemVer indicators.

The general form is:

```
<scope>(optional area of codebase): rest of message

body is not used
```

Where `<scope>` is `major`, `minor`, or `patch` (must be lowercase).

The `(area)` is not used for any programmatic reasons, but could be used by other tools to categorize changes. 

Example that would be treated as a `Scope.MAJOR`:

```
major: Dropped support for Gradle 5

This is a breaking change reoving support for Gradle 5 due to use of a new feature in Gradle 6.
```

Example that would be treated as a `Scope.MINOR`:

```
minor(plugin): Dropped support for Gradle 5

This is a breaking change reoving support for Gradle 5 due to use of a new feature in Gradle 6.
```

Take this example commit log:

```
xzy1234 patch: other fixes
abc1234 (tag: 1.2.3) patch: fixed things
def1234 chore(docs): Documenting change to plugin application
ghi1234 (tag: 1.3.0-beta.1) minor: Adding property to override tag message
jkl1234 patch(extension): Fixed support for Provider in extension
mno1234 Other message not following convention
pqr1234 (HEAD -> main) major: Removed deprecated setNormal method
```

In this case we'd be looking at all commits since the last tagged final version, `1.2.3`. We'd only care about messages that follow our convention of prefixing the message with a scope. Since there's a mix of commits using all 3 scopes, we pick the most severe of the ones we found `major`.

##### Special Case for pre-1.0.0

Before 1.0.0, SemVer doesn't really guarantee anything, but a good practice seems to be a `PATCH` increment is for bug fixes, while a `MINOR` increase can be new features or breaking changes.

In order to promote the convention of using `major: My message` for breaking changes, before 1.0.0 a `major` in a commit message will be read as `minor`. The goal is to promote you explicitly documenting breaking changes in your commit logs, while requiring the actual 1.0.0 version bump to come via an override with `-Preckon.scope=major`.

##### DISCLAIMER this is not Convention Commits compliant

While this approach is similar to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), it does not follow their spec, sticking to something more directly applicable to Reckon's scopes. User's can use the `calcScopeFromCommitMessages(Function<String, Optional<Scope>>)` form if they want to implement Conventional Commits, or any other scheme themselves.

### Tagging and pushing your version

Reckon's Gradle plugin also provides two tasks:

- `reckonTagCreate` - Tags the current `HEAD` with the inferred version (the tag name will be the literal version, without a `v` prefix).
- `reckonTagPush` (depends on `reckonTagCreate`) - Pushes the tag to your branches upstream remote.

```
./gradlew reckonTagPush
```

It's suggested you add dependencies to these tasks to ensure your project is in the right state before tagging it. For example:

```
reckonTagCreate.dependsOn check
```

### Examples

See [How Reckon Works](docs/index.md), which includes examples of how reckon will behave in various scenarios.

## Finding versions of reckon

### Newest versions are on Maven Central

As of 0.13.1, reckon is published to Maven Central.

As of 0.13.2 reckon is no longer directly published to the Gradle Plugin Portal, but since the portal proxies Maven Central you can still access reckon through the portal. The only side effect is that [the portal](https://plugins.gradle.org/plugin/org.ajoberstar.reckon) will no longer list the latest version. Use this repo or [search.maven.org](https://search.maven.org/search?q=g:org.ajoberstar.reckon) to find the latest version.

### Old versions from Bintray/JCenter

This project was previously uploaded to JCenter, which was deprecated in 2021.

In the event that JCenter is unavailable and acess to past versions is needed, I've made a Maven repo available in [bintray-backup](https://github.com/ajoberstar/bintray-backup). Add the following to your repositories to use it.

```groovy
maven {
  name = 'ajoberstar-backup'
  url = 'https://ajoberstar.org/bintray-backup/'
}
```

Made possible by [lacasseio/bintray-helper](https://github.com/lacasseio/bintray-helper) in case you have a similar need to pull your old Bintray artifacts.

## Acknowledgements

Thanks to [everyone](https://github.com/ajoberstar/gradle-git/graphs/contributors)
who contributed to previous iterations of this library and to
[Zafar Khaja](https://github.com/zafarkhaja) for the very helpful
[jsemver](https://github.com/zafarkhaja/jsemver) library.
