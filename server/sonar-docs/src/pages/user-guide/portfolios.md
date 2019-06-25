---
title: Portfolios
url: /user-guide/portfolios/
---

*Portfolios are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

### Portfolios Home Page

The Portfolio Home Page is the central place for managers and tech leads to keep an eye on the Releasability of the projects under their supervision. Releasability is based on the project's quality gate: Passed is releasable and Failed is not. Each Portfolio home page offers an aggregate view of the releasability of all projects in the Portfolio.

At the top of the page, you can easily see whether the overall Portfolio is currently rated as releasable and if any projects in the Portfolio have failed their Quality Gate. And the Reliability, Security Vulnerabilities, Security Review, and Maintainability ratings show the overall health of the Portfolio in these three domains, along with an indicator of the worst-performing project(s) in each domain.

For each domain, you'll see:

* the rating (see [Metric Definitions](/user-guide/metric-definitions/) for more details about how they are computed)
* an indicator of when the rating last changed
* an indicator of the worst-performing project(s) in the domain

### Releasability Rating

The Releasability rating is the ratio of projects in the Portfolio that have a **Passed** Quality Gate:

**A**: > 80%  
**B**: > 60%  
**C**: > 40%  
**D**: > 20%  
**E**: <= 20%  

### Reliability, Security Vulnerabilities, Security Review, and Maintainability Ratings

The Reliability, Security Vulnerabilities, Security Review, and Maintainability ratings for a Portfolio are calculated as the average of the ratings for all projects included in the Portfolio. 

SonarQube converts each project's letter rating to a number (see conversion table below), calculates an average number for the projects in the portfolio, and converts that average to a letter rating. Averages ending with .5 are rounded up resulting in the "lower" of the two possible ratings, so an average of 2.5 would be rounded up to 3 and result in a "C" rating).

This gives an "problem density" measure on the four axes of Reliability, Security Vulnerabilities, Security Review, and Maintainability for your Portfolio.

Rating conversion:

**E**: 5  
**D**: 4  
**C**: 3  
**B**: 2  
**A**: 1  

*Note: the Portfolio Home Page is also available at Sub-Portfolio level*

### Portfolio PDF Report

On a Portfolio Home Page, you can download a PDF overview of the Portfolio by selecting **Download as PDF** from the "Portfolio PDF Report" dropdown menu in the upper-right corner. This is really convenient, for example, if you're going into a meeting where you may not have access to your SonarQube instance.

You can subscribe to receive a PDF by email by selecting **Subscribe** from the "Portfolio PDF Report" dropdown. You can set the frequency of the report at the portfolio and global levels to **daily**, **weekly**, or **monthly**. The default frequency is monthly.

**Note:** You will only receive the PDF if the Portfolio is computed.

Portfolios are created and edited in the global Portfolio administration interface: **Administration > Configuration > Portfolios**. For more information, see [Configuring Portfolios and Applications](/project-administration/configuring-portfolios-and-applications/).
