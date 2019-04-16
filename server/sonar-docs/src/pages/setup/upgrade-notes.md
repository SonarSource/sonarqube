---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

## Release 7.8 Upgrade Notes
**Google Analytics Support**
Support for Google Analytics is now available via property in `sonar.properties`. ([SONAR-11793](https://jira.sonarsource.com/browse/SONAR-11793))

**Additional authentication methods embedded**
The SAML and GitHub Authentication plugins are now embedded in all versions ([SONAR-11894](https://jira.sonarsource.com/browse/SONAR-11894))

**Scanner version compatibility**
Only the following scanner versions are compatible with SonarQube 7.8:
* SonarQube Scanner CLI 2.9+
* SonarQube Scanner Maven 3.3.0.603+
* SonarQube Scanner Gradle 2.3+

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

* When the following inclusion / exclusion types are specified in the analysis properties at project level, they must be relative to the project / analysis root: source files, test files, coverage, and duplications. Paths specified at project level will continue to be re-applied at module level but will raise a warning. This backward-compatibile behavior is considered deprecated and will be dropped in a future version. 
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
