---
title: LTS to LTS Release Upgrade Notes
url: /setup/lts-to-lts-upgrade-notes/
---

These Upgrade Notes are intended for users who are directly upgrading from SonarQube _v7.9 LTS_ to _v8.9 LTS_. Just upgrading a few minor versions? Refer to the regular [Upgrade Notes](/setup/upgrade-notes/).

## Authentication
**Default Authentication and Administrator credentials (8.6)**  
On a fresh install to avoid misconfiguration and related security risks, authentication is now required by default, and you need to change the default password for the administrator account. 

When upgrading, if you were still using default credentials, you'll be asked to change the password the next time you authenticate with the admin account. ([MMF-1352](https://jira.sonarsource.com/browse/MMF-1352), [MMF-2146](https://jira.sonarsource.com/browse/MMF-2146)).

**Additional SAML checks (8.4)**  
SAML authentication adds additional checks for validating SAML responses from the identity provider. This could reveal a non-standard configuration that needs to be updated. Information will appear in the logs upon a failed login attempt in the event that the configuration needs to be tweaked.

**GitLab Authentication now available (8.0)**  
GitLab OAuth2 authentication is now available in all editions. If you were using the community plugin, you need to remove it from SonarQube before upgrading. The configured variable of the plugin will be migrated, so the authentication will work without having to rewrite the configuration. Due to changes in group mapping, GitLab subgroups mapped using the community plugin will need to be renamed in SonarQube for the mapping to work. ([SONAR-12460](https://jira.sonarsource.com/browse/SONAR-12460)).

## Analysis
**Updated built-in Quality Profiles (8.0-8.9)**  
The built-in Quality Profiles for each language have been updated, meaning rules may have been added, changed, deprecated or dropped. If you are using or extending any of the “Sonar way” built-in Quality Profiles, make sure to check their Changelog to see what has changed. 

**JavaScript security analysis can take longer (8.8)**  
The JavaScript security analysis in commercial editions has been overhauled for far better accuracy. This overhaul results in an expected increase in memory requirement for analysis. 

**JavaScript, TypeScript, and CSS analysis now requires Node.js 10+ (8.7, 8.8)**  
In order to analyze Javascript, Typescript, and CSS code, you now need to have Node.js 10+ installed on the machine running the scan.

**SonarScanner for MSBuild compatibility (and renaming) (8.5)**  
Analyzing a C# / VB.NET solution in SonarQube 8.5 requires SonarScanner for MSBuild 4.0+.

The SonarScanner for MSBuild has been renamed to the [SonarScanner for .NET](/analysis/scan/sonarscanner-for-msbuild/)

**New Code Period values simplified (8.0, 8.4)**  
It's now easier to set your New Code Period in the UI. With the new settings, specific analysis has replaced setting the New Code Period to a specific date or version. If you were using a specific date or version for your New Code Period, now you'll need to use a specific analysis. 

It is now also possible to set the New Code Period to be defined against an already analyzed branch, mimicking the New Code Period of what were previously short-lived branches.

See the [Setting Your New Code Period](/project-administration/new-code-period/) for more info. ([MMF-1579](https://jira.sonarsource.com/browse/MMF-1579)).

**Security Hotspots in the built-in Quality Gate (8.3)**   
We've added a new condition to the built-in "Sonar way" Quality Gate to make sure all Security Hotspots on New Code are reviewed. The Quality Gate fails if the percentage of new Hotspots reviewed is less than 100%. ([MMF-1907](https://jira.sonarsource.com/browse/MMF-1907)).

**Jenkins automatic branch and Pull Request detection (8.3)**  
With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), Scanners now automatically detect branches and Pull Requests in Jenkins Multibranch Pipelines. You no longer need to pass branch and Pull Request parameters. When upgrading from Community Edition or an old commercial edition version, the branch name in your SonarQube project needs to match the branch name in your code repository to continue writing history to the branch. Because SonarQube names the Main Branch "master" by default, you may have to rename it before running analysis again. See the [Jenkins CI Integration](/analysis/jenkins/) page for more information. ([MMF-1676](https://jira.sonarsource.com/browse/MMF-1676)).

**Updated .NET code coverage (8.3)**  
The code coverage for .NET projects now takes into account the branch/condition coverage in addition to the line coverage. The coverage of your projects may decrease to be closer to reality, and it can impact your Quality Gate. (See more details [here](https://community.sonarsource.com/t/c-vb-net-sonarqube-and-sonarcloud-support-branch-condition-coverage-data/22384)).

**Support for `.exec` format JaCoCo Coverage Reports dropped (8.2)**  
The `.exec` format for JaCoCo coverage reports is no longer supported.

Once upgraded, you will only be able to import `.xml` style reports. You should ensure that you are now [Importing JaCoCo coverage reports in XML format](https://community.sonarsource.com/t/coverage-test-data-importing-jacoco-coverage-report-in-xml-format/12151).

**Short-lived and Long-lived branches are now just branches (8.1, 8.4)**  
The concept for branches is now simplified, with a single way to handle all of them. ([MMF-1786](https://jira.sonarsource.com/browse/MMF-1786)).
* Analysis is the same for all branches. The parameter `sonar.branch.target` is no longer used and can be removed.
* All branches behave as previous Long-lived branches: all measures are available. The New Code period is configurable and starts by default after the first analysis. The Quality Gate check applies on all conditions.
* As a consequence, branches that were previously Short-Lived branches may display incomplete measures before they are analyzed again. With the first analysis, measures on New Code and the Quality Gate status may change.
* New housekeeping settings replace the Long-lived branch pattern and allow you to choose the branches which should be kept when inactive.
* Detection of new issues in branches and PRs is simplified. The list of issues reported as new may change slightly. ([SONAR-12627](https://jira.sonarsource.com/browse/SONAR-12627)).

## Integration
**GitHub Enterprise compatibility (8.9)**  
SonarQube 8.9 only supports GitHub Enterprise 2.21+ for pull request decoration (the previous minimum version was 2.15).

**Azure DevOps Services and Bitbucket Cloud are now supported (8.7)**  
SonarQube now officially supports Azure DevOps Services and Bitbucket Cloud. If you were running analysis using Bitbucket Pipelines previously, when you upgrade, the Main branch name in your SonarQube project needs to match the branch name in your code repository to continue writing history to the branch. You may have to rename it before running analysis again.

**Analysis summary for GitHub Pull Requests (8.3)**
* Pull Request analysis can be shown under the Conversation tab in GitHub. You can enable or disable it at **Project Settings > General Settings > Pull Request Decoration**. 
* If you already have Pull Request analysis under the GitHub Checks tab, you'll need to update your GitHub App to give Pull Requests read & write access. For more information see [Pull Requests](/analysis/pull-request/). ([MMF-1892](https://jira.sonarsource.com/browse/MMF-1892)).

**Configuration of Pull Request decoration (8.1)**  
The configuration of Pull Request decoration changes. Previous settings are replaced by a new configuration in the UI. Also, decoration of Pull Requests now supports multiple instances of a same ALM provider in Enterprise Edition and above. ([MMF-1814](https://jira.sonarsource.com/browse/MMF-1814)).

## Operations
**Plugins require risk consent (8.9)**  
When upgrading, if you're using plugins, a SonarQube administrator needs to acknowledge the risk involved with plugin installation when prompted in SonarQube. ([MMF-2301](https://jira.sonarsource.com/browse/MMF-2301)).

**Database support updated (8.9)**  
SonarQube 8.9 supports the following database versions:

* PostgreSQL versions 9.6 to 13. PostgreSQL versions <9.6 are no longer supported.
* MSSQL Server 2014, 2016, 2017, and 2019.
* Oracle XE, 12C, 18C, and 19C. Oracle 11G is no longer supported.

**Webhooks aren't allowed to target the instance (8.9)**  
To improve security, webhooks, by default, aren't allowed to point to the SonarQube server. You can change this behavior in the configuration. ([SONAR-14682](https://jira.sonarsource.com/browse/SONAR-14682)).

**Docker Images for commercial SonarQube Editions (8.2, 8.7)**  
If you wish to deploy SonarQube in a containerized environment, we recommend using the Docker Images provided by SonarSource available on [Docker Hub](https://hub.docker.com/_/sonarqube), now for all SonarQube editions. 

**Microsoft SQL Server and Integrated Authentication (8.7)**  
If you are using Microsoft SQL Server with Integrated Authentication, you will need to replace the `sqljdbc_auth.dll` file on your `PATH` with `mssql-jdbc_auth-9.2.0.x64.dll` from the [Microsoft SQL JDBC Driver 9.2.0 package](https://docs.microsoft.com/en-us/sql/connect/jdbc/release-notes-for-the-jdbc-driver?view=sql-server-ver15#92). See [Install the Server](/setup/install-server/) for more information.

**Elasticsearch update and change in cluster configuration (8.6)**  
For non-DCE editions, the Elasticsearch upgrade doesn't change the configuration. SonarQube automatically binds to the loopback address an additional Elasticsearch port which can be configured optionally.  

When running a cluster with Data Center Edition, the configuration of search nodes has changed. The old search properties will now fail. You need to configure two new sets of properties. See [Configure and Operate a Cluster](/setup/operate-cluster/) for more information.  

We recommend only giving external access to the application nodes and to the main port. ([SONAR-12686](https://jira.sonarsource.com/browse/SONAR-12686)).

**Upgrade simplified: Languages, Git and SVN, LDAP/GitHub/SAML support now built-in (8.0, 8.5)**  
All plugins related to languages, Git/SVN support, and LDAP/GitHub/SAML authentication are now built into SonarQube itself. If you were using these plugins, you need to remove them from your extensions/plugins directory before upgrading. Read more in this community guide: [SonarQube v8.5 and Beyond: Where did all the plugins go?](https://community.sonarsource.com/t/sonarqube-v8-5-and-beyond-where-did-all-the-plugins-go/32792) ([MMF-2042](https://jira.sonarsource.com/browse/MMF-2042))

**Updated system settings recommendation (8.4)**  
In previous versions, the recommended limits regarding threads, file descriptors, and vm.max_map_count were taken from Elasticsearch dependencies. This release can reach these limits occasionally, so we recommend increasing the following settings of your OS when upgrading:

* `vm.max_map_` count is greater than or equal to 524288
* `fs.file-max` is greater than or equal to 131072
* the user running SonarQube can open at least 131072 file descriptors
* the user running SonarQube can open at least 8192 threads

For more information, see the [Requirements](/requirements/requirements/) documentation. 

**Project, Application, and Portfolio availability when rebuilding Elasticsearch indexes (8.4)**  
From now on if your upgrade requires the rebuild of Elasticsearch indexes, your projects and Applications will become available as they are reindexed. Portfolios won't be available until all projects are reindexed. ([MMF-2010](https://jira.sonarsource.com/browse/MMF-2010))

**Deprecated configuration (8.2)**  
The old way of referencing environment variables in server configuration is deprecated and replaced with the support of default environment variables. ([SONAR-13113](https://jira.sonarsource.com/browse/SONAR-13113)).

## User Interface
**Applications on the Projects page (8.3)**  
[Applications](/user-guide/applications/) are now found on the Projects page. You can filter, favorite, and tag applications like you can with projects. ([MMF-1382](https://jira.sonarsource.com/browse/MMF-1382)).

**Security Hotspots: dedicated space and workflow (8.2)**  
* The Security Hotspots have a brand new space where developers can perform security reviews. The review process has been simplified. It's no longer necessary to transform a Security Hotspot into a Manual Vulnerability and back. A developer can now simply mark a Security Hotspot as Safe, Fixed, or leave it as-is if more time is needed. ([MMF-1868](https://jira.sonarsource.com/browse/MMF-1868)).
* Manual Vulnerabilities created from Security Hotspots are migrated to Security Hotspots with the status "To Review". A comment "Migrated from Manual Vulnerability" is added to the review history to recognize them.  
* The formula to compute the Security Review Rating, which was previously only available at the portfolio level, has been updated to be more meaningful. Historical values for this indicator have been removed to avoid confusion. ([MMF-1890](https://jira.sonarsource.com/browse/MMF-1890)).
* A Security Hotspots Reviewed metric has been added and is available to Quality Gates along with the Security Review Rating.

**New project homepage (8.2)**  
The project homepage has been redesigned to focus on New Code. ([MMF-1886](https://jira.sonarsource.com/browse/MMF-1886)). Projects details are now tucked into a new "Project information" pane. The project administration menu has been renamed "Project Settings".

## Web/Plugin API
**Deprecated web services have been dropped (8.1, 8.8)**  
Web services that were deprecated in 6.x versions have been dropped. ([SONAR-13848](https://jira.sonarsource.com/browse/SONAR-13848)).

**Changes in web services and plugin APIs (8.4)**  
The format of several IDs exposed in web services changed and their use is deprecated. See [SONAR-13248](https://jira.sonarsource.com/browse/SONAR-13248), [SONAR-13249](https://jira.sonarsource.com/browse/SONAR-13249), and [SONAR-13300](https://jira.sonarsource.com/browse/SONAR-13300).  
A related change is introduced in a plugin API method. See [SONAR-13420](https://jira.sonarsource.com/browse/SONAR-13420).
