---
title: Pull Request analysis
---

<!-- sonarqube -->

_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)_

<!-- /sonarqube -->


Pull Request analysis allows you to:

* see your Pull Request (PR) analysis results in the SonarQube UI and see the green or red status to highlight the existence of open issues.
* automatically decorate your PRs with SonarQube issues in your SCM provider's interface. 

PRs are visible in SonarQube from the "branches and pull requests" dropdown menu of your project.

When PR decoration is enabled, SonarQube publishes the status of the analysis (Quality Gate) on the PR.

When "Confirm", "Resolved as False Positive" or "Won't Fix" actions are performed on issues in SonarQube UI, the status of the PR is updated accordingly. This means, if you want to get a green status on the PR, you can either fix the issues for real or "Confirm", "Resolved as False Positive" or "Won't Fix" any remaining issues available on the PR.

PR analyses on SonarQube are deleted automatically after 30 days with no analysis. This can be updated in **Configuration > General > Number of days before purging inactive short living branches**. 

## Analysis Parameters
### Pull Request Analysis in SonarQube
These parameters enable PR analysis:

| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.branch` | The name of your PR<br/> Ex: `sonar.pullrequest.branch=feature/my-new-feature`|
| `sonar.pullrequest.key` | Unique identifier of your PR. Must correspond to the key of the PR in GitHub or TFS. <br/> E.G.: `sonar.pullrequest.key=5` |
| `sonar.pullrequest.base` | The long-lived branch into which the PR will be merged. <br/> Default: master <br/> E.G.: `sonar.pullrequest.base=master`|

### Pull Request Decoration
To activate PR decoration, you need to:

* declare an Authentication Token
* specify the Git provider
* feed some specific parameters (GitHub only)

#### Authentication Token
The first thing to configure is the authentication token that will be used by SonarQube to decorate the PRs. This can be configured in **Administration > Pull Requests**. The field to configure depends on the provider.

For GitHub Enterprise or GitHub.com, you need to configure the **Authentication token** field. For VSTS/TFS, it's the **Personal access token**.

#### Pull Request Provider
| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.provider` | `github` or `vsts`<br/> This is the name of the system managing your PR. In VSTS/TFS, when the Analyzing with SonarQube Extension for VSTS-TFS is used, `sonar.pullrequest.provider` is automatically populated with "vsts". |

#### GitHub Parameters
| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.github.repository` | SLUG of the GitHub Repo |
| `sonar.pullrequest.github.endpoint` | The API url for your GitHub instance.<br/> Ex.: `https://api.github.com/` or `https://github.company.com/api/v3/` |

Note: if you were relying on the GitHub Plugin, its properties are no longer required and they must be removed from your configuration: `sonar.analysis.mode`, `sonar.github.repository`, `sonar.github.pullRequest`, `sonar.github.oauth`.

<!-- sonarcloud -->
## TravisCI + GitHub.com + SonarCloud
All the analysis parameters are automatically populated if you are relying on the SonarCloud add-on. See https://blog.sonarsource.com/sonarcloud-loves-your-build-pipeline for more details.
<!-- /sonarcloud -->

