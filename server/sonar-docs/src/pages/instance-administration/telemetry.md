---
title: Telemetry
url: /instance-administration/telemetry/
---

SonarQube periodically sends telemetry data to SonarSource.
This data helps us understand how SonarQube is used, which helps us improve our products.


## No personally identifiable information is sent

The telemetry doesn't collect personal data, such as usernames or email addresses.
It doesn't send source code or data such as project name, repository, or author.
No IP addresses are sent.
The data is sent securely, held under restricted access, and not published outside of SonarSource.

Protecting your privacy is important to us.
If you suspect the telemetry is collecting sensitive data or the data is being insecurely or inappropriately handled, please send an email to `security@sonarsource.com` so that we can investigate.


## Turning it off

You can disable telemetry at any time by setting the `sonar.telemetry.enabled` property to `false` in `$SONARQUBE-HOME/conf/sonar.properties`.
By default, it is set to `true`.


## What information is sent?

Once a day (every 24 hours), SonarQube sends a `JSON` payload to `https://telemetry.sonarsource.com/sonarqube`.

The data that is sent includes:

* Information about the SonarQube instances itself (version, edition, database type, etc.)
* The list of projects on the instance including, for each:
  * A traceable unique project identifier (but one which doesn’t reveal any identifying information about the project)
  * Information about the project like language, last analysis time, number of lines of code, etc.
* The list of users on that instance including, for each:
  * A traceable unique user identifier (but one which doesn’t reveal any identifiying information about the user)
  * Information about the user's usage of the system like time of last login and current status.

Here is an example of a telemetry payload:

```
{
  "id": "ID",
  "version": "9.6",
  "edition": "enterprise",
  "licenseType": "TEST-LICENCE",
  "database": {
    "name": "oracle",
    "version": "2.0.0"
  },
  "plugins": [
    {
      "name": "plugin-name",
      "version": "9.6"
    }
  ],
  "externalAuthProviders": [
    "sonarqube"
  ],
  "installationDate": 1661933380862,
  "installationVersion": "9.6",
  "docker": false,
  "users": [
    {
      "userUuid": "UUID-1",
      "status": "active",
      "lastActivity": "2022-01-01T08:00:00+00"
    }
  ],
  "projects": [
    {
      "projectUuid": "UUID-1",
      "lastAnalysis": "2022-01-01T08:00:00+00"
      "language": "java",
      "loc": 40
    },
    {
      "projectUuid": "UUID-2",
      "lastAnalysis": "2022-01-01T08:00:00+00"
      "language": "ts",
      "loc": 24
    },
    {
      "projectUuid": "UUID-3",
      "lastAnalysis": "2022-08-31T10:09:56+0200",
      "language": "css",
      "loc": 7
    }
  ],
  "projects-general-stats": [
    {
      "projectUuid": "UUID-1",
      "branchCount": 2,
      "pullRequestCount": 0,
      "scm": "undetected",
      "ci": "cirrus_ci",
      "alm": "gitlab_cloud"
    },
    {
      "projectUuid":"UUID-2",
      "branchCount": 1,
      "pullRequestCount": 0,
      "scm": "undetected",
      "ci": "undetected",
      "alm": "github_cloud"
    },
    {
      "projectUuid": "UUID-3",
      "branchCount": 1,
      "pullRequestCount": 0,
      "scm": "undetected",
      "ci": "undetected",
      "alm": "undetected"
    }
  ]
}
```
