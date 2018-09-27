---
title: Configuring Portfolios and Applications
url: /instance-administration/configuring-portfolios-and-applications/
---

*Portfolios and Applications are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html).*

* **UI Path** : **Administration > Configuration > Portfolios**
* **Permission** : you must have the Global Permission "Administer System" to access this page

The Portfolios management interface allows you to configure both Portfolios and Applications. It is divided into two or three columns. On the left is the list of top-level Portfolios and Applications. If a Portfolio is chosen, two more columns will be shown: sub-Portfolios, and projects. If an Application is chosen, only the projects column will be available.

From here, you can edit or delete an existing Portfolio or Application, or create a new one.

### Creating
Use the “Create” button at the top-left of the interface to open the creation dialog. You’re required to provide a name. You can optionally specify a description and key. Visibility defaults to Public, but Private may be chosen. Private Portfolios and Applications are only visible to those explicitly granted the right.

### Populating Portfolios
Once your Portfolio exists, you can populate it with any mix of projects, Applications, and sub-portfolios. Applications may only be populated with projects.

### Adding a Sub-portfolio
To add a sub-portfolio, click on “Add Portfolio” at the top of the third column, and choose:

* **Standard** - This option allows you to create a new sub-Portfolio from scratch. Once created, you can add projects, applications, and more layers of sub-portfolios.
* **Local Reference** - This option allows you to reference an existing Portfolio/Application as a sub-portfolio. Once added, it is not editable here, but must be chosen in the left-most column to be edited.

### Adding Projects to a Portfolio
To add projects directly to a Portfolio or standard sub-Portfolio, first select the Portfolio in the left column, and the sub-Portfolio (if necessary) in the middle column.
There are four project selection modes:

* **Manual** – choose the projects individually.
* **Tags** - select one or more project tags. Projects with those tags will automatically be included.
* **Regular Expression** – specify a regular expression and projects with a matching name OR key will be included.
* **All Remaining Projects** – choose this option to add all projects not already included in this Portfolio (directly or via sub-Portfolio).

[[info]]
|**Project unicity under a portfolio**<br/><br/>
|Projects, applications and sub-portfolios can only appear once in any given hierarchy in order to avoid magnifying their impacts on aggregated ratings. The portfolio configuration interface has some logic to prevent obvious duplications (e.g. manually adding the same project), however in case of more subtle duplications (e.g. due to regex, or other bulk definition), then the calculation of that portfolio will fail with a helpful error message.
### Populating Applications

Once your application exists, you can populate it with manually-selected projects. By default, the configuration interface shows the list of projects currently selected for the application. To add additional projects, choose the "Unselected" or "All" filter.

### Creating branches

Once your application is populated with projects, you can create application branches by choosing long-lived branches from the application's component projects. This option is available in the Application's **Administration > Edit Definition** interface, or from the global administration interface.

### After Computation

When you launch the Computation, the real work is done on {instance} server side.  

For each Portfolio, a “Background Task” is created and you can follow the progress on each in the **Administration > Projects > Background Tasks** by looking at the logs available for each Portfolio.
