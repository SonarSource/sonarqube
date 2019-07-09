---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

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


## Release 7.8 Upgrade Notes
**MySQL Deprecation and Migration**  
This is the last version that will support MySQL. To migrate from MySQL to a supported database, see the free [MySQL Migrator tool](https://github.com/SonarSource/mysql-migrator).

**Elasticsearch bootstrap checks enforced**  
SonarQube will now fail to start if Elasticsearch's bootstrap checks fail. That means you may need to [adjust the maximum number of open files and processes](/requirements/requirements/) for the SonarQube user as part of this upgrade ([SONAR-11264](https://jira.sonarsource.com/browse/SONAR-11264)). 

**Scanner version compatibility**  
Only the following scanner versions are compatible with SonarQube 7.8:
* SonarQube Scanner CLI 2.9+
* SonarQube Scanner Maven 3.3.0.603+
* SonarQube Scanner Gradle 2.3+

**Analysis fails with old branch parameter**
`sonar.branch` was deprecated in 6.7. With this version analysis fails when it is used. Where it is still in use, simply remove the `sonar.branch` property and update your `sonar.projectKey` value to `key:branch`.

**Notifications changes**  
Several changes have been made to notificatons. The notifications algorithm has been replaced with one that offers better performance during background task processing. Issue change notifications spawned by analysis or bulk change now generate only one email per event rather than one email per issue. The ability to subscribe globally to new issues notifications and notifications for issues resolved as False Positive or Won't fix has been dropped, as have all such subscriptions. Issue-related notifications on PRs have also been dropped.

**Webhook payloads now signed**  
It is now possible to verify that webhook payloads actually come from SonarQube via the `X-Sonar-Webhook-HMAC-SHA256` HTTP header. ([SONAR-12000](https://jira.sonarsource.com/browse/SONAR-12000))

**Graceful shutdown**  
The SonarQube server now shuts down gracefully. I.E. it completes any currently-processing background tasks before shutting down. This may mean that shutdown takes longer than previously. ([SONAR-12043](https://jira.sonarsource.com/browse/SONAR-12043))

**Duplication density correction**  
A bug affecting the calculation of duplication density has been fixed. Each project's duplication density value will likely rise at the next analysis, possibly affecting Quality Gate status. ([SONAR-12188](https://jira.sonarsource.com/browse/SONAR-12188))

**Additional authentication methods embedded**  
The SAML and GitHub Authentication plugins are now embedded in all editions ([SONAR-11894](https://jira.sonarsource.com/browse/SONAR-11894))

**Deprecated web services dropped**  
Web services that were deprecated in 5.x versions have been dropped. ([SONAR-11876](https://jira.sonarsource.com/browse/SONAR-11876))

[Full Release Notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=14939)

## Release 7.7 Upgrade Notes
**Deprecated parameters dropped**  
`sonar.language`, and  `sonar.profile`, both deprecated since 4.5, are dropped in this version as is `sonar.analysis.mode`, which as been deprecated since 6.6. These now-unrecognized parameters will simply be ignored, rather than failing analysis.

**PR decoration below GitHub Enterprise 2.14 swapped for GitHub checks**  
This version adds support for GitHub Enterprise (GHE) checks, which were introduced in GHE 2.14, and drops support for PR decoration in GHE versions prior to 2.14. To use the new checks implementation, an application will need to be created in GHE, and further configuration will be required via the SonarQube UI. ([Details in the docs.](/instance-administration/github-application/).)

**ElasticSearch update requires index rebuild, potentially more filespace**  
While it is generally possible to keep ElasticSearch indices in an upgrade (see [Configuring the Elasticsearch storage path](/setup/install-server/)), this version's upgrade of ElasticSearch will force all indices to be rebuilt. Additionally, more filespace may be required for this version's data ([SONAR-11826](https://jira.sonarsource.com/browse/SONAR-11826)).

**32-bit architecture support dropped**  
Support for 32-bit architectures has been dropped as part of this version's upgrade of ElasticSearch, and those scripts removed from the distributions. 

**Deprecated metrics dropped**  
Several deprecated ([SONAR-1794](https://jira.sonarsource.com/browse/SONAR-11794)) or obsolete ([SONAR-11664](https://jira.sonarsource.com/browse/SONAR-11664)) metrics have been dropped from the platform.

[Full Release Notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14848)

## Release 7.6 Upgrade Notes
**Quality Gates Simplified**  
Quality Gates have been streamlined to remove a number of confusing options. Conditions previously using the "on new code" checkbox will be migrated to On New Code metrics. For example, a condition previously using the overall Coverage metric with the "on new code" checkbox enabled will be migrated to a condition using the Coverage on New Code metric. The ability to set Warning conditions has been dropped, as have some metric/operator conditions have been removed. Conditions using dropped options will be removed in the upgrade. ([MMF-473](https://jira.sonarsource.com/browse/MMF-473))

**Concept of module removed from the UI**  
This version drops the concept of module from the interface. There is no longer a homepage presentation for any level below the project itself. Additionally, the presentation of the project has been updated in the Measures and Code pages to display the project tree as it is in the file system. For the most part (see below) analysis of multi-module projects will continue to work as it has.

**Multi-Module analysis properties removed**  
Multi-module analysis configuration may need to be changed ([MMF-365](https://jira.sonarsource.com/browse/MMF-365)):

* When exclusions based on file paths are specified in the analysis properties at project level, those file paths must be relative to the project / analysis root. Paths specified at project level will continue to be re-applied at module level but will raise a warning. This backward-compatibile behavior is considered deprecated and will be dropped in a future version. 
* Specifying source encoding, and issue inclusions / exclusions at module level is no longer supported.

**Incompatibility with Findbugs plugin version 3.9.1 and earlier**  
This version embeds SonarHTML, which analyzes both `.html` and `.jsp` files. Because of this change, the community plugin Findbugs versions 3.9.1 and earlier are incompatible with SonarQube 7.6+ ([MMF-1567](https://jira.sonarsource.com/browse/MMF-1567)).

[Full Release Notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14693)

## Release 7.5 Upgrade Notes  
**More Issues Backdated**  
Additional cases of issue backdating have been added, so fewer genuinely old issues will be reported in the New Code period ([MMF-1287](https://jira.sonarsource.com/browse/MMF-1287))

**Two Vulnerabilities Patched**  
An open redirect vulnerability on login was corrected ([SONAR-11475](https://jira.sonarsource.com/browse/SONAR-11475)).

An XSS vulnerability in custom project links was also patched. ([SONAR-11506](https://jira.sonarsource.com/browse/SONAR-11506)).

**Deadlock Fixed**  
The deadlock that could occur with the combination of 
* SQL Server
* Multiple workers
* Analysis of projects and portfolios  

has been fixed ([SONAR-11467](https://jira.sonarsource.com/browse/SONAR-11467)).

**DB Connection Pool Defaults Restored**  
Database connection pool defaults have been restored to their pre-SonarQube 7.4 values. They were inadvertently affected by a change of connection pooling in 7.4 ([SONAR-11539](https://jira.sonarsource.com/browse/SONAR-11539)). 

**Database Name in JDBC URL Now Case-Sensitive**
For MSSQL users, a driver upgrade rendered the database name case-sensitive in the JDBC URL ([SONAR-11443](https://jira.sonarsource.com/browse/SONAR-11443)).

[Full Release Notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14693)


## Release 7.4 Upgrade Notes
**Analysis Failure on Invalid New Code Period**  
Analysis will fail if the New Code Period (see below) is not set to one of:
* valid, in-the-past date
* positive integer (number of days)
* `previous_version`
* the version string of an existing snapshot  

For more, see [SONAR-10555](https://jira.sonarsource.com/browse/SONAR-10555)

**New Create Portfolios and Create Applications permissions**  
Two distinct new create permissions have been added. Users who have any creation permission will see a new "+" item in the top menu giving access to these permissions. For more, see the Global Permissions topic in [Security](/instance-administration/security/) 

**Issues from third party Roslyn analyzers**  
Analyzing a C# / VB.NET solution now automatically imports issues found by attached Roslyn analyzers into SonarQube, and no longer suppresses them from the MSBuild output. The Quality Gate status of projects may be impacted.

**More memory may be needed for analysis**  
Changes in the advanced security analysis available in Developer Edition and above may mean that a larger heap is needed during analysis.

**Analysis warnings in UI**  
Some `WARN` messages generated during analysis are now available via the UI ([MMF-1244](https://jira.sonarsource.com/browse/MMF-1244)). More messages will be available as new versions of language analyzers are released.

**"Leak" replaced with "New Code"**  
Wording has been updated throughout the interface to replace "Leak" and "Leak Period" with "New Code" and "New Code Period".

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14549)


## Release 7.3 Upgrade Notes

**New "Administer Security Hotspots" Permission**  
During the upgrade, the new "Administer Security Hotspots" permission is granted to all users/groups who already have the "Administer Issues" permission.

**Expanded Compute Engine Logs**  
Starting with this version, Compute Engine logs will be more verbose. These logs are rotated automatically, but on a daily basis, not based on file size. 

**PostgreSQL < 9.3 No Longer Supported**  
SonarQube 7.3+ only supports PostgreSQL 9.3 to 10. SonarQube will not start if you are using a lower version of PostgreSQL.

**Some 3rd-party Plugins Incompatible**  
APIs deprecated before SonarQube 5.6 are dropped in this version, making some third-party plugins incompatible. It is always advised to check plugin compatibility in the Plugin Version Matrix with each new upgrade, and more so for this version. 

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14464)

## Release 7.2 Upgrade Notes

**License Incompatibility**  
**Users coming from 6.7.5 must not upgrade to this version.** Your license will be incompatible. Instead, if you seek an upgrade to an intermediate version before the next L.T.S. version, you must start from 7.3 or higher.

**Pull Request Analysis**  
Pull Requests are now a first class citizen feature in SonarQube for Developer, Enterprise and Data Center Edition users.

If you are using GitHub, you need to be sure to NOT have the GitHub Plugin in your SONARQUBE_HOME/extensions/plugins directory.

**New Edition Packaging**  
SonarSource Commercial Editions are now distributed individually, so you directly get the features and functionalities that match your needs. This means that upgrade/downgrade from one edition to another is no longer possible within the SonarQube Marketplace. In order to use a different edition you must download its dedicated package, and have a license ready for using that edition.

**Deprecated Features**  
SonarQube 7.2 is the last version supporting PostgreSQL < 9.3. Starting from SonarQube 7.3 the minimal supported version of PostgreSQL will be 9.3: SONAR-10668

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14213)

## Release 7.1 Upgrade Notes

**License Incompatibility**  
**Users coming from 6.7.5 must not upgrade to this version.** Your license will be incompatible. Instead, if you seek an upgrade to an intermediate version before the next L.T.S. version, you must start from 7.3 or higher.

**Live Portfolios**  
Portfolio measures are now updated without having to explicitly trigger recalculation. As a result, the "views" scanner task no longer has any effect, and will fail with a clear error message. 

**Deprecated Features**  
Support for MySQL is deprecated for all editions below Data Center Edition (see below).

**Dropped Features**  
- Support for MySQL in Data Center Edition.
- The "accessors" metric, which was deprecated in SonarQube 5.0.

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14178)

## Release 7.0 Upgrade Notes

**License incompatibility**  
**Users coming from 6.7.5 must not upgrade to this version.** Your license will be incompatible. Instead, if you seek an upgrade to an intermediate version before the next L.T.S. version, you must start from 7.3 or higher.

**Measures: Live Update**  
Project measures, including the Quality Gate status, are computed without having to trigger another code scan when issue changes may impact them.

**Built-In Read-Only Quality Gate**  
In order to make clear the default, minimum and recommended criteria Quality Gates, the "Sonar way" Quality Gate is now read-only, and the default if one is not already set. It may be updated automatically at each upgrade of SonarQube.

**Dropped Features**  
It's no longer possible to unset the default Quality Gate. 

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14041)


## Release 6.7.5 Upgrade Notes

**Commercial Edition Must Be Upgraded**  
Because a new server identifier will be generated at upgrade to this version, startup will fail unless you upgrade your commercial edition to the latest compatible version. I.E. don't just copy over your edition plugins from one instance to the next, but make sure to download the latest edition bundle.

**SonarLint Must Be Upgraded**  
Analyzers provided as part of a commercial package will be disabled in old versions of SonarLint. SonarLint users must upgrade to the latest available version:

- SonarLint for Eclipse: 3.3+.
- SonarLint for IntelliJ: 3.1+

**Multi-Version Upgrade**  
Don't forget to read all the intermediate upgrade notes if you're upgrading more than a single version.

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=14467)


## Release 6.7 Upgrade Notes

**Drop of Issues Report**  
The deprecated Issues Report feature has been removed.

[Full release notes](https://jira.sonarsource.com/jira/secure/ReleaseNote.jspa?projectId=10930&version=13972)
