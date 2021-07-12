# reckon

![](https://github.com/ajoberstar/reckon/workflows/.github/workflows/build.yaml/badge.svg)

## Project News

### Retirement of Bintray/JCenter

This project was previously uploaded to JCenter, which is being retired by JFrog on May 1st 2021.

To allow continued acess to past versions, I've made a Maven repo available in [bintray-backup](https://github.com/ajoberstar/bintray-backup). Add the following to your repositories to use it.

```groovy
maven {
  name = 'ajoberstar-backup'
  url = 'https://ajoberstar.org/bintray-backup/'
}
```

Made possible by [lacasseio/bintray-helper](https://github.com/lacasseio/bintray-helper) in case you have a similar need to pull your old Bintray artifacts.

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
- `<stage>` an alphabetical identifier indicating a level of maturity on the way to a final release. They should make logical sense to a human, but alphabetical order **must** be the indicator of maturity to ensure they sort correctly. (e.g. alpha, beta, milestone, rc, snapshot, this latter doesn't add the `<num>`)
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

```groovy
plugins {
  id 'org.ajoberstar.reckon' version '<version>'
}

reckon {
  scopeFromProp()
  stageFromProp('milestone', 'rc', 'final')
  // alternative to stageFromProp
  // snapshotFromProp()
}
```

**NOTE:** Reckon overrides the `project.version` property in Gradle

#### Execute Gradle

Execute Gradle providing the properties, as needed:

- `reckon.scope` - one of `major`, `minor`, or `patch` (defaults to `minor`) to specify which component of the previous release should be incremented
- `reckon.stage`
  - (if you used `stageFromProp`) one of the values passed to `stageFromProp` (defaults to the first alphabetically) to specify what phase of development you are in
  - (if you used `snapshotFromProp`) either `snapshot` or `final` (defaults to `snapshot`) to specify what phase of development you are in
- `reckon.snapshot` - **deprecated** (if you used `snapshotFromProp`) one of `true` or `false` (defaults to `true`) to determine whether a snapshot should be made

When Gradle executes, the version will be inferred as soon as something tries to access it. This will be output to the console (as below).

```
./gradlew build -Preckon.scope=minor -Preckon.stage=milestone
Reckoned version 1.3.0-milestone.1
...
```

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
