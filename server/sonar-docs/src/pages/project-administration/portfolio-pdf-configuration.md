---
title: PDF Reports
url: /project-administration/portfolio-pdf-configuration/
---

*PDF Reports are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

PDF reports give a periodic, high-level overview of the overall code quality and security of your projects, applications, or portfolios.

## Project and Application PDF Reports

Users with access to a project or application can download a PDF report or subscribe to receive PDF reports from the **Project/Application PDF Report** drop-down menu in the upper-right corner of the project or application's home page. The frequency that you receive reports is set by a project or application administrator.

### Changing PDF subscription frequency
Users with administrative rights on a project or application can configure how frequently SonarQube sends PDF reports. You can change the frequency all projects and applications at a global level or for each project or application individually: 

- **Global-level** – To change the frequency setting globally, navigate to **Administration > General Settings > Governance**. Under **Project and Application PDF Reports**, select an option from the **PDF Reports Frequency** drop-down menu. 

- **Project-level** – To change the frequency setting for a specific project, navigate to the project's home page then **Project Settings > General Settings > Governance**. Under **Project and Application PDF Reports**, select an option from the **PDF Reports Frequency** drop-down menu.

- **Application-level** – To change the frequency setting for a specific application, navigate to the application's home page then **Application Settings > Application Report Settings**, and select an option from the **Application Reports Frequency** drop-down menu.

You have the following options for subscription frequency:

- **Daily** – report is sent on a daily basis.
- **Weekly** – report is sent on a weekly basis.
- **Monthly (default)** – report is sent on a monthly basis.

### Temporary branches
You cannot download or subscribe to a PDF report for a temporary branch. If you are unable to download or subscribe to a PDF report for a branch, go to **Project Settings > Branches & Pull Requests** and make sure that the **Keep when inactive** toggle is on for that branch.

## Portfolio PDF Reports
Users with access to a portfolio can download a PDF report or subscribe to receive PDF reports from the **Portfolio PDF Report** drop-down menu in the upper-right corner of the portfolio's home page. The frequency that you receive reports is set by a portfolio administrator. See the following section for more information.

### Changing PDF subscription frequency
Users with administrative rights on a portfolio can configure how frequently SonarQube sends PDF reports. You can change the frequency for all portfolios globally or for each portfolio individually. 

- **Global-level** – To change the frequency setting at a global level, navigate to **Administration > Portfolios**, and select an option from the **Frequency** drop-down menu. 

- **Portfolio-level** – To change the frequency setting for a specific portfolio, navigate to the portfolio's home page then **Portfolio Settings > Executive Report** and select an option under **Frequency**. 

You have the following options for subscription frequency:

- **Daily** – report is sent during the first portfolio calculation of the day (if any)
- **Weekly** – report is sent during the first portfolio calculation of the week (if any) from Monday
- **Monthly (default)** – report is sent during the first portfolio calculation of the month (if any), starting from the first day of the current month

### Sending portfolio PDF reports to non-SonarQube users
Users with administrative rights on a portfolio can send the portfolio PDF report to non-SonarQube users by adding their email in the **Other Recipients** field at **Portfolio Settings > Executive Report**.

## Regulatory PDF Reports

Users with access to a project can download a regulatory report for any permanent branch of that project.
A permanent branch is one that has been set to **Keep when inactive** (see [Branch Analysis](/branches/overview/) for details on how to adjust this setting).

To download the regulatory report, go to **Project Information > Regulatory Report** and select the desired branch.
The report will be dynamically generated and downloaded.
This may take a few minutes.

The report is a zip file containing a snapshot of the selected branch. It contains:

* An overview of the the selected branch of the project.
* The configuration items relevant to the project's quality (quality profile, quality gate, and analysis exclusions).
* Lists of findings for both new and overall code on the selected branch.
