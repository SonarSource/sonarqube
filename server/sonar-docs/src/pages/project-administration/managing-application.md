---
title: Managing Applications
url: /project-administration/managing-applications/
---

*Applications are available starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html).*

## Permissions
There are two levels of users with permissions for adding and editing Applications: users with the Create Applications permission and Global Administrators.  

### Create Applications Permission
Users with the Create Applications permission (granted at the global level at [Administration > Security > Global Permissions](/#sonarqube-admin#/admin/permissions)) can create Applications by clicking the "+" in the top menu.

![Create Applications](/images/creatingportfoliosandapps.png)

Users with the Create Applications permission can edit an individual Application definition from the Application-level **Portfolio Settings > Edit Definition** interface.

### Global Administrators
In addition to the access granted to users with the Create Applications permission, Global Administrators have access to the overall Portfolio and Application administration interface at **[Administration > Configuration > Portfolios](/#sonarqube-admin#/admin/extension/governance/views_console)**. From this page, they can create and edit Applications. 

Global Administrators also have access to the Projects Management page at **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management)**. Changing the selection mechanism on this page to “Portfolios” or “Applications” lets you manage the Portfolios or Applications of your SonarQube instance. The dropdown menu to the right of each item lets you edit permissions, apply Permission Templates, or restore access to a Portfolio or Application.

## Populating Applications
Once your Application exists, you can populate it with manually-selected projects. By default, the configuration interface shows the list of projects currently selected for the application. To add additional projects, choose the "Unselected" or "All" filter.

## Creating Application Branches
Once your Application is populated with projects, you can create application branches by choosing branches from the Application's component projects. This option is available in the Application's **Application Settings > Edit Definition** interface, or from the global administration interface.

## Calculation
By default, Applications are queued to be recalculated after each analysis of an included project. For each relevant Application, a “Background Task” is created, and you can follow the progress on each in the **[Administration > Projects > Background Tasks](/#sonarqube-admin#/admin/background_tasks)** by looking at the logs available for each item.

## Reindexing
During Elasticsearch reindexing due to disaster recovery or upgrading, Applications will become available after each Application is indexed.