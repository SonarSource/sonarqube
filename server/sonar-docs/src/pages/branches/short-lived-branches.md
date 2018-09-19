---
title: Short-lived Branches
url: /branches/short-lived-branches/
---

<!-- sonarqube -->

_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)_

<!-- /sonarqube -->

## Status vs Quality Gate

For short-lived branches, there is a kind of hard-coded quality gate focusing only on new issues. Its status is reflected by the green|red signal associated with each short-lived branch:

* status: green / OK or red / ERROR
* error conditions:
  * new open bugs > 0
  * new open vulnerabilities > 0
  * new open code smells > 0

It is possible to change the status of a short-lived branch from ERROR to OK (red to green), i.e. mergable, by manually confirming the issues. The same is true for the False-Positive and Won't Fix statuses.
It means the status of a short-lived branch will be red only when there are Open issues in the branch.

## Issue Creation and Synchronization

The issues visible on the short-lived branch are the new issues corresponding to files modified in the branch.

Modified files are determined based on the checksum of each file on the sonar.branch.target and the short-lived branch.

## New Code Period

The ephemeral nature of short-lived branches means the New Code Period is implicit; everything changed in the branch is new code.

## Settings and Quality Profiles on Branches

Branch settings and quality profiles default to those set for the master branch, and by design, it's not possible to configure other values.

## Known Limitations

* Only the issue-focused, hard-coded quality gate is available on a short-lived branch.
* You cannot connect SonarLint to a short-lived branch.
* Analysis of a short-lived branch based on another short-lived branch is not supported.
