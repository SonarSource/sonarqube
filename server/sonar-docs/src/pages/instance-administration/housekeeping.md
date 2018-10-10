---
title: Housekeeping
url: /instance-administration/housekeeping/
---

When you run a new analysis of your project or its branches or pull requests(PRs), some data that was previously available is cleaned out of the database. For example the source code of the previous analysis, measures at directory and file levels, and so on are automatically removed at the end of a new analysis. Additionally, some old analysis snapshots, PR analyses, and branches are also removed.

Why? Well, it's useful to analyze a project frequently to see how its quality evolves. It is also useful to be able to see the trends over weeks, months, years. But when you look back in time, you don't really need the same level of detail as you do for the project's current state. To save space and to improve overall performance, the Database Cleaner deletes some rows in the database. Here is its default configuration:

* For each project:
  * only one snapshot per day is kept after 1 day. Snapshots marked by an event are not deleted.
  * only one snapshot per week is kept after 1 month. Snapshots marked by an event are not deleted.
  * only one snapshot per month is kept after 1 year. Snapshots marked by an event are not deleted.
  * only snapshots with version events are kept after 2 years. Snapshots without events or with only other event types are deleted.
  * **all snapshots** older than 5 years are deleted, including snapshots marked by an event. 
* All closed issues more than 30 days old are deleted
* History at package/directory level is removed

These settings can be changed at [Administration > General > Database Cleaner](/#sonarqube-admin#/admin/settings).
