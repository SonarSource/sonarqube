---
title: Portfolios
url: /user-guide/portfolios/
---

*Portfolios are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

### Portfolios Home Page

The Portfolio Home Page is the central place for managers and tech leads to keep an eye on the Releasability of the projects under their supervision. Releasability is based on the project's quality gate: green (pass) is releasable. Red (error) is not. Each Portfolio home page offers an aggregate view of the releasability of all the projects in the Portfolio.

At the top of the page, you can easily see whether overall Portfolio is currently rated as releasable and if any projects in the Portfolio have failed their Quality Gate. And the Reliability, Security, and Maintainability ratings show the overall health of the Portfolio in these three domains, along with an indicator of the worst-performing project(s) in each domain.

For each domain you see:

* the rating (see [Metric Definitions](/user-guide/metric-definitions/) for more details about how they are computed)
* an indicator of when the rating last changed
* an indicator of the worst-performing project(s) in the domain

### Releasability Rating

The Releasability Rating tells you the ratio of projects in the Portfolio that do NOT have a **FAILED** Quality Gate (ie QG being **PASSED**) :

**A**: > 80%  
**B**: > 60%  
**C**: > 40%  
**D**: > 20%  
**E**: <= 20%  

### Reliability, Security and Maintainability Ratings

Each of the Reliability, Security and Maintainability Ratings for a Portfolio is calculated as the average of the ratings for all projects included in the Portfolio. SonarQube converts the rating for each project to a number (see conversion table below), calculates an average for the portfolio and converts that average back to a rating. Averages that land exactly on the 0.5 mark are rounded up (i.e. the result is the "lower" of the two possible ratings, so an average of 2.5 would result in a "C" rating).

This gives an “issue density" measure on the three axes of Reliability, Security and Maintainability for your Portfolio.

Rating conversion:

**E**: 5  
**D**: 4  
**C**: 3  
**B**: 2  
**A**: 1  

*Note: the Portfolio Home Page is also available at Sub-Portfolio level*

### Print as PDF or Subscribe

On a Portfolio Home Page you can choose to download an overview of the Portfolio as a PDF. To do that, simply click on the "Print as PDF" button. This is really convenient, for example, if you're going into a meeting where you may not have access to your SonarQube instance.

If you don't want to perform this action every time, you can subscribe to receive the PDF by email. The frequency of the mailing is decided by the administrator of the Portfolio.

Please note you will receive the PDF only if the Portfolio is computed.

Portfolios are created and edited in the global Portfolio administration interface: **Administration > Configuration > Portfolios**. For more information, see [Configuring Portfolios and Applications](/project-administration/configuring-portfolios-and-applications/).
