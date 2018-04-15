# How Reckon Works

## Inference Algorithm

### Axioms

These are the rules that reckon presumes are true, both informing how it reads a repo's history and how it calculates the next version:

1. **Existing version tags MUST be SemVer compliant or they will be ignored**. Any existing tags you want to be considered by the algorithm **must** be SemVer compliant strings (optionally, prefixed with `v`). i.e. `v2.3.0` and `1.0.4-beta.1+abcde` **are** compliant, `1.0` **is not** (only 2 segments).
1. **NO duplicates version in the history.** A single version MUST not be produced from two different commits.
1. **Version numbers MUST increase.** If version X's commit is an ancestor of version Y's commit, X < Y.
1. **NO skipping final versions.** Final versions MUST increase using only the rules from [SemVer 6, 7, 8](http://semver.org/spec/v2.0.0.html). e.g. If X=1.2.3, Y must be one of 1.2.4, 1.3.0, 2.0.0.
1. **Two branches MUST NOT create tagged pre-releases for the same targeted final version.**
    * If the branches have a shared merge base the version being inferred must skip the targeted final version and increment a second time.
    * If no shared merge base the inference should fail.
1. **Final versions MUST NOT be re-released as a pre-release.** Once you release a final version (e.g. 1.2.3), that same commit cannot be re-released as a pre-release (e.g. 1.3.0-beta.1). However, the commit can be re-released as a final (e.g. 1.3.0).
1. **Final and significant versions MUST be released from a clean repo.** If there are any uncommitted changes, the version will not be treated as a final or significant.

### Inputs

In order to infer the next version, reckon needs two pieces of input:

- **scope** - one of `major`, `minor`, or `patch` (defaults to `minor`), indicating which component of the version should be incremented. If the previous version was 1.2.3, a scope of `minor` would result in 1.3.0.
- **stage** - one of a user-provided list (e.g. `alpha`, `beta`, `rc`, `final`), indicating the stage of development.

These inputs can be provided directly by the user or using a custom implementation that might detect them from elsewhere.

### Inference

Reckon will use the history of your repository to determine what version your changes are based on and the inputs above will indicate how the version should be incremented from that previous one.

### Gathering inventory from Git

The first step is gathering information about your repository.

- **commitId** - the ID of the current `HEAD`, if any
- **clean** - whether or not the repository has any uncommitted changes
- **currentVersion** - the highest precedence version tagged on the current `HEAD`, if any. (e.g. if the `HEAD` is tagged with 1.0.0-rc.1 and 1.0.0, the `currentVersion` is 1.0.0)
- **baseVersion** - the topologically nearest version in this history of the current `HEAD`. Walk backwards from the `HEAD` following all parents along the way. Stop at the first tagged version you find on all paths. If more than one tagged version is found, the highest precedence one is used. (e.g. if 2.0.0-beta.1 and 1.4.1 are found and 1.4.1, 2.0.0-beta.1 is used)
- **baseNormal** - the topologically nearest _final_ version in the history of the current `HEAD`. The same logic is used as for `baseVersion`, but only final versions are considered.
- **parallelNormals** - any normal version being developed on another branch. Any tagged version that is not in the history of the current `HEAD` is included, but only the normal component is used. (e.g. if another branch has 2.0.0-beta.1 tagged, 2.0.0 is considered a parallel version)
- **claimedVersions** - any tagged version within the repository

### Reckoning version

#### Apply the scope to get a target normal version

Based on the inventory determined above, and something supplying which **scope** (i.e. `major`, `minor`, or `patch`) should be used to increment the version, determine what normal version your current head is targeting.

From Gradle the supplier will typically be a project property: `reckon.scope`.

1. Which **scope**?
  - If the scope is supplied explicitly (e.g. via `reckon.scope`), use that.
  - Else if your `baseVersion` is a significant version, reuse its scope. (e.g. if `baseNormal` is 1.0.0 and `baseVersion` is 1.0.1-rc.1, use `patch`)
  - Else use `minor`
1. Increment the `baseNormal` based on the **scope**. (e.g. `major` for 1.0.0 results in 2.0.0)
1. If the resulting normal version is in `parallelNormals`, increment again using the same scope. This is meant to let you concurrently develop two normal versions.
1. If the resulting normal version is already claimed, fail inference. You cannot re-release a version or release a pre-release for an existing final.
1. You're done

#### Apply the stage to get a target version (stage)

**NOTE:** If using snapshot, skip to next section.

Based on the inventory and target normal determined above, and something supplying which **stage** (e.g. `rc` or `final`) should be used to increment the pre-release component of the version, determine the version your current head is targeting.

1. Which **stage** and **final/significant** or **insignificant**?
  - If the stage is supplied explicitly (e.g. via `reckon.stage`), use that to create a final/significant version.
    - If the repository is not clean, inference will fail.
  - Else if `baseVersion` targeted the same normal as determined above (e.g. `baseVersion` is 1.2.3-rc.1 and target normal was 1.2.3), use it's stage (e.g. `rc`) to create an insignificant version.
  - Else use the default stage (whichever valid stage is first lexicographically). (e.g. for `beta`, `rc`, `final`, `beta` would be the default) to create an insignificant version
1. If the stage is `final`, return the target normal unchanged.
1. If the repository is clean, `currentVersion` is present, and no stage is provided, consider this a rebuild and return the `currentVersion` unchanged.
1. If creating a significant version and `baseVersion` had the same stage, increment the number. Otherwise use 1. (e.g. `baseVersion` of 1.2.3-beta.1. With stage of `beta`, 1.2.3-beta.2. With stage of `rc`, 1.2.3-rc.1)
1. If creating an insignificant version and `baseVersion` is not a normal, append the number of commits to the `baseVersion` version and add build metadata indicating the current commit and whether the repo contains uncommitted changes. (e.g. `baseVersion` is 1.2.3-beta.1, results in 1.2.3-beta.1.12+e06c68a863eb6ceaf889ee5802f478c10c1464bb.uncommitted)
1. If creating an insignificant version and `baseVersion` is a normal, append the number of commits to the normal version and add build metadata indicating the current commit and whether the repo contains uncommitted changes. (e.g. `baseVersion` is 1.2.3-beta.1, results in 1.2.3-beta.1.12+e06c68a863eb6ceaf889ee5802f478c10c1464bb.uncommitted)

#### Apply the stage to get a target version (snapshot)

**NOTE:** If not using snapshot, see previous section.

Based on the inventory and target normal determined above, and something supplying which **stage** (e.g. `snapshot` or `final`) should be used to increment the pre-release component of the version, determine the version your current head is targeting.

_More details later_
