---
title: Applications
url: /user-guide/applications/
---

*Applications are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

## Using Applications

An Application is an aggregation of projects into a synthetic project. Assume you have a set of projects which has been split for technical reasons, but which shares a lifecycle; they interact directly in production and are always released together. With an Application, they can be treated as a single entity in {instance} with a unified Project Homepage, Issues list, Measures space, and most importantly: Quality Gate.

### Applications vs. Portfolios

Applications and Portfolios are both aggregations of projects, but they have different goals and therefore different presentations. A Portfolio is designed to be a very high-level, executive overview that shows how a package of projects that may only be tangentially related are doing quality-wise, and what the trends are. Applications allow you to see your set of projects as a larger, overall meta-project. For instance, because all the projects in an application ship together, if one of them isn't releasable then none of them are, and an Application's consolidated Quality Gate gives you an immediate summary of what must be fixed across all projects in order to allow you to release the set.

## Application Setup

Applications are created and edited in the global Portfolio administration interface: **Administration > Configuration > Portfolios**. For more, see [Configuring Portfolios and Applications](/project-administration/configuring-portfolios-and-applications/). Applications must be created initially by a user with global administration rights, but after set-up, administration of an individual Application can be delegated to other users.

### Populating Application Data

An Application is automatically re-calculated after each analysis of one of its projects. If you want immediate (re)calculation, a user with administration rights on the Application can use the **Recompute** button in the Application-level **Administration > Edit Definition** interface. The global Portfolio administration interface: **Administration > Configuration > Portfolios** offers the ability to queue re-computation of all Applications and Portfolios at once.

## Applications and Branch Analysis

Long-lived Branches are available for applications. They allow you to aggregate long-lived branches from the projects in an application.

Once an Application has been set up, anyone with administration rights on the Application can manually create a new branch in the **Administration > Edit Definition** interface. Branches can also be managed from the global **Administration > Configuration > Portfolios** interface. For each Application branch you can choose which project branch should be included, or whether the project should be represented in the branch at all.
