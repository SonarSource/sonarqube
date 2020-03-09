---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

## Release 8.3 Upgrade Notes  

**Analysis summary for GitHub Pull Requests**
* Pull Request analysis can be shown under the Conversation tab in GitHub. You can enable or disable it at **Project Settings > General Settings > Pull Request Decoration**. 
* If you already have Pull Request analysis under the GitHub Checks tab, you'll need to update your GitHub App to give Pull Requests read & write access. For more information see [Decorating Pull Requests](/analysis/pr-decoration/). ([MMF-1892](https://jira.sonarsource.com/browse/MMF-1892)).

**Jenkins Automatic Branch and Pull Request Detection**  
With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), Scanners now automatically detects branches and Pull Requests in Jenkins Multibranch Pipelines. You no longer need to pass branch and Pull Request parameters. When upgrading from Community Edition or an old commercial edition version, the branch name in your SonarQube project needs to match the branch name in your code repository to continue writing history to the branch. Because SonarQube renames the Main Branch "master" by default, you may have to rename it before running analysis again. See the [Jenkins CI Integration](/analysis/jenkins/) page for more information. ([MMF-1676](https://jira.sonarsource.com/browse/MMF-1676)).

## Release 8.2 Upgrade Notes  
**Security Hotspots: dedicated space and workflow**
* The Security Hotspots have a brand new space where developers can perform security reviews. The review process has been simplified. It's no longer necessary to transform a Security Hotspot into a Manual Vulnerability and back. A developer can now simply mark a Security Hotspot as Safe, Fixed, or leave it as-is if more time is needed. ([MMF-1868](https://jira.sonarsource.com/browse/MMF-1868)).
* Manual Vulnerabilities created from Security Hotspots are migrated to Security Hotspots with the status "To Review". A comment "Migrated from Manual Vulnerability" is added to the review history to recognize them.  
* A Security Hotspots Reviewed metric has been added and is available to Quality Gates along with the Security Review Rating.

**New project homepage**  
The project homepage has been redesigned to focus on New Code. ([MMF-1886](https://jira.sonarsource.com/browse/MMF-1886)). Projects details are now tucked into a new "Project information" pane. The project administration menu has been renamed "Project Settings".

**Deprecated configuration**  
The old way of referencing environment variables in server configuration is deprecated and replaced with the support of default environment variables. ([SONAR-13113](https://jira.sonarsource.com/browse/SONAR-13113)).

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15301)

## Release 8.1 Upgrade Notes  
**Short-lived and Long-lived branches are now just branches**  
The concept for branches is now simplified, with a single way to handle all of them. ([MMF-1786](https://jira.sonarsource.com/browse/MMF-1786)).
* Analysis is the same for all branches. The parameter `sonar.branch.target` is no longer used and can be removed.
* All branches behave as previous Long-lived branches: all measures are available. The New Code period is configurable and starts by default after the first analysis. The Quality Gate check applies on all conditions.
* As a consequence, branches that were previously Short-Lived branches may display incomplete measures before they are analyzed again. With the first analysis, measures on New Code and the Quality Gate status may change.
* New housekeeping settings replace the Long-lived branch pattern and allow you to choose the branches which should be kept when inactive.
* Detection of new issues in branches and PRs is simplified. The list of issues reported as new may change slighlty. ([SONAR-12627](https://jira.sonarsource.com/browse/SONAR-12627)).

**Configuration of Pull Request decoration**  
The configuration of Pull Request decoration changes. Previous settings are replaced by a new configuration in the UI. Also, decoration of Pull Requests now supports multiple instances of a same ALM provider in Enterprise Edition and above. ([MMF-1814](https://jira.sonarsource.com/browse/MMF-1814)).

**Deprecated web services and parameters dropped**  
Some Web services and parameters which were deprecated in 6.x versions have been dropped, including some related to Quality Profiles. See Full Release Notes for more info.

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15243)


## Release 8.0 Upgrade Notes  
**GitHub, LDAP, and SAML authentication now built in**  
GitHub, LDAP, and SAML authentication is now built in. If you were using the authentication plugins (sonar-ldap, sonar-auth-github, and sonar-auth-saml), you need to remove them from SonarQube before upgrading. ([SONAR-12471](https://jira.sonarsource.com/browse/SONAR-12471)).

**GitLab Authentication now available**  
GitLab OAuth2 authentication is now available in all editions. If you were using the community plugin, you need to remove it from SonarQube before upgrading. The configured variable of the plugin will be migrated, so the authentication will work without having to rewrite the configuration. Due to changes in group mapping, GitLab subgroups mapped using the community plugin will need to be renamed in SonarQube for the mapping to work. ([SONAR-12460](https://jira.sonarsource.com/browse/SONAR-12460)).

**New Code Period values simplified**  
It's now easier to set your New Code Period in the UI. With the new settings, specific analysis has replaced setting the New Code Period to a specific date or version. If you were using a specific date or version for your New Code Period, now you'll need to use a specific analysis. See the [Setting Your New Code Period](/project-administration/new-code-period/) for more info. ([MMF-1579](https://jira.sonarsource.com/browse/MMF-1579)).  

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=14962)


## Release 7.9.1 LTS Upgrade Notes  
**Upgrade on Microsoft SQL Server fixed**  
Upgrade failure and performance issues with Microsoft SQL Server have been fixed ([SONAR-12260](https://jira.sonarsource.com/browse/SONAR-12260), [SONAR-12251](https://jira.sonarsource.com/browse/SONAR-12251)).

**Pylint execution on Windows fixed**  
Automatic execution of Pylint during python analysis on Windows has been fixed. Note that automatic execution of pylint during analysis remains deprecated on all OSes. ([SONAR-12274](https://jira.sonarsource.com/browse/SONAR-12274)).

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15029)


## Release 7.9 LTS Upgrade Notes  
**Upgrade can fail on Microsoft SQL Server**  
Migration from SonarQube v6.7.x to v7.9 fails on Microsoft SQL Server ([SONAR-12260](https://jira.sonarsource.com/browse/SONAR-12260)). 

**MySQL No Longer Supported**  
SonarQube no longer supports MySQL. To migrate from MySQL to a supported database, see the free [MySQL Migrator tool](https://github.com/SonarSource/mysql-migrator).

**Java 11 Required**  
The SonarQube server now requires Java 11. Analyses may continue to use Java 8 if necessary.

**Pylint should be run manually**  
Running Pylint automatically during python analysis has been deprecated. Additionally, it is broken in this version on Windows. If needed, Pylint must be run ahead of time and the resulting report passed in to analysis. 

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=14945)