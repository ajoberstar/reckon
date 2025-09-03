# Changelog

All notable changes to this project will be documented in this file.

The format is roughly based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] - 2025-09-02

Released via SourceHut. No functionality changes.

## [1.0.0] - 2025-08-26

It's past time that reckon should be considered stable, so support for Gradle 9 seemed like as good a time as any to make the bump.

In terms of real changes, this release only contains dependency updates.

Thanks to [fml2](https://github.com/fml2) for a typo fix in the docs.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- Dependency updates

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.6, 8.0.2, 8.14.3 |
| 17                 | 7.3.3, 7.6.6, 8.0.2, 8.14.3, 9.0.0, 9.1.0-rc-1 |
| 21                 | 8.4, 8.14.3, 9.0.0, 9.1.0-rc-1 |

## [0.19.2] - 2025-04-14

This release tries to resolve configuration cache support (yet again). This time with a workaround for JGit's use of the git executable to detect system configuration files. As a consequence, system configuration will not be read while reckon is calculating versions.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- #202 Fix configuration cache support

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.4, 8.0.2, 8.13, 8.14-rc-1, 9.0-milestone-1 |
| 17                 | 7.3.3, 7.6.4, 8.0.2, 8.13, 8.14-rc-1, 9.0-milestone-1 |
| 21                 | 8.4, 8.13, 8.14-rc-1, 9.0-milestone-1 |

## [0.19.1] - 2024-11-24

*NOTE:* 0.19.1 is the same as 0.19.0. There was a publishing issue preventing 0.19.0 from getting out to Central.

This release makes commit message scope suppliers more flexible.

Use of `scopeSupplier = calcScopeFromCommitMessages()` allowed specifying the scope with a commit message like `major: My Message` or `patch: My message`. However, there was a special case when the project hadn't reached 1.0.0 yet.

Before a project reaches 1.0.0, `major:` prefixes were downgraded to `minor`. The intent was to encourage breaking changes to still be committed as `major:` for consistency, but respecting that reaching 1.0.0 is a more significant decision than just the first breaking change introduced into your code.

The downside was that the only alternative to get to 1.0.0 was to use `-Preckon.scope=major` to supersede what the commit message supplier decided. This can be incompatible with many workflows that don't allow user interaction when reckon runs.

With this release:

- `scopeSupplier = calcScopeFromCommitMessages()` now additionally supports a `major!:` prefix which ignores whether the project is pre-1.0.0 and forces use of the major scope
- Introduces `scopeSupplier = calcScopeFromCommitMessageParser(BiFunction)` which allows custom logic to consider both the commit message and whether the project is pre-1.0.0 when it makes it's scope decision
- `scopeSupplier = calcScopeFromCommitMessages(Function)` continues to use the same pre-1.0.0 behavior when a major scope is returned for backwards compatibility

### Breaking Changes

_None_

### Enhancements

- #205 Allow commit message scope suppliers to bump version to v1.0.0

### Fixes

- Dependency updates

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.4, 8.0.2, 8.11.1 |
| 17                 | 7.3.3, 7.6.4, 8.0.2, 8.11.1 |
| 21                 | 8.4, 8.11.1 |

## [0.19.0] - 2024-11-24

This release makes commit message scope suppliers more flexible.

Use of `scopeSupplier = calcScopeFromCommitMessages()` allowed specifying the scope with a commit message like `major: My Message` or `patch: My message`. However, there was a special case when the project hadn't reached 1.0.0 yet.

Before a project reaches 1.0.0, `major:` prefixes were downgraded to `minor`. The intent was to encourage breaking changes to still be committed as `major:` for consistency, but respecting that reaching 1.0.0 is a more significant decision than just the first breaking change introduced into your code.

The downside was that the only alternative to get to 1.0.0 was to use `-Preckon.scope=major` to supersede what the commit message supplier decided. This can be incompatible with many workflows that don't allow user interaction when reckon runs.

With this release:

- `scopeSupplier = calcScopeFromCommitMessages()` now additionally supports a `major!:` prefix which ignores whether the project is pre-1.0.0 and forces use of the major scope
- Introduces `scopeSupplier = calcScopeFromCommitMessageParser(BiFunction)` which allows custom logic to consider both the commit message and whether the project is pre-1.0.0 when it makes it's scope decision
- `scopeSupplier = calcScopeFromCommitMessages(Function)` continues to use the same pre-1.0.0 behavior when a major scope is returned for backwards compatibility

### Breaking Changes

_None_

### Enhancements

- #205 Allow commit message scope suppliers to bump version to v1.0.0

### Fixes

- Dependency updates

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.4, 8.0.2, 8.11.1 |
| 17                 | 7.3.3, 7.6.4, 8.0.2, 8.11.1 |
| 21                 | 8.4, 8.11.1 |

## [0.18.3] - 2024-02-18

Dependency updates. The [zafarkhaja/semver](https://github.com/zafarkhaja/jsemver) library had a bunch of breaking changes, but it doesn't appear that any of those will affect the outward behavior. Please open an issue if you find otherwise.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- Dependency updates

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.4, 8.0.2, 8.6 |
| 17                 | 7.3.3, 7.6.4, 8.0.2, 8.6 |
| 21                 | 8.4, 8.6 |

## [0.18.2] - 2023-12-30

Minor improvement for people interacting with the tasks during another settings plugin. Also now tested against Gradle 8.5.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- #200 Register tag/push tasks earlier to make settings plugins easier

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.1, 8.0.2, 8.5, 8.6-rc-1 |
| 17                 | 7.3.3, 7.6.1, 8.0.2, 8.5, 8.6-rc-1 |
| 21                 | 8.4, 8.5, 8.6-rc-1 |

## [0.18.1] - 2023-10-21

Small patch release to update dependencies and resolve a deprecation.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- #199 Update Gradle and dependencies
- #198 Remove use of deprecated `forUseAtConfigurationTime` function from Gradle

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.1, 8.0.2, 8.4 |
| 17                 | 7.3.3, 7.6.1, 8.0.2, 8.4 |
| 21                 | 8.4 |

## [0.18.0] - 2023-04-22

This release decouples reckon from [grgit](https://github.com/ajoberstar/grgit/), using direct `JGit` for version inference and CLI Git for tag creation and pushes. The motivation is selfishly just to simplify maintenance of reckon.

For most cases, this should be a transparent change, but it does have a few potentially user-facing effects:

- You have to have `git` installed (which you almost certainly do)
- If you use `reckonTagPush`, you must have your credentials set up already in some way that won't require a prompt (basic auth creds, SSH agent, whatever). The old `org.ajoberstar.grgit.*` properties or `GRGIT_*` environment variables are no longer used.

Generally, we will be using the current user's identity (`user.email` and `user.name`) to create the tag. In cases where that information is not present (for example, in GitHub Actions), we'll use the author identity from the most recent commit.

This release contains a couple other small changes as well, see below.

### Breaking Changes

- #196 grgit is completely removed from reckon (affecting authentication and tagging)
- #195 `defaultInferredScope` no longer defaults to `minor` and must be set explicitly

### Enhancements

- #175 The `Scope` enum is now accepted for `defaultInferredScope` and `parallelBranchScope`

### Fixes

- #91 You can run your build with `--info` to see status details from JGit if reckon fails due to an unclean repo

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.1, 8.0.2, 8.1.1 |
| 17                 | 7.3.3, 7.6.1, 8.0.2, 8.1.1 |

## [0.17.0] - 2023-04-22

This release implements a new `org.ajoberstar.reckon.settings` plugin that can be used as an alternative to `org.ajoberstar.reckon`. This is applied in a `settings.gradle` instead of a `build.gradle` but is otherwise configured the same. By applying to Settings, it will run and be configured before any projects are evaluated, which should avoid the timing issues uncovered in #147. The settings plugin does not have the same "soft-fail" workaround that the project plugin has, as it should not be needed.

Additionally we have improvements to version inference logic. Our prior parallel version logic allowed reckon to increment by the requested scope a second time in order to avoid a parallel version. However, if that version is also in the parallel branch, it would fail saying the version was already claimed.

In the new logic, you are able to set a `parallelBranchScope` to indicate how you use your parallel branches. For example, people use branches like `maintenance/1.2.x` should set it to `MINOR`. Users of branches like `maintenance/2.x` should set it to MAJOR.

When reckon identifies a conflict with a parallel branch, it will increment by the _greater_ of the user-provided scope and `parallelBranchScope`.

```text
 O abc123
 O abc124 (v1.2.0)
 | \
 |  O abc125 (v1.2.1)
 O abc126
```

In the old logic, commit `abc126` would infer as `1.2.2-alpha.0.1+abc126` where in the new logic (with `parallelBranchScope=MINOR`) it would infer as `1.3.0-alpha.0.1+abc126`.

### Breaking Changes

- #183 Insignificant versions will never use the parallel version avoidance logic. This is mainly targeted to benefit feature branches that aren't up to date with your main branch, however it can also affect the main branch if it hasn't been tagged since a parallel branch was created and tagged.

### Enhancements

- New `org.ajoberstar.reckon.settings` plugin that can be applied in `settings.gradle` as an alternative to the normal plugin. This ensures reckon gets configured before project plugins.

### Fixes

- #180 Parallel branch with two released versions will cause failure due to claimed version
- #194 Version inference exceptions are no longer buried by the "soft-fail" logic in the project plugin

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.6.1, 8.0.2, 8.1.1 |
| 17                 | 7.3.3, 7.6.1, 8.0.2, 8.1.1 |

## [0.17.0-beta.4] - 2022-08-19

This release implements a new `org.ajoberstar.reckon.settings` plugin that can be used as an alternative to `org.ajoberstar.reckon`. This is applied in a `settings.gradle` instead of a `build.gradle` but is otherwise configured the same. By applying to Settings, it will run and be configured before any projects are evaluated, which should avoid the timing issues uncovered in #147.

Additionally we have improvements to version inference logic. Our prior parallel version logic allowed reckon to increment by the requested scope a second time in order to avoid a parallel version. However, if that version is also in the parallel branch, it would fail saying the version was already claimed.

In the new logic, you are able to set a `parallelBranchScope` to indicate how you use your parallel branches. For example, people use branches like `maintenance/1.2.x` should set it to `MINOR`. Users of branches like `maintenance/2.x` should set it to MAJOR.

When reckon identifies a conflict with a parallel branch, it will increment by the _greater_ of the user-provided scope and `parallelBranchScope`.

```text
 O abc123
 O abc124 (v1.2.0)
 | \
 |  O abc125 (v1.2.1)
 O abc126
```

In the old logic, commit `abc126` would infer as `1.2.2-alpha.0.1+abc126` where in the new logic (with `parallelBranchScope=MINOR`) it would infer as `1.3.0-alpha.0.1+abc126`.

### Breaking Changes

- #183 Insignificant versions will never use the parallel version avoidance logic. This is mainly targeted to benefit feature branches that aren't up to date with your main branch, however it can also affect the main branch if it hasn't been tagged since a parallel branch was created and tagged.

### Enhancements

- New `org.ajoberstar.reckon.settings` plugin that can be applied in `settings.gradle` as an alternative to the normal plugin. This ensures reckon gets configured before project plugins.

### Fixes

- #180 Parallel branch with two released versions will cause failure due to claimed version

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.5.1 |
| 17                 | 7.3.3, 7.5.1 |

## [0.17.0-beta.3] - 2022-08-17

This release implements a new `org.ajoberstar.reckon.settings` plugin that can be used as an alternative to `org.ajoberstar.reckon`. This is applied in a `settings.gradle` instead of a `build.gradle` but is otherwise configured the same. By applying to Settings, it will run and be configured before any projects are evaluated, which should avoid the timing issues uncovered in #147.

Additionally we have improvements to version inference logic. Our prior parallel version logic allowed reckon to increment by the requested scope a second time in order to avoid a parallel version. However, if that version is also in the parallel branch, it would fail saying the version was already claimed.

In the new logic, you are able to set a `parallelBranchScope` to indicate how you use your parallel branches. For example, people use branches like `maintenance/1.2.x` should set it to `MINOR`. Users of branches like `maintenance/2.x` should set it to MAJOR.

When reckon identifies a conflict with a parallel branch, it will increment by the _greater_ of the user-provided scope and `parallelBranchScope`.

```text
 O abc123
 O abc124 (v1.2.0)
 | \
 |  O abc125 (v1.2.1)
 O abc126
```

In the old logic, commit `abc126` would infer as `1.2.2-alpha.0.1+abc126` where in the new logic (with `parallelBranchScope=MINOR`) it would infer as `1.3.0-alpha.0.1+abc126`.

### Breaking Changes

_None_

### Enhancements

- New `org.ajoberstar.reckon.settings` plugin that can be applied in `settings.gradle` as an alternative to the normal plugin. This ensures reckon gets configured before project plugins.

### Fixes

- #180 Parallel branch with two released versions will cause failure due to claimed version

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.5.1 |
| 17                 | 7.3.3, 7.5.1 |

## [0.17.0-beta.2] - 2022-08-14

This release implements a new `org.ajoberstar.reckon.settings` plugin that can be used as an alternative to `org.ajoberstar.reckon`. This is applied in a `settings.gradle` instead of a `build.gradle` but is otherwise configured the same. By applying to Settings, it will run and be configured before any projects are evaluated, which should avoid the timing issues uncovered in #147.

Additionally we have improvements to version inference logic. Our prior parallel version logic allowed reckon to increment by the requested scope a second time in order to avoid a parallel version. However, if that version is also in the parallel branch, it would fail saying the version was already claimed.

In the new logic, if we bump the target normal in order to avoid a parallel version and that version is still in the parallel versions
list, we increment by a higher scope (i.e. by MAJOR if they requested MINOR or by MINOR if they requested PATCH).

This may resolve many of the bugs we had with parallel version handling.

The two unintuitive parts are that it may still not increment as far as someone wants in some cases. And in others someone could be surprised that we incremented by a higher scope than they asked for.

To deal with the latter, we may want to consider making a distinction between "soft" and "hard" scopes (i.e. did they explicitly ask for the scope or did it get inferred). This was clearer in the past, when "inferred" really only meant no input from the scope calc. However, with the new commit message scope calc, that's really more of a "soft" scope request than an explicit one. It's trickier because to the Reckoner there's no difference between commit message scope calcs and explicit user-requested scope calcs.

### Breaking Changes

- #181 results in cases where reckon may increment with a higher scope than the user provided in order to avoid parallel versions. In past versions, this would have failed instead saying the version was already claimed

### Enhancements

- New `org.ajoberstar.reckon.settings` plugin that can be applied in `settings.gradle` as an alternative to the normal plugin. This ensures reckon gets configured before project plugins.

### Fixes

- #180 Parallel branch with two released versions will cause failure due to claimed version

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.5.1 |
| 17                 | 7.3.3, 7.5.1 |

## [0.17.0-beta.1] - 2022-08-14

Our prior parallel version logic allowed reckon to increment by the requested scope a second time in order to avoid a parallel
version. However, if that version is also in the parallel branch, it would fail saying the version was already claimed.

In the new logic, if we bump the target normal in order to avoid a parallel version and that version is still in the parallel versions
list, we increment by a higher scope (i.e. by MAJOR if they requested MINOR or by MINOR if they requested PATCH).

This may resolve many of the bugs we had with parallel version handling.

The two unintuitive parts are that it may still not increment as far as someone wants in some cases. And in others someone could be surprised that we incremented by a higher scope than they asked for.

To deal with the latter, we may want to consider making a distinction between "soft" and "hard" scopes (i.e. did they explicitly ask for the scope or did it get inferred). This was clearer in the past, when "inferred" really only meant no input from the scope calc. However, with the new commit message scope calc, that's really more of a "soft" scope request than an explicit one. It's trickier because to the Reckoner there's no difference between commit message scope calcs and explicit user-requested scope calcs.

### Breaking Changes

- #181 results in cases where reckon may increment with a higher scope than the user provided in order to avoid parallel versions. In past versions, this would have failed instead saying the version was already claimed

### Enhancements

- _None_

### Fixes

- #180 Parallel branch with two released versions will cause failure due to claimed version

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.5.1 |
| 17                 | 7.3.3, 7.5.1 |

## [0.16.1] - 2022-02-18

This provides a fix for NullPointerExceptions that have become more common with changes in Gradle 7.4 that stem from evaluation order issues and other plugins that call `project.getVersion().toString()` at configuration time. This allows builds to work, though there is some small risk that some config in the project will have the incorrect version `unspecified` instead of the desired version reckon would calculate.

In the cases where you see the following warning:

```
Project version evaluated before reckon was configured. Run with --info to see cause.
```

You can rerun the build with `--info` to see the exception that would have been thrown in prior versions. This can help you track down which plugin is too eagerly evaluating the version, in case you want to try to fix that.

### Breaking Changes

_None_

### Enhancements

- _None_

### Fixes

- #147 and #174 Soft fail if reckon extension isn't configured instead of throwing an exception

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.16.0] - 2022-02-12

The highlight of 0.16.0 is support for scope inference via commit messages. See the [README](https://github.com/ajoberstar/reckon#reading-scope-from-commit-messages) for details.

General commit message format reckon looks for:

```
<scope>(optional area of codebase): rest of message

body is not used
```

Example:

```
major: Dropped support for Gradle 5

This is a breaking change reoving support for Gradle 5 due to use of a new feature in Gradle 6.
```

This feature is not limited to this convention alone, but this is the one I made easy to turn on.

### Breaking Changes

_None_

### Enhancements

- #172 Supports inferring scope from commit messages
- #72 Expose custom strategy methods for scope and stage calculation

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.15.0] - 2022-02-12

Many new configuration options on the `ReckonExtension` addressing long asked for features.

Thanks to [Maksim Kostromin](https://github.com/daggerok) for adding documentation of the authentications options.

### Breaking Changes

_None_

### Enhancements

- #54 Added `reckon.tagParser` and `reckon.tagWriter` to customize how versions are found from tags and named when created. This can be useful for monrepos.
- #158 Added `reckon.tagMessage` to customize how tag messages are created
- #115 Added `reckon.defaultInferredScope` to customize what scope is used if no input is provided
- #89 Added `reckon.remote` to override which remote the tag is pushed to

### Fixes

- #97 Grgit is applied via class now, which enables using reckon from a script plugin

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.14.0] - 2022-02-10

Upgrades to Grgit 5 and supports Gradle's configuration cache

### Breaking Changes

- Upgrades to Grgit 5
- Drops support for Java 8 (due to Grgit upgrade)
- Drops support for Gradle <7
- Removes deprecated `reckon.setNormal()` and `reckon.setStage()` methods

### Enhancements

- Supports `--configuration-cache`
- Reckon extension now has a `Provider<Version>` to allow access to the metadata of the version #156

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.14.0-rc.1] - 2022-02-10

Upgrades to Grgit 5 and supports Gradle's configuration cache

### Breaking Changes

- Upgrades to Grgit 5
- Drops support for Java 8 (due to Grgit upgrade)
- Drops support for Gradle <7
- Removes deprecated `reckon.setNormal()` and `reckon.setStage()` methods

### Enhancements

- Supports `--configuration-cache`
- Reckon extension now has a `Provider<Version>` to allow access to the metadata of the version #156

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 11                 | 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.13.2] - 2022-02-09

