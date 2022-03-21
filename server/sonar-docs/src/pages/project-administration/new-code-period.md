---
title: Defining New Code
url: /project-administration/new-code-period/
---

Defining what is considered New Code is an important part of SonarQube's Clean as You Code approach to code quality and safety. By focusing on code that's been added or changed since your New Code definition, you can set consistent quality requirements and expectations. Your New Code will be issue free and you'll clean up the code you encounter along the way. For more information on New Code and why it's important, check out [Clean as You Code](/user-guide/clean-as-you-code/).

## Setting your New Code definition

You can define New Code at the global, project, or branch level.

- **Global level**  
   You can set a global New Code definition at [**Administration > Configuration > General Settings > New Code**](/#sonarqube-admin#/admin/settings?category=new_code_period/). What you define as New Code at the global level will be the default for your projects.

- **Project level**  
   You can set a New Code definition for your project at **Project Settings > New Code**. What you define as New Code at the project level will be the default for the project's branches if you're using an edition that supports multiple branches (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)).

- **Branch level**  
   You can define New Code for each branch from the **Actions** column of the branches table on the project's **New Code** settings page if you're using an edition that supports multiple branches (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)).

## New Code definitions

You can define New Code as changes from a previous version, a specific analysis, a reference branch, or within a specific period (number of days):

- **Previous Version** – Define New Code as any changes made in your project's current version. This works well for projects with regular versions or releases.

   Available at the global, project, and branch level.

- **Specific analysis** – Choose a previous analysis as your New Code definition. Any changes made since that analysis are considered New Code.

   Available at the branch level starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and the project level for community edition.

[[info]]
| For Community Edition, past analysis is available at the project-level because Community Edition doesn't support multiple branches. Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can do this at the branch level, and each branch can be set to one of the branch's specific past analyses.

- **Reference Branch** – Choose a specific branch to define your New Code. Any changes made from your reference branch are considered New Code.

   Available at the project and branch level. You can also specify a reference branch using a scanner parameter, overriding the server's definition. See the [Scanner Analysis Parameters](/analysis/analysis-parameters/).

- **Number of days** – Specify a number of days for a floating New Code period. For example, setting **Number of Days** to 30 creates a floating New Code period beginning 30 days from the current date.
  Available at the global, project, and branch level.
