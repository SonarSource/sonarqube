---
title: Overview
url: /analysis/branch-pr-analysis-overview/
---

_Merge and Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in GitLab CI/CD, Cirrus CI, and Jenkins with a Branch Source plugin configured can automatically detect branches and merge or pull requests by using environment variables set in the jobs.

[[warning]]
| Automatic configuration is disabled if any branch or pull request properties have been set manually.

## Keeping your "master" branch history when upgrading from Community Edition to a commercial edition

In Community Edition, your analyzed branch is named "master" by default. 

When upgrading to a current commercial edition version, automatic branch and pull request configuration creates branches based on their names in your code repository. If the name of your Main Branch (master) in SonarQube doesn't match the branch's name in your code repository, the history of your Main Branch won't be taken on by the branch you analyze. 

**Before running analysis**, you can keep your branch history by renaming the Main Branch in SonarQube with the name of the branch in your code repository at **Project Settings > Branches and Pull Requests**. 

For example, if your Main Branch is named "master" in SonarQube but "develop" in your code repository, rename your Main Branch "develop" in SonarQube.

## GitLab CI/CD
- For GitLab CI/CD configuration, see the [GitLab ALM integration](/analysis/gitlab-integration/) page.

## Jenkins
- For Jenkins configuration, see [Jenkins](/analysis/jenkins/).