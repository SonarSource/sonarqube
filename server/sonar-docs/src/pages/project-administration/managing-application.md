---
title: Managing Applications
url: /project-administration/managing-applications/
---

*Applications are available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html).*

## Permissions

### Creating Applications
Both users with the Create Applications permission and global administrators can create Applications:

* **Create Applications permission** – Users with the Create Applications permission (granted at the global level at **Administration > Security > Global Permissions**) can create Applications by clicking the **Create Application** button in the upper-right corner of the Projects homepage.
* **Global Administrators** – In addition to creating Applications from the Projects homepage, global administrators (with the global Administer System permission granted at [**Administration > Security > Global Permissions**](/#sonarqube-admin#/admin/permissions)) can create Applications from the overall Portfolio administration interface at **Administration > Configuration > Portfolios**.

### Editing Applications
Users need to have either **Administer** permissions for any Applications that they want to edit (set on the specific Application's page at **Application Settings > Permissions**) or the global **Administer System** permission.

[[info]]
| Users with **Administer** permissions for an Application can see the list of projects that make up the Application even if they don't have browse permissions for those projects.

## Populating Applications
Once your Application exists, you can populate it with manually-selected projects. By default, the configuration interface shows the list of projects currently selected for the application. To add additional projects, choose the "Unselected" or "All" filter.

## Creating Application Branches
Once your Application is populated with projects, you can create application branches by choosing branches from the Application's component projects. This option is available in the Application's **Application Settings > Edit Definition** interface, or from the global administration interface.

## Calculation
By default, Applications are queued to be recalculated after each analysis of an included project. For each relevant Application, a “Background Task” is created, and you can follow the progress on each in the **[Administration > Projects > Background Tasks](/#sonarqube-admin#/admin/background_tasks)** by looking at the logs available for each item.

## Reindexing
During Elasticsearch reindexing due to disaster recovery or upgrading, Applications become available as they are indexed.