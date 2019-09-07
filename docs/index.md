# How Reckon Works

## Axioms

These are the rules that reckon presumes are true, both informing how it reads a repo's history and how it calculates the next version:

1.  **Existing version tags MUST be SemVer compliant or they will be ignored**. Any existing tags you want to be considered by the algorithm **must** be SemVer compliant strings (optionally, prefixed with `v`). i.e. `v2.3.0` and `1.0.4-beta.1+abcde` **are** compliant, `1.0` **is not** (only 2 segments).
1.  **NO duplicates version in the history.** A single version MUST not be produced from two different commits.
1.  **Version numbers MUST increase.** If version X's commit is an ancestor of version Y's commit, X < Y.
1.  **NO skipping final versions.** Final versions MUST increase using only the rules from [SemVer 6, 7, 8](http://semver.org/spec/v2.0.0.html). e.g. If X=1.2.3, Y must be one of 1.2.4, 1.3.0, 2.0.0.
1.  **Two branches MUST NOT create tagged pre-releases for the same targeted final version.**
    - If the branches have a shared merge base the version being inferred must skip the targeted final version and increment a second time.
    - If no shared merge base the inference should fail.
1.  **Final versions MUST NOT be re-released as a pre-release.** Once you release a final version (e.g. 1.2.3), that same commit cannot be re-released as a pre-release (e.g. 1.3.0-beta.1). However, the commit can be re-released as a final (e.g. 1.3.0).
1.  **Final and significant versions MUST be released from a clean repo.** If there are any uncommitted changes, the version will not be treated as a final or significant.

## Inputs

In order to infer the next version, reckon needs two pieces of input:

- **scope** - one of `major`, `minor`, or `patch` (defaults to `minor`), indicating which component of the version should be incremented. If the previous version was 1.2.3, a scope of `minor` would result in 1.3.0.
- **stage** - one of a user-provided list (e.g. `alpha`, `beta`, `rc`, `final`), indicating the stage of development.

These inputs can be provided directly by the user or using a custom implementation that might detect them from elsewhere.

## Examples

This is a continuous example of how the inference algorithm works in practice with the Gradle plugin.

```groovy
plugins {
  id 'org.ajoberstar.reckon' version '<version>'
  // other plugins
}

// ...
reckon {
  scopeFromProp()
  stageFromProp('beta', 'rc', 'final')
}
// ...
```

### You have some changes in the repository, but no commits yet

```
$ ./gradlew build
Reckoned version: 0.1.0-beta.0.0+20180704T171826Z
```

This used the default of `minor` scope and `beta` stage (`beta` is the first stage alphabetically). Since you have some changes in your repo that aren't committed, we use a timestamp instead of commit hash.

### Now make a commit, but run the same Gradle command

```
$ ./gradlew build
Reckoned version: 0.1.0-beta.0.1+e06c68a
```

The version now shows 1 commit since a normal has been released, and the abbreviated commit hash in the build metadata.

### Now make some more changes, but don't commit them

```
$ ./gradlew build
Reckoned version: 0.1.0-beta.0.1+20180704T171826Z
```

The version hasn't changed except to switch to a timestamp in the build metadata, since it's not a clean commit.

### Now commit this and let's release a minor version beta

You can specify the scope or leave it off, since `minor` is the default.

```
$ ./gradlew build reckonTagPush -Preckon.scope=minor -Preckon.stage=beta
$ ./gradlew build reckonTagPush -Preckon.stage=beta
Reckoned version: 0.1.0-beta.1
```

Note that you no longer have a count of commits or a commit hash, since this is a significant version that will result in a tag.

Note also that if your repository origin based not on ssh protocol, but https:

```shell
$ git remote -v
origin  https://github.com/$username/repository.git (fetch)
origin  https://github.com/$username/repository.git (push)
```

Then you must provide credenctials Grgit is going to be used during `git push` command:

```shell
$ ./gradlew build reckonTagPush -Preckon.stage=beta \
    -Dorg.ajoberstar.grgit.auth.username=$username \
    -Dorg.ajoberstar.grgit.auth.password=$password
```

See details [hete](http://ajoberstar.org/grgit/grgit-authentication.html)

### Now just run the build again

```
$ ./gradlew build
Reckoned version: 0.1.0-beta.1
```

The current `HEAD` is tagged and you haven't changed anything, or indicated you wanted a different version by providing scope or stage. Reckon assumes you just want to rebuild the existing version.

### Make a bunch more commits and build again

```
$ ./gradlew build
Reckoned version: 0.1.0-beta.1.8+e06c68a
```

We're back to an insignificant version, since you didn't indicate a stage. Again we get the commit count and hash.

### Release another beta

```
$ ./gradlew build reckonTagPush -Preckon.stage=beta
Reckoned version: 0.1.0-beta.2
```

While you already could have left the scope of with the default of `minor`, you can also leave it off because you just want to continue development towards the _target_ normal version you've been working on.

### Release this commit as an rc

You've decided there's enough features in this release, and you're ready to treat it as a release-candidate.

```
$ ./gradlew build reckonTagPush -Preckon.stage=rc
Reckoned version: 0.1.0-rc.1
```

Note that the count after the stage resets to 1.

### Make a bug fix but don't commit it yet

```
$ ./gradlew build
Reckoned version: 0.1.0-rc.1.8+20180704T171826Z
```

Note that the commit count does not reset (since it's based on commits since the last normal).

### Commit the change and release another rc

```
$ ./gradlew build reckonTagPush -Preckon.stage=rc
Reckoned version: 0.1.0-rc.2
```

### Release this as a final

You've decided there aren't any bugs in this release and you're ready to make it official.

```
$ ./gradlew build reckonTagPush -Preckon.stage=final
Reckoned version: 0.1.0
```

### Make this the 1.0.0

You've decided this is feature complete and you're ready to make your 1.0.0 release.

```
$ ./gradlew build reckonTagPush -Preckon.scope=major -Preckon.stage=final
Reckoned version: 1.0.0
```

### Make some commits and build

```
$ ./gradlew build
Reckoned version: 1.1.0-beta.0.4+7836cf7
```

Note that `minor` was again used as a default, same with `beta`, and that your commit count reset since a normal was released.

### Release this as a patch rc

```
$ ./gradlew build reckonTagPush -Preckon.scope=patch -Preckon.stage=rc
Reckoned version: 1.0.1-rc.1
```

### Release as a final patch

```
$ ./gradlew build reckonTagPush -Preckon.stage=final
Reckoned version: 1.0.1
```

While the default is usually `minor`, if you're already developing towards a `patch` or `major` those will be used as defaults instead.

### Make some changes but don't commit them and run again

```
$ ./gradlew build reckonTagPush -Preckon.stage=final
Reckoned version: 1.0.1-beta.0.0+20180704T171826Z
```

Normally if your `HEAD` is already tagged, that version is used as a rebuild. However, if your repo is dirty, it knows it's not a rebuild.
