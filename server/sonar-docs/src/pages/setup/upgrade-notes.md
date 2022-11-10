---
title: Release Upgrade Notes
url: /setup/upgrade-notes/
---

## Release 9.8 Upgrade notes
**Analysis discards from its scope all files greater than 20 MB**
SonarQube analysis discards from its scope all files greater than 20 MB. This setting can be changed at scanner level using following property `sonar.filesize.limit`. ([SONAR-11096](https://sonarsource.atlassian.net/browse/SONAR-11096)) 

**Dropping the embedded documentation**
SonarQube no longer ships with an embedded version of the documentation. The documentation will now only be available at [docs.sonarqube.org](https://docs.sonarqube.org/). ([SONAR-17221](https://sonarsource.atlassian.net/browse/SONAR-17221))  
* All in-app links will now point to the documentation website.
* External links that pointed to `/documentation` will be correctly redirected to the corresponding page on the documentation website.

**Update in the Database support**
* PostgreSQL versions <11 are no longer supported.
* Adding support to the latest version 15 of PostgreSQL. Supported versions are now from 11 to 15.

[Full release notes](https://sonarsource.atlassian.net/issues/?jql=project%20%3D%2010139%20AND%20fixVersion%20%3D%2013884)

## Release 9.7 Upgrade notes
**Change in the database connection pool**  
The database connection pool has been replaced for better performance. The `sonar.jdbc.maxIdle`, `sonar.jdbc.minEvictableIdleTimeMillis` and `sonar.jdbc.timeBetweenEvictionRunsMillis` properties no longer have any effect and should be removed from the configuration. Also, the JMX information that is provided to monitor the connection pool has evolved. See the [Monitoring documentation](/instance-administration/monitoring/) for more information. ([SONAR-17200](https://sonarsource.atlassian.net/browse/SONAR-17200)).

**JavaScript, TypeScript, and CSS analysis now requires Node.js 14.17+**  
In order to analyze Javascript, Typescript, and CSS code, Node.js 14.17+ must be installed on the machine running the scan.
We recommend that you use the latest Node.js LTS, which is currently Node.js 16.

[Full release notes](https://sonarsource.atlassian.net/issues/?jql=project%20%3D%2010139%20AND%20fixVersion%20%3D%2013800)

## Release 9.6 Upgrade notes
**Microsoft SQL Server changes in configuration and Integrated Authentication**  
* If your Microsoft SQL Server doesn't support encryption, you will need to add `encrypt=false` to the JDBC URL connection string. ([SONAR-16249](https://jira.sonarsource.com/browse/SONAR-16249)).
* If your Microsoft SQL Server requires encryption but you don't want SonarQube to validate the certificate, you will need to add `trustServerCertificate=true` to the JDBC URL connection string.
* If you are using Microsoft SQL Server with Integrated Authentication, you will need to replace the `mssql-jdbc_auth` dll file on your `PATH` with `mssql-jdbc_auth-10.2.1.x64.dll` from the  [Microsoft SQL JDBC Auth 10.2.1 package](https://github.com/microsoft/mssql-jdbc/releases/tag/v10.2.1). See [Install the Server](/setup/install-server/) for more information.

**Token expiry**  
New tokens can now have an optional expiration date. Expired tokens cannot be used and must be updated. With [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://redirect.sonarsource.com/editions/editions.html), system administrators can set a maximum lifetime for new tokens. See [Security](/instance-administration/security/) documentation for more information. ([SONAR-16565](https://sonarsource.atlassian.net/browse/SONAR-16565), [SONAR-16566](https://sonarsource.atlassian.net/browse/SONAR-16566)).

**Running SonarQube as a Service and Java version selection**
* To install, uninstall, start or stop SonarQube as a service on Windows, now you should use `%SONAR_HOME%\bin\windows-x86-64\SonarService.bat install`. See [Operating the Server](/setup/operate-server/) and [Upgrade Guide](/setup/upgrading/) for more information.
* If there are multiple versions of Java installed on your server, to select specific Java version to be used, set the environment variable `SONAR_JAVA_PATH`. Read more [here](/setup/install-server/).

[Full release notes](https://sonarsource.atlassian.net/issues/?jql=project%20%3D%2010139%20AND%20fixVersion%20%3D%2012633)

## Release 9.5 Upgrade notes
**Project analysis token**  
You can now generate tokens of different types and can create a different analysis token for every specific project. The new tokens will include a prefix to help you quickly identify SonarQube tokens and their type. The usage of project analysis tokens is encouraged to limit the access this token has. See [Generating and Using Tokens](/user-guide/user-token/) documentation for more information.
([SONAR-16260](https://jira.sonarsource.com/browse/SONAR-16260)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=17328)

## Release 9.4 Upgrade notes
**Password of old inactive account needs reset**  
The support for SHA1 hashed password has been removed. This algorithm was replaced by a stronger hashing algorithm since version 7.2. As a result, local accounts that did not log in since 7.2 will be forced to have their password reset by a SonarQube administrator. Accounts using external authentication such as SAML, LDAP, GitHub authentication, etc., are not impacted. Information about the possibly impacted accounts will appear in the logs during the upgrade. ([SONAR-16204](https://jira.sonarsource.com/browse/SONAR-16204)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=17167)

## Release 9.3 Upgrade Notes  
**Portfolio overview now shows ratings on both New Code and Overall Code**  
The Portfolio overview and project breakdown have been redesigned to provide a high-level view on project health according to your New Code definition as well as Overall Code. New Code ratings are shown for Reliability, Security Vulnerabilities, Security Review, and Maintainability. To see these ratings on New Code, Portfolios need to be recomputed after upgrading to 9.3.

Along with this redesign, Portfolios and Applications no longer show users information on projects they don't have access to, and Application administration has been moved out of the Portfolio administration UI.

**Microsoft SQL Server and Integrated Authentication**  
If you are using Microsoft SQL Server with Integrated Authentication, you will need to replace the `mssql-jdbc_auth-9.2.0.x64.dll` file on your `PATH` with `mssql-jdbc_auth-9.4.1.x64.dll` from the [Microsoft SQL JDBC Driver 9.4.1 package](https://docs.microsoft.com/en-us/sql/connect/jdbc/release-notes-for-the-jdbc-driver?view=sql-server-ver15#94). See [Install the Server](/setup/install-server/) for more information.

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=17060)

## Release 9.2 Upgrade Notes
**Bitbucket Cloud authentication now built-in**  
Support for Bitbucket Cloud authentication is now built-in. If you were using the Bitbucket Cloud authentication plugin before, you need to remove it from SonarQube before upgrading.

SonarQube uses the same settings as the plugin, so you do not need to update them. The Teams restriction has been replaced with the Workspaces restriction and is migrated accordingly. 

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16959)

## Release 9.1 Upgrade Notes  
**Secured settings no longer available in web services and on the scanner side**  
This change especially affects the analysis of SVN projects but also, possibly, the use of some 3rd-party plugins. Secured settings required to perform the analysis now need to be passed to the scanner as parameters. 

**Custom measures feature has been dropped**  
The custom measures feature, which was previously deprecated, has been removed. ([SONAR-10762](https://jira.sonarsource.com/browse/SONAR-10762)).

**Deprecated WebAPI endpoints and parameters removed**  
The WebAPI endpoints and parameters deprecated during the 7.X release cycle have been removed. For a complete list of removed endpoints and parameters see [SONAR-15313](https://jira.sonarsource.com/browse/SONAR-15313).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16792)

## Release 9.0 Upgrade Notes  
**Scanners require Java 11**  
Java 11 is required for SonarQube scanners. Use of Java 8 is no longer supported. See the documentation on [Scanner Environment](/analysis/scanner-environment/) for more information. 

**Support for Internet Explorer 11 dropped**  
Support for Internet Explorer 11 and other legacy browsers has been dropped. ([SONAR-14387](https://jira.sonarsource.com/browse/SONAR-14387)).

**Reporting Quality Gate status on GitHub branches requires an additional permission**  
When working in private GitHub repositories, you need to grant read-only access to the **Contents** permission on the GitHub application that you're using for SonarQube integration. See the [GitHub integration documentation](/analysis/github-integration/) for more information.

**JavaScript custom rule API removed**  
The JavaScript custom rule API, which was previously deprecated, has been removed. Plugins can no longer use this API to implement custom rules. See the [JavaScript documentation](/analysis/languages/javascript/) for more information. ([SONAR-14928](https://jira.sonarsource.com/browse/SONAR-14928)).

**Deprecated Plugin Java API dropped**  
Parts of the Java API for plugins that were deprecated before SonarQube 7.0 have been dropped. You should compile plugins against SonarQube 9.0 to ensure they're compatible and to check if they're using a deprecated API that has been dropped. ([SONAR-14925](https://jira.sonarsource.com/browse/SONAR-14925), [SONAR-14885](https://jira.sonarsource.com/browse/SONAR-14885)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=15682)

## Release 8.9 LTS Upgrade Notes  
Upgrading directly from SonarQube _v7.9 LTS to v8.9 LTS_? Refer to the [LTS to LTS Release Upgrade Notes](/setup/lts-to-lts-upgrade-notes/).

**GitHub Enterprise compatibility**  
SonarQube 8.9 only supports GitHub Enterprise 2.21+ for pull request decoration (the previous minimum version was 2.15).

**Plugins require risk consent**  
When upgrading, if you're using plugins, a SonarQube administrator needs to acknowledge the risk involved with plugin installation when prompted in SonarQube. 

**Database support updated**  
SonarQube 8.9 supports the following database versions:

* PostgreSQL versions 9.6 to 13. PostgreSQL versions <9.6 are no longer supported.
* MSSQL Server 2014, 2016, 2017, and 2019.
* Oracle XE, 12C, 18C, and 19C. Oracle 11G is no longer supported.

**Webhooks aren't allowed to target the instance**  
To improve security, webhooks, by default, aren't allowed to point to the SonarQube server. You can change this behavior in the configuration. ([SONAR-14682](https://jira.sonarsource.com/browse/SONAR-14682)).

[Full release notes](https://jira.sonarsource.com/secure/ReleaseNote.jspa?projectId=10930&version=16710)
