---
title: Issues
url: /user-guide/issues/
---

While running an analysis, {instance} raises an issue every time a piece of code breaks a coding rule. The set of coding rules is defined through the associated [quality profile](/instance-administration/quality-profiles/) for each language in the project. 

Each issue has one of five severities:

1. **BLOCKER**  
Bug with a high probability to impact the behavior of the application in production: memory leak, unclosed JDBC connection, .... The code MUST be immediately fixed.
1. **CRITICAL**  
Either a bug with a low probability to impact the behavior of the application in production or an issue which represents a security flaw: empty catch block, SQL injection, ... The code MUST be immediately reviewed. 
1. **MAJOR**  
Quality flaw which can highly impact the developer productivity: uncovered piece of code, duplicated blocks, unused parameters, ...
1. **MINOR**  
Quality flaw which can slightly impact the developer productivity: lines should not be too long, "switch" statements should have at least 3 cases, ...
1. **INFO**  
Neither a bug nor a quality flaw, just a finding.

Ideally, the team wouldn't introduce any new issues (any new technical debt). [SonarLint](https://sonarlint.org) can help developers because it provides the ability to perform local analyses to check their code before pushing it back to the SCM. But in real life, it's not always possible to code without any new technical debt, and sometimes it's not worth it.

So new issues get introduced.

## Understanding issue context
Sometimes, issues are self-evident once they're pointed out. For instance, if your team has agreed to a init-lower, camelCase variable naming convention, and an issue is raised on `My_variable`, you don't need a lot of context to understand the problem. But in other situations context may be essential to understanding why an issue was raised. That's why {instance} supports not just the primary issue location, where the issue message is shown, but also secondary issue locations. For instance, secondary issues locations are used to mark the pieces of code in a method which add Cognitive Complexity to a method. 

But there are times when a simple laundry list of contributing locations isn't enough to understand an issue. For instance, when a null pointer can be dereferenced on some paths through the code, what you really need are issue flows. Each flow is a _set_ of secondary locations ordered to show the exact path through the code on which a problem can happen. And because there can be multiple paths through the code on which, for instance a resource is not released, {instance} supports multiple flows.


## Lifecycle of Code Smell, Bug, and Vulnerability Issues
### Statuses
After creation, issues flow through a lifecycle, taking one of five possible statuses:

* **Open** - set by {instance} on new issues
* **Confirmed** - set manually to indicate that the issue is valid
* **Resolved** - set manually to indicate that the next analysis should Close the issue
* **Reopened** - set automatically by {instance} when a Resolved issue hasn't actually been corrected
* **Closed** - set automatically by {instance} for automatically created issues. 

### Resolutions
Closed issues will have one of two resolutions:

* **Fixed** - set automatically when a subsequent analysis shows that the issue has been corrected or the file is no longer available (removed from the project, excluded or renamed)
* **Removed** - set automatically when the related rule is no longer available. The rule may not be available either because it has been removed from the Quality Profile or because the underlying plugin has been uninstalled.

Resolved issues will have one of two resolutions:
* **False Positive** - set manually
* **Won't Fix** - set manually

### Issue Workflow 
Issues are automatically closed (status: Closed) when:
* an issue (of any status) has been properly fixed => Resolution: Fixed
* an issue no longer exists because the related coding rule has been deactived or is no longer available (ie: plugin has been removed) => Resolution: Removed

Issues are automatically reopened (status: Reopened) when:
* an issue that was manually Resolved as Fixed(but Resolution is not False positive) is shown by a subsequent analysis to still exist

### Security Hotspot Issue Lifecycle
Security Hotspot issues have a dedicated lifecycle. They aren't considered actionable, and must be reviewed by a user with the "Administer Security Hotspots" permission. 

## Lifecycle of Security Hotspot Issues
Security Hotspot issues have a dedicated lifecycle. They aren't considered actionable, and must be reviewed by a user with the "Administer Security Hotspots" permission. 

### Statuses
After creation, Security Hotspot issues flow through a dedicated lifecycle, taking one of four possible statuses:

