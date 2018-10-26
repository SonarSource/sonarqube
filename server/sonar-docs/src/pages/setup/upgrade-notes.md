---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

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
