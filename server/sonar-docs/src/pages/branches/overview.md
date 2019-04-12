---
title: Overview
url: /branches/overview/
---

<!-- sonarqube -->
_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._
<!-- /sonarqube -->


Branch analysis allows you to

* analyze long-lived branches
* analyze short-lived branches
* notify external systems when the status of a short-lived branch is impacted

## Branch Types

### Short-lived

This corresponds to Pull/Merge Requests or Feature Branches. This kind of branch:

* will disappear quickly
* will be merged rapidly to prevent integration issues
* is developed for a given version, so the version does not change,
  and there is no way to set the New Code period; everything that has been changed in the branch is new code
* tracks all the new issues related to the code that changed on it.

![conceptual illustration of short-lived branches.](/images/short-lived-branch-concept.png)

For more, see [Short-lived Branches](/branches/short-lived-branches/)

### Long-lived

This corresponds to "Maintenance" Branches that will house several release versions.
This kind of branch will:

* last for a long time
* inevitably diverge more and more from the other branches
* house several release versions, each of which must pass the quality gate
  to go to production not be expected to be merged into another branch

![conceptual illustration of long-lived branches.](/images/long-lived-branch-concept.png)

For more, see [Long-lived Branches](/branches/long-lived-branches/)

### Master / Main Branch

This is the default, and typically corresponds to what's being developed for
your next release. This is usually known within a development team as
"master" or "head", and is what is analyzed when no specific branch parameters
are provided. It is labeled "Main Branch" and defaults to the name "master",
but can be renamed from within the interface. When you are not using Developer Edition, this is the only branch you see.

## Analysis

A branch is created when the `sonar.branch.name` parameter is passed during analysis.

| Parameter Name        | Description                                                                                                                                                                                                                                                             |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `sonar.branch.name`   | Name of the branch (visible in the UI)                                                                                                                                                                                                                                  |
| `sonar.branch.target` | Name of the branch where you intend to merge your short-lived branch at the end of its life. If left blank, this defaults to the master branch. It can also be used while initializing a long-lived branch to sync the issues from a branch other than the Main Branch. |

### Git History

By default, TravisCI only fetches the last 50 git commits. You must use `git fetch --unshallow` to get the full history. If you don't, new issues may not be assigned to the correct developer.

### Configuring the Branch type

A regular expression is used to determine whether a branch is treated as long-lived or short-lived. By default, branches that have names starting with either "branch" or "release" will be treated as long-lived.

This can be updated <!-- sonarqube -->globally in **Configuration > General Settings > General > Detection of long-lived branches** or <!-- /sonarqube -->at a project's level in **Admininstration > Branches & Pull requests**.

Once a branch type has been set, it cannot be changed. Explicitly, you cannot transform a long-lived to short-lived branch, or vice-versa.

## See also
* [Short-lived Branches](/branches/short-lived-branches/)
* [Long-lived Branches](/branches/long-lived-branches/)
* [Frequently Asked Questions](/branches/branches-faq/)
