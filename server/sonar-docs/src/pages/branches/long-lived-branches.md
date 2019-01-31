---
title: Long-lived Branches
url: /branches/long-lived-branches/
---

<!-- sonarqube -->

_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

<!-- /sonarqube -->

## Status vs Quality Gate

The same quality gate that is applied to the project as a whole is automatically applied to long-lived branches as well. This is not editable.

## Issue Creation and Synchronization

During the **first analysis only**, issues (type, severity, status, assignee, change log, comments) are synchronized with the Main Branch. In each synchronized issue, a comment is added to the change log of the issue on the long-lived branch: "The issue has been copied from branch 'master' to branch yyy".

Then, at each subsequent analysis of the long-lived branch, any new issue that comes from a short-lived branch automatically inherits the attributes (type, severity, ...) the issue had in the short-lived branch. A comment is added to the change log of the issue on the long-lived branch: "The issue had been copied from branch 'the short-live branch' to branch yyy".

## New Code Period

Because long-lived branches will persist for a long time, you are likely to develop and release multiple versions from it, and so you can change the New Code period of a long-lived branch in **Administration > Branches**.

## Settings and Quality Profiles on Branches

Branch settings and quality profiles default to those set for the master branch, and by design, it's not possible to configure other values.
