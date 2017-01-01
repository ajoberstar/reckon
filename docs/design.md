# Design Documentation

This will start as a brain dump of the various challenges/ideas that I can think of.

# Scope and Stage

In gradle-git, the entire versioning approach was based on user inputs of the _scope_ (whether the change was for a major, minor, or patch release) and the _stage_ (what phase of development that release was in). The rest was determined by backwards looking in history from the current `HEAD` to the _nearest_ tagged versions.

Since my personal use was purely around the use case of publishing from my laptop for open source projects, there wasn't much complexity involved. I didn't have any active branching going on, I didn't release from anywhere besides master, I rarely (if ever) did any maintenance releases.

* Some people took issue with the default scope of patch, since they generally developed new features.
* Others noted that the correct version wasn't chosen if


# Use Cases

Generally, I think I need to get some visualizations of open source histories to see if the logic is going to make sense.

## Pipelines

Even without a change to the gradle-git model, there is benefit in providing ways for the scope and stage to be determined automatically, rather than requiring user input. This could resolve around pluggable sources for this information.

Scopes could be determined based in the issues that have been included in the release. So, for GitHub, look through commits since the last version, pick out the issue numbers, query labels from GitHub and support some scheme for this. Maybe two dimensions of the labels: enhancement vs bug and breaking vs compatible.

|                 | Breaking | Compatible |
|-----------------|----------|------------|
| **Enhancement** | MAJOR    | MINOR      |
| **Bug**         | MAJOR    | PATCH      |

Scopes could alternately be determined by detecting which GitHub milestone is being developed towards. Would also probably involve looking up based on issue numbers. The name of the milestone would be assumed to be the version number you are working towards.

The issue number inference seems like it could be brittle, so not super convinced by it, but it seems interesting.

Scopes could also be inferred from the branch names:

* `master` or `develop` imply MINOR (though at some point they would be for MAJOR too)
* `fix/*` or `maintenance/*` imply PATCH

## GitHub FLow

* Generally developing on mainline
* May have fix/maintenance branches
* Long-running branches probably play well with gradle-git style backwards inference
* PR branches are what become the issue
  * The "stage" of a PR is somewhat irrelevant (unless it's getting published). I could see this being a use case though.
  * PR's should sort lower than any other stage (I would think).
  * 1.0.0-aardvark+commit.hash

## Gitflow

* Multiple branches active at a time.
* Release branches _claim_ a version and the develop branch needs to move on to the next one
* This requires some sort of forward or global inference, rather than just gradle-git style backwards inference.

## Parallel Development

There may be other generic parallel issues. Such as wanting to develop 2.0.x and 2.1.x concurrently. Maybe this is just a generalization of the Gitflow release branches.

## Merges

When branches are merged gradle-git backwards inference doesn't always match user expectations. This, I think, is just because it focuses on distance rather than ancestry. The initial semver-vcs sorting is based on ancestry, where as long as there are no commits between you and the head, you're equal to any other commit that's true for. So if 2.0.0 is 100 commits before the HEAD and 0.1.0 is 1 commit before, they could still be tied. That tie is broken by version precedence, so 2.0.0 would win. This makes one (hopefully not big) assumption that you would never merge a 2.0.0 branch into a 1.x branch and still expect a 1.x version to come out. It seems illogical, given that you're merging the 2 history in, which presumably has a breaking change.

## Axioms

In order to determine how the inference can be implemented, I want to start from a few axioms and see what that implies. Assuming these continue to make sense at that point, the implementation should be verified with an automated analysis running against existing repos from large open source projects. This can validate whether the logic can choose the right versions in practice.

1. **NO duplicates.** A single version MUST not be produced from two different commits.
1. **Version numbers MUST increase.** If version X's commit is an ancestor of version Y's commit, X < Y.
1. **NO skipping final versions.** Final versions MUST increase using only the rules from [SemVer 6, 7, 8](http://semver.org/spec/v2.0.0.html). e.g. If X=1.2.3, Y must be one of 1.2.4, 1.3.0, 2.0.0.
1. **Two branches MUST NOT create tagged pre-releases for the same targeted final version.**
    * If the branches have a shared merge base the version being inferred must skip the targeted final version and increment a second time.
    * If no shared merge base the inference should fail.

### Implications

* Version inference needs to take into account **globally**, all tagged versions in the repo. If a version would duplicate another commit, the inference must fail.
* If you're rebuilding an already released commit the version can be the same or higher as the existing tagged version.
* If a pre-release for final version Y does skip a version X. Y cannot have a final release until X is merged into it's history.
* A pre-release skip is caused by:
  * Another branch with a tag for the version you would have otherwise targeted.
  * Another branch matching some naming scheme indicating it has claimed that version.
* The merge base of two branches is treated as if it was tagged with the latest version on either branch (and/or the version claimed via branch name)
