---
title: Managing Portfolios
url: /project-administration/managing-portfolios/
---

*Portfolios are available starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html).*

This page has information on managing and setting up Portfolios. For a general overview of Portfolios, see the [Portfolios](/user-guide/portfolios/) page in the User Guide.

## Permissions
Global Administrators and users with the **Create Portfolios** permission can add and edit Portfolios. 

### Create Portfolios Permission

Users with the Create Portfolios permission (granted at the global level at [Administration > Security > Global Permissions](/#sonarqube-admin#/admin/permissions)) can create Portfolios by clicking the **Create Portfolio** button in the upper-right corner of the **Portfolios** homepage.

These users can also edit an individual Portfolio definition from the Portfolio-level **Portfolio Settings > Edit Definition** interface.

### Global Administrators

In addition to the access granted to users with the Create Portfolios permission, Global Administrators have access to the overall Portfolio and Application administration interface at **[Administration > Configuration > Portfolios](/#sonarqube-admin#/admin/extension/governance/views_console)**. From this page, they can create and edit Portfolios. 

Global Administrators also have access to the Projects Management page at **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management)**. Selecting **Portfolios** from the first drop-down menu lets you manage the Portfolios or Applications of your SonarQube instance. The drop-down menu to the right of each item lets you edit permissions, apply Permission Templates or restore access to a Portfolio or Application.

## Populating Portfolios
After you've created a portfolio, you can populate it with projects, applications, and sub-portfolios.

### Adding a Sub-portfolio
To add a sub-portfolio, from **[Administration > Configuration > Portfolios](/#sonarqube-admin#/admin/extension/governance/views_console)** click the **Add Portfolio** button at the top of the third column, and choose:

* **Standard** - This option allows you to create a new sub-Portfolio from scratch. Once created, you can add projects, applications, and more layers of sub-portfolios.
* **Local Reference** - This option allows you to reference an existing Portfolio/Application as a sub-portfolio. Once added, it is not editable here, but must be chosen in the left-most column to be edited.

### Adding a project to a Portfolio
To add projects to a Portfolio, navigate to the Portfolio you want to add a project to. Select **Edit Definition** from **Portfolio Settings**. Click the pencil icon next to **Project selection mode**, and select one of the following options:

* **Manual** – choose the projects individually.
* **Tags** - select one or more project tags. Projects with those tags will automatically be included in the Portfolio.
* **Regular Expression** – specify a regular expression and projects with a matching name OR key will be included.
* **All Projects** – choose this option to add all projects not already included in this Portfolio (directly or via sub-Portfolio).

By default, adding a project to a portfolio shows the analysis of the project's main branch. See the following section if you want to select a non-main branch or multiple branches for your project.

#### **Selecting specific project branches for your portfolio**
In some situations, you may want to either monitor a project branch that's not your main branch or multiple project branches. For example:

* Your project has multiple release branches, and you want to monitor them all in a Portfolio.
* Your project's main branch isn't your release branch, and you want to monitor your release branch in your Portfolio.

To specify a project branch or branches to monitor in your portfolios, you can do the following:

* **Manual** – You can use manual selection to select one or multiple project branches. To do this: 
   1. From the Portfolio you want to edit, go to **Portfolio Settings > Edit Definition**.
   2. Click the pencil icon next to the **Project selection mode**, set **Manual** as your **Project Selection Mode**, and click **Save**.
   3. click the pencil icon next to the project you want to monitor. 
   4. Select the branches you want to monitor. If you don't select a branch, the main branch is selected by default.
   
* **Tags, Regular Expressions, All Remaining Projects** – To specify a branch to monitor in your portfolios using the tags, regular expressions, or all remaining projects options, do the following:
   1. From the Portfolio you want to edit, go to **Portfolio Settings > Edit Definition**. 
   2. Click the pencil icon next to the **Project selection mode**, and select your desired **Project Selection Mode**.
   3. Enter the name of the branch you want to monitor in the **Branch selection** field, and click **Save**.

### Adding Applications to a Portfolio
To add an Application to a Portfolio, make sure your Application is [already created](/user-guide/applications/). Then:

1. Navigate to the Portfolios configuration page by going to **[Administration > Configuration > Portfolios](/#sonarqube-admint#/admin/extension/governance/views_console/)**.
2. Select the Portfolio where you want to add your Application.
3. Click **Add Portfolio**.
4. Select **Local Reference**.
5. Choose your Application from the drop-down menu and click **Add**.

[[info]]
|**Project unicity under a portfolio**<br/><br/>
|Projects, applications, and sub-portfolios can only appear once in any given hierarchy in order to avoid magnifying their impacts on aggregated ratings. The portfolio configuration interface has some logic to prevent obvious duplications (e.g. manually adding the same project), however in case of more subtle duplications (for example, due to regular expression or other bulk definition), then the calculation of that portfolio will fail with a helpful error message.

## Calculation
By default, Portfolios are queued to be recalculated after each analysis of an included project. For each relevant Portfolio, a “Background Task” is created, and you can follow the progress on each in the **[Administration > Projects > Background Tasks](/#sonarqube-admin#/admin/background_tasks)** by looking at the logs available for each item.

If you're having performance issues related to the automatic recalculation of large portfolios, you can specify the hour(s) at which you want them to be recalculated at **[Administration > Portfolios > Recalculation](/#sonarqube-admin#/admin/settings?category=portfolios)**. Portfolios are queued to be recalculated at the beginning of the hour(s) that you specify.

## Elasticsearch reindexing
During Elasticsearch reindexing due to disaster recovery or an upgrade, you won't have access to Portfolios until all projects are indexed.