Drop publishing directly to Gradle plugin portal to _really_ ensure POM references static versions. Plugin is still published to Maven Central (which the Gradle plugin portal proxies), so no changes should be needed from consumers.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- POM _really_ no longer has a dynamic version reference, which could cause unexpected failures #168 

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0.2, 4.10.3, 5.0, 5.6.4, 6.0.1, 6.9.2, 7.0.2, 7.4 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.9.2, 7.0.2, 7.4 |
| 17                 | 7.3.3, 7.4 |

## [0.13.1] - 2021-12-06

Publish to Maven Central and ensure POM references static versions.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- POM no longer has a dynamic version reference, which could cause unexpected failures

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0.2, 4.10.3, 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 15                 | 6.3, 6.6.1, 6.7-rc-1 |

## [0.13.1-rc.3] - 2021-12-06

Publish to Maven Central and ensure POM references static versions.

### Breaking Changes

_None_

### Enhancements

_None_

### Fixes

- POM no longer has a dynamic version reference, which could cause unexpected failures

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0.2, 4.10.3, 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 15                 | 6.3, 6.6.1, 6.7-rc-1 |

## [0.13.0] - 2020-09-20

Update dependencies and confirming support for Gradle 6.6 and Java 15.

### Breaking Changes

_None_

### Enhancements

