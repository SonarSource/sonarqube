---
title: Branch Analysis
url: /branches/overview/
---

_Branch analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

## Overview

With Branch Analysis, you can ensure that you're maintaining consistent code quality all the way down to the branch level of your projects. 

### Master / Main Branch

This is the default branch and typically corresponds to what's being developed for your next release. This branch is usually known within a development team as "master" or "head" and is analyzed when no specific branch parameters are provided. SonarQube labels this branch as **Main Branch**, and, with Community Edition, this is the only branch you can analyze. 

Adding projects by DevOps Platform copies the main branch name in SonarQube from the main branch name in your repository. See **DevOps Platform Integrations** in the documentation navigation for more information on adding your project from an DevOps Platform.

If you add your project manually, your main branch defaults to the name "master".

Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), your main branch can be renamed from the project settings at **Project Settings > Branches and Pull Requests**. 

### Settings and Quality Profiles on Branches

Branch settings and Quality Profiles are the same as those set for the main branch, and by design, it's not possible to configure other values. The New Code Period is the only exception to this as it can be set on a branch-by-branch basis.

### New Code

You can set a New Code definition for each branch. This is especially helpful if you are likely to develop and release multiple versions from the branch. See the [Defining New Code](/project-administration/new-code-period/) documentation for more information.

### Quality Gate

The branch Quality Gate lets you know if your branch is ready to be merged. Each branch has a quality gate that:

* Applies on conditions on New Code and overall code.
* Assigns a status (Passed or Failed).

## Setting up Branch analysis

A branch is created when the `sonar.branch.name` parameter is passed during analysis.

| Parameter Name        | Description |
| --------------------- | ------------------------------------------------- |
| `sonar.branch.name`   | Name of the branch (visible in the UI)

### Limiting analysis to relevant branches  

You need to add a condition to your pipeline script to ensure only relevant branches are analyzed. For example, you wouldn't want to run analysis on feature branches that won't need analysis until they have pull requests . 

In the following example, analysis would be limited to branches named `main` or `release/*`.

```
if [[ "$CI_BRANCH_NAME" == main ]] || [[ "$CI_BRANCH_NAME" == release/* ]]; then
  ./gradlew sonarqube
fi
``` 

### Issue Creation and Synchronization

During the first analysis, issues (type, severity, status, assignee, change log, comments) are synchronized with the Main Branch. In each synchronized issue, a comment is added to the change log of the issue on the branch: "The issue has been copied from branch 'main' to branch 'yyy'".

At each subsequent analysis of the branch, any new issue in the Main Branch that comes from a pull request automatically inherits its attributes (type, severity, ...) from the pull request. A comment is added to the change log of the issue on the branch: "The issue has been merged from 'xxx' into 'yyy'"

Issues can be synchronized between branches if you're using the **Reference Branch** [New Code](/project-administration/new-code-period/) definition. When using this New Code definition, any new issue in the reference branch that comes from a feature branch automatically inherits its attributes (type, severity, ...) from the feature branch. A comment is added to the change log of the issue on the branch: "The issue has been merged from 'xxx' into 'yyy'".

### Fetching full Git history

By default, some CIs don't fetch your full Git history. For example, TravisCI only fetches the last 50 git commits. You must use `git fetch --unshallow` to get the full history. If you don't, new issues may not be assigned to the correct developer.

## Managing inactive branches
Inactive branches are branches that are no longer being analyzed. You can use Housekeeping to automatically delete branches that are inactive (i.e. old feature branches) or to keep inactive branches that you want to continue maintaining (i.e. release branches). 

### Deleting inactive branches

You can set the number of days a branch can be inactive before it's deleted in the global settings at **Administration > General Settings > Housekeeping > Number of days before deleting inactive branches**. Branches that are inactive for the number of days that you set will be automatically deleted.

### Using patterns to keep inactive branches

You can use naming patterns to protect specific branches, such as release branches, from automatic deletion. To do this, add a pattern using Java regular expressions under **Administration > General Settings > Housekeeping > Branches > Branches to keep when inactive** at either the global or project level. When a branch is created with a name that follows one of these patterns, it will be kept indefinitely. 

For example, adding the pattern `release/.*` would keep any branches named release/6.0, release/7, and so on.

**Note:** Patterns aren't retroactive and won't apply to branches that have already been created. They only apply to branches created after the pattern is set. You can protect an existing branch at the project level. See the following section.

### Managing inactive branches at a project level

You can set a branch to **Keep when inactive** at the project level from from the **Branches** tab at **Project Settings > Branches and Pull Requests**. Here, you can also turn off protection for a branch so it will be deleted when it's inactive for the number of days that has been specified in the global settings at **Administration > General Settings > Housekeeping > Number of days before deleting inactive branches**. 

**Note:** The main branch is always protected from automatic deletion, even if it's inactive. This can't be changed.

## Keeping your "master" branch history when upgrading from Community Edition to a commercial edition
[[info]]
| If you added a SonarQube project from an DevOps Platform instead of manually, the name of the main branch in SonarQube is copied from the name of the main branch in your repository, and you shouldn't have any branch history issues when upgrading to a commercial edition.

In Community Edition, if you create a project manually, your analyzed branch is named "master" by default. 

When upgrading to a current commercial edition version, automatic branch and pull request configuration creates branches based on their names in your code repository. If the name of your Main Branch (master) in SonarQube doesn't match the branch's name in your code repository, the history of your Main Branch won't be taken on by the branch you analyze. 

**Before running analysis**, you can keep your branch history by renaming the Main Branch in SonarQube with the name of the branch in your code repository at **Project Settings > Branches and Pull Requests**. 

For example, if your Main Branch is named "master" in SonarQube but "develop" in your code repository, rename your Main Branch "develop" in SonarQube.
