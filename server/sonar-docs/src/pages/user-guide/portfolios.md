---
title: Portfolios
url: /user-guide/portfolios/
---

*Portfolios are available starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html).*

## Portfolios Home Page

The Portfolio Home Page is the central place for managers and tech leads to keep an eye on the Releasability of the projects under their supervision. Releasability is based on the portfolio's projects' [quality gates](/user-guide/quality-gates/). Each Portfolio home page offers an aggregate view of the releasability of all projects in the Portfolio.

At the top of the page, you can  see the overall releasablilty of the Portfolio, a graph showing the releasability trend, and the number of project branches that are failing and passing their quality gate. 

The Reliability, Security Vulnerabilities, Security Review, and Maintainability ratings show the overall health of the Portfolio on both New Code and Overall Code. 

Below the New Code rating for each metric, you'll see how many project branches are at risk and how many are doing well. 

Below the Overall Code rating, you'll see a graph showing the trend for each metric. Additionally, you'll see the number of at risk project branches.

## Releasability Rating

The Releasability rating is the ratio of projects in the Portfolio that have a **Passed** Quality Gate:

**A**: > 80%  
**B**: > 60%  
**C**: > 40%  
**D**: > 20%  
**E**: <= 20%  

## Reliability, Security Vulnerabilities, Security Review, and Maintainability Ratings

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

## Portfolio PDF Report

On a Portfolio Home Page, you can download a PDF overview of the Portfolio by selecting **Download as PDF** from the "Portfolio PDF Report" dropdown menu in the upper-right corner. This is really convenient, for example, if you're going into a meeting where you may not have access to your SonarQube instance.

You can subscribe to receive a PDF by email by selecting **Subscribe** from the "Portfolio PDF Report" dropdown. You can set the frequency of the report at the portfolio and global levels to **daily**, **weekly**, or **monthly**. The default frequency is monthly.

**Note:** You will only receive the PDF if the Portfolio is computed.

Portfolios are created and edited in the global Portfolio administration interface: **Administration > Configuration > Portfolios**. For more information, see [Managing Portfolios](/project-administration/managing-portfolios/).