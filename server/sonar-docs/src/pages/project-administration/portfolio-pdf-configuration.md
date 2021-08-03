---
title: PDF Report Configuration
url: /project-administration/portfolio-pdf-configuration/
---

*PDF Reports are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

PDF reports give a periodic, high-level overview on the overall code quality and security of your projects, applications, or portfolios.

## Project and Application PDF Reports

Users with access to a project or application can download a PDF reportfrom the **Project PDF Report** drop-down menu in the upper-right corner of the project or application's home page.

## Portfolio PDF Reports
Users with access to a portfolio can download a PDF report or subscribe to receive PDF reports from the **Portfolio PDF Report** drop-down menu in the upper-right corner of the portfolio's home page. The frequency that you receive reports is set by a portfolio administrator. See the following section for more information.

### Changing PDF subscription frequency
Users with administrative rights on a portfolio can configure the frequency at which the PDF is sent. You can change the frequency globally for all portfolios or for each portfolio individually. 

To change the frequency setting at a global level, navigate to **Administration > Portfolios**, and select an option from the **Frequency** drop-down menu. 

To change the frequency setting at a portfolio level, navigate to a portfolio home page then **Portfolio Settings > Executive Report** and select an option under **Frequency**. 

You have the following options for subscription frequency:

* Daily: report is sent during the first portfolio calculation of the day (if any)
* Weekly: report is sent during the first portfolio calculation of the week (if any) from Monday
* Monthly (default): report is sent during the first portfolio calculation of the month (if any), starting from the first day of the current month

### Sending portfolio PDF reports to non-SonarQube users
Users with administrative rights on a portfolio can send the portfolio PDF report to non-SonarQube users by adding their email in the **Other Recipients** field at **Portfolio Settings > Executive Report**.