- Dependency updates

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0.2, 4.10.3, 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.6.1, 6.7-rc-1 |
| 15                 | 6.3, 6.6.1, 6.7-rc-1 |

## [0.12.0] - 2019-11-21

Upgrade to grgit 4.0.0.

### Breaking Changes

- Drop support for Gradle 3 (by upgrading to grgit 4.0.0)

### Enhancements

_None_

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0, 4.10.3, 5.0, 5.6.4 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.1-milestone-1 |

## [0.12.0-rc.1] - 2019-11-21

Upgrade to grgit 4.0.0.

### Breaking Changes

- Drop support for Gradle 3 (by upgrading to grgit 4.0.0)

### Enhancements

_None_

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 4.0, 4.10.3, 5.0, 5.6.4 |
| 11                 | 5.0, 5.6.4, 6.0.1, 6.1-milestone-1 |

## [0.11.0] - 2019-06-03

A small change to allow pre-release versions to be built from a tagged final version. For example, if the current HEAD is tagged as 1.0.0 you previously could only rebuild 1.0.0 or release another final version like 1.0.1. Now you can release 1.0.1-SNAPSHOT or 1.1.0-rc.1.

### Breaking Changes

_None_

### Enhancements

- Allow pre-releases to be built from a tagged final version.

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.3, 5.0, 5.4.1 |
| 11                 | 5.0, 5.4.1 |


