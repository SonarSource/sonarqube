---
title: Applications
url: /user-guide/applications/
---

*Applications are available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html).*

## Using Applications

An Application aggregates projects into a synthetic project. Assume you have a set of projects which has been split for technical reasons, but which shares a life cycle; they interact directly in production and are always released together. With an Application, they can be treated as a single entity in {instance} with a unified Project Homepage, Issues list, Measures space, and most importantly: Quality Gate.

### Applications vs. Portfolios

Applications and Portfolios are both aggregations of projects, but they have different goals and therefore different presentations. A Portfolio is designed to be a very high-level, executive overview that shows how a package of projects that may only be tangentially related are doing quality-wise, and what the trends are. Applications allow you to see your set of projects as a larger, overall meta-project. For instance, because all the projects in an application ship together, if one of them isn't releasable then none of them are, and an Application's consolidated Quality Gate gives you an immediate summary of what must be fixed across all projects in order to allow you to release the set.

## Application Setup

You can create an Application by clicking the **Create Application** button in the upper-right corner of the **Projects** homepage. 

Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can also create and edit applications in the global Portfolio administration interface at **Administration > Configuration > Portfolios**. 

For more information on setting up Applications, see [Managing Applications](/project-administration/managing-applications/). 

### Populating Application Data

An Application is automatically re-calculated after each analysis of one of its projects. If you want immediate (re)calculation, a user with administration rights on the Application can use the **Recompute** button in the Application-level **Application Settings > Edit Definition** interface. 

Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), the global Portfolio administration interface: **Administration > Configuration > Portfolios** offers the ability to queue re-computation of all Applications and Portfolios at once.

## Applications and Branch Analysis

Branches are available for Applications. They allow you to aggregate branches from the projects in an Application.

**Note:** Avoid adding branches to your application that will be deleted to prevent issues with your Application status.

Once an Application has been set up, anyone with administration rights on the Application can manually create a new branch in the **Application Settings > Edit Definition** interface. In Enterprise Edition and above, you can also manage branches from the global **Administration > Configuration > Portfolios** interface. For each Application branch you can choose which project branch should be included, or whether the project should be represented in the branch at all.