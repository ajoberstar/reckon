# semver-vcs

[![Travis](https://img.shields.io/travis/ajoberstar/semver-vcs.svg?style=flat-square)](https://travis-ci.org/ajoberstar/semver-vcs)
[![Maintainer Status](http://stillmaintained.com/ajoberstar/semver-vcs.svg)](http://stillmaintained.com/ajoberstar/semver-vcs)
[![Download](https://api.bintray.com/packages/ajoberstar/libraries/org.ajoberstar%3Asemver-vcs/images/download.svg?style=flat-square)](https://bintray.com/ajoberstar/libraries/org.ajoberstar%3Asemver-vcs/_latestVersion)
[![GitHub license](https://img.shields.io/github/license/ajoberstar/semver-vcs.svg?style=flat-square)](https://github.com/ajoberstar/semver-vcs/blob/master/LICENSE)

## Introduction

semver-vcs provides an API for calculating a project's current version number based
on the state of the version control system (VCS) and (possibly) some user input. You
can think of it like a more flexible `git describe`.

### Rationale

- [Semantic versioning](http://semver.org) is a great specification and makes it
feasible to put meaningful tooling around versioning.
- Version control systems know your project's history and can provide most, and
in some cases all, of the information to determine your next version.
- Useful abstractions should live on their own, rather than hiding inside tooling-specific
plugins. This functionality originated in [gradle-git](https://github.com/ajoberstar/gradle-git),
but doesn't need to be specific to Gradle or Git.

### Supported VCSs

* [Git](http://git-scm.com/) (through [grgit](https://github.com/ajoberstar/grgit)) (in progress)

### Supported Tooling

* [Gradle](http://gradle.org/) (in progress)

## How to use?

For full documentation see the [wiki](https://github.com/ajoberstar/semver-vcs/wiki).

### From Gradle w/ Git

Apply the plugin:

```groovy
buildscript {
	repositories { jcenter() }
	dependencies { classpath 'org.ajoberstar:semver-vcs-gradle-grgit:<version>' }
}

apply plugin: 'org.ajoberstar.semver-vcs-grgit'

semver {
	// optionally configure how the version will be calculated
}
```

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

## Modules

**NOTE:** *All* modules require Java 8.

- [semver-vcs-api](http://ajoberstar.org/semver-vcs/docs/semver-vcs-api/javadoc) - Base API that tooling should use.
- [semver-vcs-grgit](http://ajoberstar.org/semver-vcs/docs/semver-vcs-grgit/groovydoc) - Implementation of a grgit backend.
- [semver-vcs-gradle-base](http://ajoberstar.org/semver-vcs/docs/semver-vcs-gradle-base/groovydoc) - Base Gradle plugin that
will calculate the project's version (given a VCS impl).
- [semver-vcs-gradle-grgit](http://ajoberstar.org/semver-vcs/docs/semver-gradle-grgit/groovydoc) - Extension of the base Gradle plugin to automatically configure a grgit VCS.

## How to implement?

* VCS providers should implement [Vcs](http://ajoberstar.org/semver-vcs/docs/semver-vcs-api/javadoc/org/ajoberstar/semver/vcs/Vcs.html).
* Gradle plugin authors should provide a plugin that applies `org.ajoberstar.semver-base` and configures the `semver.vcs` property.

See the reference documentation linked in the *Modules* section for more information.

## Have a question or found a bug?

Open an [issue](https://github.com/ajoberstar/semver-vcs/issues) describing your
situation. Stack traces and logs are always helpful.

## Contributing

Contributions are very welcome and should be submitted as pull requests.
For larger or more complex changes, please open an issue first to discuss the
approach.

## Acknowledgements

Thanks to [everyone](https://github.com/ajoberstar/gradle-git/graphs/contributors)
who contributed to previous iterations of this library and to
[Zafar Khaja](https://github.com/zafarkhaja) for the very helpful
[java-semver](https://github.com/zafarkhaja/java-semver) library.
