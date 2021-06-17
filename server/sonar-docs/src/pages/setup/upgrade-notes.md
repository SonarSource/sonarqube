---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

## Release 9.0 Upgrade Notes  
**Scanners require Java 11**
Java 11 is required for SonarQube scanners. Use of Java 8 is no longer supported. See the documentation on [Moving Analysis to Java 11](/analysis/analysis-with-java-11/) for more information. ([MMF-2051](https://jira.sonarsource.com/browse/MMF-2051)).

**JavaScript custom rule API removed**  
The JavaScript custom rule API, which was previously deprecated, has been removed. Plugins can no longer use this API to implement custom rules. See the [JavaScript documentation](/analysis/languages/javascript/) for more information. ([SONAR-14928](https://jira.sonarsource.com/browse/SONAR-14928)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15682)

## Release 8.9 LTS Upgrade Notes  
Upgrading directly from SonarQube _v7.9 LTS_? Refer to the [LTS to LTS Release Upgrade Notes](/setup/lts-to-lts-upgrade-notes/).

**GitHub Enterprise compatibility**  
SonarQube 8.9 only supports GitHub Enterprise 2.21+ for pull request decoration (the previous minimum version was 2.15).

**Plugins require risk consent**  
When upgrading, if you're using plugins, a SonarQube administrator needs to acknowledge the risk involved with plugin installation when prompted in SonarQube. ([MMF-2301](https://jira.sonarsource.com/browse/MMF-2301)).

**Database support updated**  
SonarQube 8.9 supports the following database versions:

* PostgreSQL versions 9.6 to 13. PostgreSQL versions <9.6 are no longer supported.
* MSSQL Server 2014, 2016, 2017, and 2019.
* Oracle XE, 12C, 18C, and 19C. Oracle 11G is no longer supported.

**Webhooks aren't allowed to target the instance**  
To improve security, webhooks, by default, aren't allowed to point to the SonarQube server. You can change this behavior in the configuration. ([SONAR-14682](https://jira.sonarsource.com/browse/SONAR-14682)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16710)

## Release 8.8 Upgrade Notes  
**CSS analysis now requires Node.js 10+**  
In order to analyze CSS code, you now need to have Node.js 10+ installed on the machine running the scan.

**Deprecated web services have been dropped**  
Web services that were deprecated in 6.x versions have been dropped. ([SONAR-13848](https://jira.sonarsource.com/browse/SONAR-13848)).

**JavaScript security analysis can take longer**  
The JavaScript security analysis in commercial editions has been overhauled for far better accuracy. This overhaul results in an expected increase in memory requirement for analysis. Additionally, there is an impact on the duration of JavaScript taint analysis which can be significant for some projects. 

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16674)  

## Release 8.7 Upgrade Notes  
**JavaScript and TypeScript analysis now requires Node.js 10+**  
In order to analyze JavaScript or TypeScript code, you now need to have Node.js 10+ installed on the machine running the scan.

**Azure DevOps Services and Bitbucket Cloud are now supported**  
SonarQube now officially supports Azure DevOps Services and Bitbucket Cloud. If you were running analysis using Bitbucket Pipelines previously, when you upgrade, the Main branch name in your SonarQube project needs to match the branch name in your code repository to continue writing history to the branch. You may have to rename it before running analysis again.

**Microsoft SQL Server and Integrated Authentication**  
If you are using Microsoft SQL Server with Integrated Authentication, you will need to replace the `sqljdbc_auth.dll` file on your `PATH` with `mssql-jdbc_auth-9.2.0.x64.dll` from the [Microsoft SQL JDBC Driver 9.2.0 package](https://docs.microsoft.com/en-us/sql/connect/jdbc/release-notes-for-the-jdbc-driver?view=sql-server-ver15#92). See [Install the Server](/setup/install-server/) for more information.

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16540)  

## Release 8.6 Upgrade Notes  
**Elasticsearch update and change in cluster configuration**  
For non-DCE editions, the Elasticsearch upgrade doesn't change the configuration. SonarQube automatically binds to the loopback address an additional Elasticsearch port which can be configured optionally.  

When running a cluster with Data Center Edition, the configuration of search nodes has changed. The old search properties will now fail. You need to configure two new sets of properties. See [Configure and Operate a Cluster](/setup/operate-cluster/) for more information.  

We recommend only giving external access to the application nodes and to the main port. ([SONAR-12686](https://jira.sonarsource.com/browse/SONAR-12686)).

**Default Authentication and Administrator credentials**  
On a fresh install to avoid misconfiguration and related security risks, authentication is now required by default, and you need to change the default password for the administrator account. 
When upgrading, if you were still using default credentials, you'll be asked to change the password the next time you authenticate with the admin account. ([MMF-1352](https://jira.sonarsource.com/browse/MMF-1352), [MMF-2146](https://jira.sonarsource.com/browse/MMF-2146)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16435)  

## Release 8.5 Upgrade Notes  
**GitHub Enterprise compatibility**  
SonarQube 8.5 only supports GitHub Enterprise 2.15+ for pull request decoration (the previous minimum version was 2.14).

**SonarScanner for MSBuild compatibility**  
Analyzing a C# / VB.NET solution in SonarQube 8.5 requires SonarScanner for MSBuild 4.0+.

**Upgrade simplified: Languages, GIT and SVN support now built-in**  
Languages provided with your edition and support for GIT and SVN version control are now built-in and don’t require plugins. If you were using these plugins, you need to remove them from SonarQube before upgrading. ([MMF-2042](https://jira.sonarsource.com/browse/MMF-2042)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16342)

## Release 8.4 Upgrade Notes  
**Updated system settings recommendations**  
In previous versions, the recommended limits regarding threads, file descriptors, and vm.max_map_count were taken from Elasticsearch dependencies. This release can reach these limits occasionally, so we recommend increasing the following settings of your OS when upgrading:

* `vm.max_map_` count is greater than or equal to 524288
* `fs.file-max` is greater than or equal to 131072
* the user running SonarQube can open at least 131072 file descriptors
* the user running SonarQube can open at least 8192 threads

For more information, see the [Requirements](/requirements/requirements/) documentation. 

**Project, Application, and Portfolio availability when rebuilding Elasticsearch indexes**  
From now on if your upgrade requires the rebuild of Elasticsearch indexes, your projects and Applications will become available as they are reindexed. Portfolios won't be available until all projects are reindexed. ([MMF-2010](https://jira.sonarsource.com/browse/MMF-2010)).

**Additionnal SAML checks**  
SAML authentication adds additional checks for validating SAML responses from the identity provider. This could reveal a non-standard configuration that needs to be updated. Information will appear in the logs upon a failed login attempt in the event that the configuration needs to be tweaked.

**Changes in web services and plugin APIs**  
The format of several IDs exposed in web services changed and their use is deprecated. See [SONAR-13248](https://jira.sonarsource.com/browse/SONAR-13248), [SONAR-13249](https://jira.sonarsource.com/browse/SONAR-13249), and [SONAR-13300](https://jira.sonarsource.com/browse/SONAR-13300).  
A related change is introduced in a plugin API method. See [SONAR-13420](https://jira.sonarsource.com/browse/SONAR-13420).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15833)

## Release 8.3 Upgrade Notes  
**Security Hotspots in the built-in Quality Gate**   
We've added a new condition to the built-in "Sonar way" Quality Gate to make sure all Security Hotspots on New Code are reviewed. The Quality Gate fails if the percentage of new Hotspots reviewed is less than 100%. ([MMF-1907](https://jira.sonarsource.com/browse/MMF-1907)).

**Jenkins automatic branch and Pull Request detection**  
With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), Scanners now automatically detect branches and Pull Requests in Jenkins Multibranch Pipelines. You no longer need to pass branch and Pull Request parameters. When upgrading from Community Edition or an old commercial edition version, the branch name in your SonarQube project needs to match the branch name in your code repository to continue writing history to the branch. Because SonarQube names the Main Branch "master" by default, you may have to rename it before running analysis again. See the [Jenkins CI Integration](/analysis/jenkins/) page for more information. ([MMF-1676](https://jira.sonarsource.com/browse/MMF-1676)).

**Updated .NET code coverage**  
The code coverage for .NET projects now takes into account the branch/condition coverage in addition to the line coverage. The coverage of your projects may decrease to be closer to reality, and it can impact your Quality Gate. (See more details [here](https://community.sonarsource.com/t/c-vb-net-sonarqube-and-sonarcloud-support-branch-condition-coverage-data/22384)).

**Analysis summary for GitHub Pull Requests**
* Pull Request analysis can be shown under the Conversation tab in GitHub. You can enable or disable it at **Project Settings > General Settings > Pull Request Decoration**. 
* If you already have Pull Request analysis under the GitHub Checks tab, you'll need to update your GitHub App to give Pull Requests read & write access. For more information see [Pull Requests](/analysis/pull-request/). ([MMF-1892](https://jira.sonarsource.com/browse/MMF-1892)).

**Applications on the Projects page**  
[Applications](/user-guide/applications/) are now found on the Projects page. You can filter, favorite, and tag applications like you can with projects. ([MMF-1382](https://jira.sonarsource.com/browse/MMF-1382)).

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15640)

## Release 8.2 Upgrade Notes  
**Security Hotspots: dedicated space and workflow**
* The Security Hotspots have a brand new space where developers can perform security reviews. The review process has been simplified. It's no longer necessary to transform a Security Hotspot into a Manual Vulnerability and back. A developer can now simply mark a Security Hotspot as Safe, Fixed, or leave it as-is if more time is needed. ([MMF-1868](https://jira.sonarsource.com/browse/MMF-1868)).
* Manual Vulnerabilities created from Security Hotspots are migrated to Security Hotspots with the status "To Review". A comment "Migrated from Manual Vulnerability" is added to the review history to recognize them.  
* The formula to compute the Security Review Rating, which was previously only available at the portfolio level, has been updated to be more meaningful. Historical values for this indicator have been removed to avoid confusion. ([MMF-1890](https://jira.sonarsource.com/browse/MMF-1890)).
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
