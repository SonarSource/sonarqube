---
title: Setting Your New Code Period
url: /project-administration/new-code-period/
---
By focusing on code that's been added or changed in your New Code Period, you can set consistent quality requirements and expectations on all new code. With this focus, your new code will be issue-free and you'll clean up the code you encounter along the way. For more information on the New Code Period, see the [Clean as You Code](/user-guide/clean-as-you-code/) page.

You can set a New Code Period at the global, project, or branch level.

## Setting a global New Code Period
Your global New Code Period will be the default for your projects. You can set the global New Code Period at [**Administration > Configuration > General Settings > New Code Period**](/#sonarqube-admin#/admin/settings?category=new_code_period/).  

You can set the global New Code Period to the following:

* **Previous Version** – The New Code Period defaults to **Previous version** which shows any changes made in your project's current version. This works well for projects with regular versions or releases.
* **Number of days** – You can specify a number of days for a floating New Code Period. For example, setting **Number of Days** to 30 creates a floating New Code Period beginning 30 days from the current date.

## Setting a project-level New Code Period
You can override the global New Code Period by setting a project-level New Code Period from the project page at **Project Settings > New Code Period**. For [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), this will be the default New Code Period for all of the project's branches.

You can set a project's New Code Period to the following:

* **Previous Version** – Set the New Code Period to show any changes made in your project's current version. This works well for projects with regular versions or releases.
* **Number of days** – Specify a number of days for a floating New Code Period. For example, setting **Number of Days** to 30 creates a floating New Code Period beginning 30 days from the current date.
* **Specific analysis** – Choose a previous analysis as your New Code Period. The New Code Period will show any changes made since that analysis. 

   **Note:** For Community Edition, you can set the New Code Period to a specific past analysis at the project-level because Community Edition doesn't support multiple branches. [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/) let you set the New Code Period to a specific analysis at the branch level. Each branch can be set to one of the branch's specific past analyses. See the following section for information on setting a branch-level New Code Period. 

### Setting a branch-level New Code Period
_Branch analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._  
For projects with multiple branches, you can set a New Code Period for each branch from the **Actions** column of the branches table on the project's **New Code Period** settings page.

You can set a branch's New Code Period to the following:

* **Previous Version** – Set the New Code Period to show any changes made in your branch's current version. This works well for branches with regular versions.
* **Number of days** – Specify a number of days for a floating New Code Period. For example, setting **Number of Days** to 30 creates a floating New Code Period beginning 30 days from the current date.
* **Specific analysis** – Choose a specific past analysis of the branch as the New Code Period. The New Code Period will show any changes made since that analysis.
