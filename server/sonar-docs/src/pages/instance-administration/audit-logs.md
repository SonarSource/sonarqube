---
title: Audit Logs
url: /instance-administration/audit-logs/
---
_Audit logs are available starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html)._

Administrators can download audit logs to maintain an audit trail of the security-related changes made to their SonarQube instance. See the following section for the types of changes tracked in the audit logs. 

## Tracked changes
Audit logs track the following changes in SonarQube:
- user accounts
- permissions
- global configuration of security-related settings
- creating, updating, or deleting of projects, applications, and portfolios
- installing or updating plugins
- setting or revoking licenses

## Downloading audit logs
Administrators can download audit logs at **Administration > Audit logs**. From here, you can select the time period that you want to download audit logs for. This is limited by your housekeeping settings. See the following section for more on setting your audit log housekeeping settings.

## Audit log housekeeping
You can set how often SonarQube deletes audit logs in the housekeeping settings at **[Administration > General > Housekeeping](/#sonarqube-admin#/admin/settings?category=housekeeping)**. By default, SonarQube deletes audit logs monthly. 

Setting your housekeeping policy to keep your audit logs for a long period of time (for example, only deleting logs yearly) can increase your database size and the amount of time it takes to download audit logs. To avoid this, we recommend downloading your audit logs at shorter intervals and storing them outside of SonarQube. 

We also recommend downloading and storing your audit logs outside of SonarQube if you need to maintain them for a longer period of time than can be set in the housekeeping settings.