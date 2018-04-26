---
title: Frequently Asked Branches Questions
---

_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)_

**Q:** How long are branches retained?  
**A:** Long-lived branches are retained until you delete them manually (**Administration > Branches**).
Short-lived branches are deleted automatically after 30 days with no analysis.
This can be updated in **Configuration > General > Number of days before purging inactive short living branches**.

**Q:** Do I need to have my project stored in an SCM such as Git or SVN to use this feature?  
**A:** No, you don't need to be connected to a SCM. But if you use Git or SVN we can better track the new files on short-lived branches and so better report new issues on the files that really changed during the life of the short-lived branch.

**Q:** If I flag an Issue as "Won't Fix" or "False-Positive", will it be replicated as such when merging my short-lived branch into the Master?  
**A:** Yes. Each time there is an analysis of a long-lived branch, we look at the issues on the short-lived branches and try to synchronize them with the newly raised issues on the long-lived branch. In case you made some changes on the issues (false-positive, won't fix), these changes will be reported on the long-lived branch.

**Q:** Can I still use `sonar.branch`?  
**A:** `sonar.branch` is deprecated. You can still use it but it will behave the same way it always has: a separate project will be created. We encourage you to smoothly migrate your users to the new parameter `sonar.branch.name`.
Please note you cannot use `sonar.branch` together with `sonar.branch.name`.

**Q:** Can I manually delete a branch?  
**A:** This can be achieved by going into the Administration menu at Project's level, then Branches.

**Q:** How do I control the lifespan of a short-lived branch?  
**A:** As a global admin, you can set the parameter sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches to control how many days you want to keep an inactive short-lived branch.

**Q:** Does the payload of the Webhook contain extra information related to Branches?  
**A:** Yes, an extra node called "branch" is added to the payload.

**Q:** When are Webhooks called?  
**A:** When the computation of the background task is done for a given branch but also when an issue is updated on a short-lived branch.

**Q:** What is the impact on my LOCs consumption vs my license?  
**A:** LOCs scanned on long-lived or short-lived branches are NOT counted so you can scan as much as you want without impact on your LOCs consumed
