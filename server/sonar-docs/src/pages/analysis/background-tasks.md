---
title: Background Tasks
url: /analysis/background-tasks/
---

A Background Task can be:
* the import of an Analysis Report
* the computation of a Portfolio
* the import or export of a project

## What happens after the scanner is done analyzing?

Analysis is not complete until the relevant Background Task has been completed. Even though the SonarScanner's log shows `EXECUTION SUCCESS`, the analysis results will not be visible in the {instance} project until the Background Task has been completed. After a SonarScanner has finished analyzing your code, the result of the analysis (Sources, Issues, Metrics) -  the Analysis Report - is sent to {instance} Server for final processing by the Compute Engine. Analysis Reports are queued and processed serially.

At the Project level, when there is a pending Analysis Report waiting to be consumed, you have a "Pending" notification in the header, next to the date of the most recent completed analysis.

Global Administrators can view the current queue at **[Administration > Projects > Background Tasks](/#sonarqube-admin#/admin/background_tasks)**. Project administrators can see the tasks for a project at **Administration > Background Tasks**.

## How do I know when analysis report processing fails?
Background tasks usually succeed, but sometimes unusual circumstances cause processing to fail. Examples include:

* running out of memory while processing a report from a very large project
* hitting a clash between the key of an existing module or project and one in the report
* ...

When that happens, the failed status is reflected on the project homepage, but that requires someone to notice it. You can also choose to be notified by email when background tasks fail - either on a project by project basis, or globally on all projects where you have administration rights, in the **Notifications** section of your profile.

## How do I diagnose a failing background task?
For each Analysis Report there is a dropdown menu allowing you to access to the "Scanner Context" showing you the configuration of the Scanner at the moment when the code scan has been run.

If processing failed for the task, an additional option will be available: "Show Error Details", to get the technical details why the processing of the Background Task failed.

## How do I cancel a pending analysis report?
Administrators can cancel the processing of a pending task by clicking:

* on the red 'x' available on each line of a `Pending` task
* on the red "bulk cancel" option next to the pending jobs count. This button cancels all pending tasks.

Once processing has begun on a report, it's too late to cancel it.