* **Open** - set by {instance} on new issues
* **Resolved** (Won't Fix) - set automatically by {instance} when a Security Auditor Accepts the fix done by a developer on a Manual Vulnerability or when an Open Hotspot or Manual Vulnerability is Cleared by a Security Auditor.
* **To Review** - set automatically when a developer requests a security auditor to Review the fix she has done on a Manual Vulnerability
* **Reopened** - set when a developer Dismisses an Open Manual Vulnerability or when a Security Auditor manually reopens issues in order to run a new audit on already Resolved issues

Security Hotspot issues are only ever closed if the code containing the Security Hotspot is deleted.  A Security Hotspot may also become Removed if the rule which identified the hotspot is removed from the project's Quality Profile.

## Understanding which Issues are "New"
To determine the creation date of an issue, an algorithm is executed during each analysis to determine whether an issue is new or existed previously. This algorithm relies on content hashes (excluding whitespace) for the line the issue is reported on. For multi-line issues, the hash of the first line is used. For each file (after detection of file renaming), the algorithm takes the base list of issues from the previous analysis, and tries to match those issues with the raw issue list reported by the new analysis. The algorithm tries to first match using the strongest evidence, and then falls back to weaker heuristics.

* if the issue is on the same rule, with the same line number and with the same line hash (but not necessarily with the same message) > MATCH
* detect block move inside file, then if the issue is on the same (moved) line and on the same rule (but not necessarily with the same message) > MATCH
* on the same rule, with the same message and with the same line hash (but not necessarily with the same line) > MATCH
* on the same rule, with the same message and with the same line number (but not necessarily with the same line hash) > MATCH
* on the same rule and with the same line hash (but not the same message and not the same line) > MATCH
* is there a matching **CLOSED** issue > MATCH and Reopen

Unmatched "base" issues are closed as fixed.

Unmatched "raw" issues are new.

## Understanding Issue Backdating
Once an issue has been determied to be "new", as described above, the next question is what date to give it. For instance, what if it has existed in code for a long time, but only found in the most recent analysis because new rules were added to the profile? Should this issue be given the date of the last change on its line, or the date of the analysis where it was first raised? That is, should it be backdated? If the date of the last change to the line is available (this requires [SCM integration](/analysis/scm-integration/)) then under certain circumstances, the issue will be backdated:

* On first analysis of a project or branch
* When the rule is new in the profile (a brand new rule activated or a rule that was deactivated and is now activated)
* When the analyzer has just been upgraded (because rule implementations could be smarter now)
* When the rule is external

As a consequence, it is possible that backdating will keep newly raised issues out of the New Code Period.


## Automatic Issue Assignment
### For Bug, Vulnerability and Code Smell
New issues are automatically assigned during analysis to the last committer on the issue line if the committer can be correlated to a {instance} user. Note that currently, issues on any level above a file, e.g. directory / project, cannot be automatically assigned.

### For Security Hotspot
Issues are automatically assigned only when the Security Hotspot is transformed into a Vulnerability through the "Detect" action.

### User Correlation
Login and email correlations are made automatically. I.e. if the user commits with her email address and that email address is part of her {instance} profile, then new issues raised on lines where she was the last committer will be automatically assigned to her.

Additional correlations can be made manually in the user's profile (see "SCM accounts" in Authorization for more).

### Known Limitation
If the SCM login associated with an issue is longer than 255 characters allowed for an issue author, the author will be left blank.

## Issue edits
{instance}'s issues workflow can help you manage your issues. There are seven different things you can do to an issue (other than fixing it in the code!): Comment, Assign, Confirm, Change Severity, Resolve, Won't Fix, and False Positive.

These actions break out into three different categories. First up is the "technical review" category.

### Technical Review
The Confirm, False Positive, Won't Fix, Severity change, and Resolve actions all fall into this category, which presumes an initial review of an issue to verify its validity. Assume it's time to review the technical debt added in the last review period - whether that's a day, a week, or an entire sprint. You go through each new issue and do one:

* **Confirm** - By confirming an issue, you're basically saying "Yep, that's a problem." Doing so moves it out of "Open" status to "Confirmed".
* **False Positive** - Looking at the issue in context, you realize that for whatever reason, this issue isn't actually a problem. So you mark it False Positive and move on. Requires Administer Issues permission on the project.
* **Won't Fix** - Looking at the issue in context, you realize that while it's a valid issue it's not one that actually needs fixing. In other words, it represents accepted technical debt. So you mark it Won't Fix and move on. Requires Administer Issues permission on the project.
* **Severity change** - This is the middle ground between the first two options. Yes, it's a problem, but it's not as bad a problem as the rule's default severity makes it out to be. Or perhaps it's actually far worse. Either way, you adjust the severity of the issue to bring it in line with what you feel it deserves.  Requires Administer Issues permission on the project.
* **Resolve** - If you think you've fixed an open issue, you can Resolve it. If you're right, the next analysis will move it to closed status. If you're wrong, its status will go to re-opened.

Additionally, Security Hotspots allow the following:

* **Detect** - Confirms a Security Hotspot as a true issue and manually opens a Vulnerability. Requires Administer Security Hotspots on the project.
* **Clear** - Marks a Security Hotspot or manually opened Vulnerability as being without issue and shouldn't be fixed. Requires Administer Security Hotspots on the project.
* **Request Review** - Request that a Security Auditor review changes made to remediate a manually opened Vulnerability.
* **Reject** - After review ,reject the remediation for a manually opened Vulnerability and return it to an open issue. Requires Administer Security Hotspots on the project.

If you tend to mark a lot of issues False Positive or Won't Fix, it means that some coding rules are not appropriate for your context. So, you can either completely deactivate them in the quality profile or use issue exclusions to narrow the focus of the rules so they are not used on specific parts (or types of object) of your application. Similarly, making a lot of severity changes should prompt you to consider updating the rule severities in your profiles.

As you edit issues, the related metrics (e.g. New Bugs), will update automatically, as will the Quality Gate status if it's relevant.

### Dispositioning
Once issues have been through technical review, it's time to decide who's going to deal them. By default they're assigned to the last committer on the issue line (at the time the issue is raised), but you can certainly reassign them to yourself or someone else. The assignee will receive email notification of the assignment if he signed up for notifications, and the assignment will show up everywhere the issue is displayed, including in the My Issues list in the My Account space.

### General
At any time during the lifecycle of an issue, you can log a comment on it. Comments are displayed in the issue detail in a running log. You have the ability to edit or delete the comments you made.

You can also edit an issue's tags. Issues inherit the tags of the rules that created them, but the tag set on an issue is fully editable. Tags can be created, added and removed at will for users with the Browse permission on the project.

Although they are initially inherited from the relevant rule, the tags on an issue are not synchronized with the rule, so adding tags to a rule will not add those tags to the rule's issues. 

### Bulk Change
All of these changes and more can be made to multiple issues at once using the Bulk Change option in the issues search results pane.


## Purging Closed Issues
By default, Closed issues are kept for 30 days. For more details, see [Housekeeping](/instance-administration/housekeeping/).