## [0.11.0-rc.1] - 2019-06-03

A small change to allow pre-release versions to be built from a tagged final version. For example, if the current HEAD is tagged as 1.0.0 you previously could only rebuild 1.0.0 or release another final version like 1.0.1. Now you can release 1.0.1-SNAPSHOT or 1.1.0-rc.1.

### Breaking Changes

_None_

### Enhancements

- Allow pre-releases to be built from a tagged final version.

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.3, 5.0, 5.4.1 |
| 11                 | 5.0, 5.4.1 |


## [0.10.0] - 2019-05-11

This release is mainly to fix an issue with rebuilding an older version. However, there's also a change to more strictly enforce that history must contain contiguous versions. 

### Breaking Changes

- Reckoner now fails if the base normal and base version are more than 1 major/minor/increment apart (related to #102).

### Enhancements

_None_

### Fixes

- #108 Rebuilding an older tag could fail if the next highest minor is already released

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.3, 5.0, 5.4.1 |
| 11                 | 5.0, 5.4.1 |


## [0.10.0-rc.1] - 2019-05-11

This release is mainly to fix an issue with rebuilding an older version. However, there's also a change to more strictly enforce that history must contain contiguous versions. 

### Breaking Changes

- Reckoner now fails if the base normal and base version are more than 1 major/minor/increment apart (related to #102).

### Enhancements

_None_

### Fixes

- #108 Rebuilding an older tag could fail if the next highest minor is already released

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.3, 5.0, 5.4.1 |
| 11                 | 5.0, 5.4.1 |


## [0.9.0] - 2018-11-19

This is a breaking release to get to Grgit 3. Other than [grgit's breaking changes](https://github.com/ajoberstar/grgit/releases/tag/3.0.0) everything else is staying the same.

Thanks to [Thomas Traude](https://github.com/Riggs333) for updating the Gradle version used to build reckon.

### Breaking Changes

- Upgrade to grgit 3 for Gradle 5 support

### Enhancements

_None_

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.2, 5.0-rc-3 |
| 9                   | 4.2.1, 4.10.2, 5.0-rc-3 |
| 10                 | 4.2.1, 4.10.2, 5.0-rc-3 |

## [0.9.0-rc.1] - 2018-11-19

This is a breaking release to get to Grgit 3. Other than [grgit's breaking changes](https://github.com/ajoberstar/grgit/releases/tag/3.0.0) everything else is staying the same.

### Breaking Changes

- Upgrade to grgit 3 for Gradle 5 support

### Enhancements

_None_

### Fixes

_None_

### Deprecations

_None_

### Compatibility

Tested against the following versions.

| Java Version | Gradle Versions |
|---------------|-------------------|
| 8                   | 3.0, 3.5.1, 4.0, 4.10.2, 5.0-rc-3 |
| 9                   | 4.2.1, 4.10.2, 5.0-rc-3 |
| 10                 | 4.2.1, 4.10.2, 5.0-rc-3 |

## Earlier Releases

- Thanks to [Niklaus Bucher](https://github.com/nikbucher) for a fix to the group ID referenced in the docs
