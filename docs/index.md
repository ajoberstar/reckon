# How Reckon Works

## Inference Algorithm

### Axioms

These are the rules that reckon presumes are true, both informing how it reads a repo's history and how it calculates the next version:

1. **Existing version tags MUST be SemVer compliant or they will be ignored**. Any existing tags you want to be considered by algorithm **must** be SemVer compliant strings (optionally, prefixed with `v`). i.e. `v2.3.0` and `1.0.4-beta.1+abcde` **are** compliant, `1.0` **is not** (only 2 segments).
1. **NO duplicates version in the history.** A single version MUST not be produced from two different commits.
1. **Version numbers MUST increase.** If version X's commit is an ancestor of version Y's commit, X < Y.
1. **NO skipping final versions.** Final versions MUST increase using only the rules from [SemVer 6, 7, 8](http://semver.org/spec/v2.0.0.html). e.g. If X=1.2.3, Y must be one of 1.2.4, 1.3.0, 2.0.0.
1. **Two branches MUST NOT create tagged pre-releases for the same targeted final version.**
    * If the branches have a shared merge base the version being inferred must skip the targeted final version and increment a second time.
    * If no shared merge base the inference should fail.


### Inputs

In order to infer the next version, reckon needs two pieces of input:

- **scope** - one of `major`, `minor`, or `patch` (defaults to `minor`), indicating which component of the version should be incremented. If the previous version was 1.2.3, a scope of `minor` would result in 1.3.0.
- **stage** - one of a user-provided list (e.g. `alpha`, `beta`, `rc`, `final`), indicating the stage of development.

These inputs can be provided directly by the user or using a custom implementation that might detect them from elsewhere.

### Inference

Reckon will use the history of your repository to determine what version your changes are based on and the inputs above will indicate how the version should be incremented from that previous one.

_More detail will be added in the future._
