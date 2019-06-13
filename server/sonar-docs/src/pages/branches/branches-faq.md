---
title: Frequently Asked Branches Questions
url: /branches/branches-faq/
---

<!-- sonarqube -->

_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

<!-- /sonarqube -->


## How long are branches retained?  
Long-lived branches are retained until you delete them manually (**Administration > Branches**).
Short-lived branches are deleted automatically after 30 days with no analysis.
This can be updated in **Configuration > General > Number of days before purging inactive short living branches**. For more, see [Housekeeping](/instance-administration/housekeeping/).

## Does my project need to be stored in an SCM like Git or SVN?  
No, you don't need to be connected to a SCM. But if you use Git or SVN we can better track the new files on short-lived branches and so better report new issues on the files that really changed during the life of the short-lived branch.

## What if I mark an Issue "Won't Fix" or "False-Positive" in a branch?
It be replicated as such when merging my short-lived branch into the Master. Each time there is an analysis of a long-lived branch, we look at the issues on the short-lived branches and try to synchronize them with the newly raised issues on the long-lived branch. In case you made some changes on the issues (false-positive, won't fix), these changes will be reported on the long-lived branch.

## Can I manually delete a branch?  
This can be achieved by going into the Administration menu at Project's level, then Branches.

## How do I control the lifespan of a short-lived branch?  
As a global admin, you can set the parameter `sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches` to control how many days you want to keep an inactive short-lived branch.

## Does the payload of the Webhook include branch information?  
Yes, an extra node called "branch" is added to the payload.

## When are Webhooks called?  
When the computation of the background task is done for a given branch but also when an issue is updated on a short-lived branch.

## What is the impact on my LOCs consumption vs my license?  
The LOC of your largest branch are counted toward your license limit. All other branches are ignored.  
