---
title: Managing Portfolios and Applications
url: /project-administration/configuring-portfolios-and-applications/
---

*Portfolios and Applications are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

## Permissions
Users with the Create Portfolios or Create Applications permission have access to the creation interfaces from the "+" item in the top menu.  

Global Administrators  have access to the overall Portfolio and Application administration interface at **[Administration > Configuration > Portfolios](/#sonarqube-admin#/admin/extension/governance/views_console)**. From this page, Portfolios and Applications can be created and edited. 

Users of either type can edit an individual Portfolio or Application definition from the lower-level **Administration > Edit Definition** interface.

Global Administrators also have access to the Projects Management page at **[Administration > Projects > Management](/#sonarqute-admin#/admin/projects_management)**. Changing the selection mechanism on this page to “Portfolios” or “Applications” lets you manage the Portfolios or Applications of your SonarQube instance. The dropdown menu to the right of each item lets you edit permissions, apply Permission Templates or restore access to a Portfolio or Application.

## Populating Portfolios
Once your Portfolio exists, you can populate it with any mix of projects, Applications, and sub-portfolios. Applications may only be populated with projects.

## Adding a Sub-portfolio
To add a sub-portfolio, click on “Add Portfolio” at the top of the third column, and choose:

* **Standard** - This option allows you to create a new sub-Portfolio from scratch. Once created, you can add projects, applications, and more layers of sub-portfolios.
* **Local Reference** - This option allows you to reference an existing Portfolio/Application as a sub-portfolio. Once added, it is not editable here, but must be chosen in the left-most column to be edited.

## Adding Projects to a Portfolio
To add projects directly to a Portfolio or standard sub-Portfolio first make sure the correct item is selected, then choose the **Project selection mode**:

* **Manual** – choose the projects individually.
* **Tags** - select one or more project tags. Projects with those tags will automatically be included.
* **Regular Expression** – specify a regular expression and projects with a matching name OR key will be included.
* **All Remaining Projects** – choose this option to add all projects not already included in this Portfolio (directly or via sub-Portfolio).

[[info]]
|**Project unicity under a portfolio**<br/><br/>
|Projects, applications and sub-portfolios can only appear once in any given hierarchy in order to avoid magnifying their impacts on aggregated ratings. The portfolio configuration interface has some logic to prevent obvious duplications (e.g. manually adding the same project), however in case of more subtle duplications (e.g. due to regex, or other bulk definition), then the calculation of that portfolio will fail with a helpful error message.

## Populating Applications
Once your Application exists, you can populate it with manually-selected projects. By default, the configuration interface shows the list of projects currently selected for the application. To add additional projects, choose the "Unselected" or "All" filter.

## Creating Application Branches
Once your Application is populated with projects, you can create application branches by choosing long-lived branches from the application's component projects. This option is available in the Application's **Administration > Edit Definition** interface, or from the global administration interface.

## Calculation
Applications and Portfolios are queued to be re-calculated after each analysis of an included project. For each relevant Portfolio and Application, a “Background Task” is created, and you can follow the progress on each in the **[Administration > Projects > Background Tasks](/#sonarqube-admin#/admin/background_tasks)** by looking at the logs available for each item.
