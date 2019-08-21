---
title: Short-lived Branches
url: /branches/short-lived-branches/
---

<!-- sonarqube -->

_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

<!-- /sonarqube -->

Short-lived branch analysis shows your analysis and quality gate status in the {instance} UI.

## Quality Gate

The short-lived branch quality gate:

* **Focuses on new code** – The short-lived branch quality gate only uses your project's quality gate conditions that apply to "on New Code" metrics.
* **Assigns a status** – Each short-lived branch shows a quality gate status reflecting whether it Passed (green) or Failed (red).

## Issue Creation and Synchronization

The issues visible on the short-lived branch are the new issues corresponding to files modified in the branch.

Modified files are determined based on the checksum of each file on the sonar.branch.target and the short-lived branch.

## New Code Period

The ephemeral nature of short-lived branches means the New Code Period is implicit; everything changed in the branch is new code.

## Settings and Quality Profiles on Branches

Branch settings and quality profiles default to those set for the master branch, and by design, it's not possible to configure other values.

## Known Limitations

* You cannot connect SonarLint to a short-lived branch.
* Analysis of a short-lived branch based on another short-lived branch is not supported.
