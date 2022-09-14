---
title: Telemetry
url: /instance-administration/telemetry/
---

SonarQube sends anonymized telemetry data to SonarSource daily.
This data helps us understand how SonarQube is used, which helps us improve our products.


## No personally identifiable information is sent

The telemetry doesn't collect personal data, such as usernames or email addresses.
It doesn't send source code or data such as project name, repository, or author.
No IP addresses are sent.
The data is sent securely, held under restricted access, and not published outside of SonarSource.

Protecting your privacy is important to us.
If you have any concerns about telemetry collection, please email us at `security@sonarsource.com`.


## Turning it off

You can disable telemetry at any time by setting the `sonar.telemetry.enabled` property to `false` in `$SONARQUBE_HOME/conf/sonar.properties`.
By default, it is set to `true`.


## What information is sent?

Once a day (every 24 hours), SonarQube sends a `JSON` payload to `https://telemetry.sonarsource.com/sonarqube`.

The data that is sent consists of:

* Anonymized information about the SonarQube instance (version, license type, edition, database type, etc.)
* Anonymized information about each project on the instance, consisting of:
  * A technical identifier that does not reveal any project-specific details.
  * Information about the project like language, last analysis time, number of lines of code, etc.
* Anonymized information about each user on the instance, consisting of:
  * A technical identifier that does not reveal any personal information about the user.
  * Information about the user's usage of the instance like last activity time and current status.

Here is an example of a telemetry payload:

```
{
    "id": "ABB010CE-AVcdRncGX_RgEGt_NVoS",
    "version": "9.7.0.59880",
    "edition": "datacenter",
    "licenseType": "PRODUCTION",
    "database": {
        "name": "PostgreSQL",
        "version": "12.8"
    },
    "plugins": [{
            "name": "iac",
            "version": "1.10.0.2310"
        },
        {
            "name": "plsql",
            "version": "3.7.0.4372"
        },
    ],
    "externalAuthProviders": [
        "github"
    ],
    "installationDate": "2022-02-01T09:12:32+0000",
    "docker": true,
    "users": [{
            "userUuid": "UI9126NM8DFghgCCDUI9",
            "status": "active",
            "lastActivity": "2022-03-22T13:18:56+0000"
        },
        {
            "userUuid": "YY456Uio878YHOJOM891",
            "status": "active",
            "lastActivity": "2022-09-06T14:08:46+0000"
        },
        {
            "userUuid": "G5GH76gb65F69Jygf789",
            "status": "active",
            "lastActivity": "2022-09-07T00:28:14+0000",
            "lastSonarlintActivity": "2022-09-07T00:28:14+0000"
        },
        {
            "userUuid": "AG7HK457TYITdsYIH67Y",
            "status": "inactive"
        }
    ],
    "projects": [
        {
            "projectUuid": "AV8WJCz7leTHsONfkGE1",
            "lastAnalysis": "2022-04-14T07:39:45+0000",
            "language": "css",
            "loc": 9722
        },
        {
            "projectUuid": "AV8WJCz7leTHsONfkGE1",
            "lastAnalysis": "2022-04-14T07:39:45+0000",
            "language": "js",
            "loc": 251210
        },
        {
            "projectUuid": "AWHotC4Cb9YxAwKuZDEk",
            "lastAnalysis": "2022-09-05T15:04:31+0000",
            "language": "java",
            "loc": 462
        },
        {
            "projectUuid": "AYAYr6o1Mi128diYBjFX",
            "lastAnalysis": "2022-09-05T15:04:31+0000",
            "language": "ts",
            "loc": 5835
        }
    ],
    "projects-general-stats": [
        {
            "projectUuid": "AV8WJCz7leTHsONfkGE1",
            "branchCount": 1,
            "pullRequestCount": 0,
            "scm": "git",
            "ci": "GitLab CI",
            "alm": "gitlab_cloud"
        },
        {
            "projectUuid": "AWHotC4Cb9YxAwKuZDEk",
            "branchCount": 1,
            "pullRequestCount": 8,
            "scm": "git",
            "ci": "Azure DevOps",
            "alm": "azure_devops_cloud"
        },
        {
            "projectUuid": "AYAYr6o1Mi128diYBjFX",
            "branchCount": 1,
            "pullRequestCount": 0,
            "scm": "git",
            "ci": "Github Actions",
            "alm": "github_cloud"
        },
    ],
    "timestamp": "2022-09-07T01:15:23.901Z",
    "type": "ping"
}
```
