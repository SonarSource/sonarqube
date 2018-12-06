---
title: Pull Request Analysis
url: /analysis/pull-request/
---

<!-- sonarqube -->

_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)_

<!-- /sonarqube -->


Pull Request analysis allows you to:

* see your Pull Request (PR) analysis results in the {instance} UI and see the green or red status to highlight the existence of open issues.
* automatically decorate your PRs with {instance} issues in your SCM provider's interface. 

PRs are visible in {instance} from the "branches and pull requests" dropdown menu of your project.

When PR decoration is enabled, {instance} publishes the status of the analysis (Quality Gate) on the PR.

When "Confirm", "Resolved as False Positive" or "Won't Fix" actions are performed on issues in {instance} UI, the status of the PR is updated accordingly. This means, if you want to get a green status on the PR, you can either fix the issues for real or "Confirm", "Resolved as False Positive" or "Won't Fix" any remaining issues available on the PR.

PR analyses on {instance} are deleted automatically after 30 days with no analysis. This can be updated in **Configuration > General > Number of days before purging inactive short living branches**. 

<!-- sonarcloud -->
## Integrations for GitHub, Bitbucket Cloud and Azure DevOps
If your repositories are hosted on GitHub, Bitbucket Cloud or Azure DevOps, check out first the dedicated Integrations for: [BitBucketCloud](/integrations/bitbucketcloud/), [GitHub](/integrations/github/), and [Azure DevOps](/integrations/vsts/). Chances are that you do not need to read this page further since those integrations handle the configuration and analysis parameters for you.
<!-- /sonarcloud -->

## Analysis Parameters
### Pull Request Analysis in {instance}
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
<!-- sonarqube -->
The first thing to configure is the authentication token that will be used by {instance} to decorate the PRs. This can be configured in **Administration > General Settings > Pull Requests**. The field to configure depends on the provider.
For GitHub Enterprise or GitHub.com, you need to configure the **Authentication token** field. For Azure DevOps, it's the **Personal access token**.
<!-- /sonarqube -->
<!-- sonarcloud -->
If you are using Azure DevOps, the first thing to configure is the authentication token that will be used by {instance} to decorate the PRs. This can be configured in **Administration > General Settings > Pull Requests > VSTS > Personal access token**.
<!-- /sonarcloud -->

#### Pull Request Provider
| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.provider` | `github` or `vsts` <!-- sonarcloud -->or `bitbucketcloud`<!-- /sonarcloud -->. This is the name of the system managing your PR. In Azure DevOps, when the {instance} Extension for Azure DevOps is used, `sonar.pullrequest.provider` is automatically populated with "vsts". <!-- sonarcloud -->Same on GitHub if you are using the Travis CI Add-on, and on Bitbucket Cloud if you are building with Bitbucket Pipelines.<!-- /sonarcloud -->|

#### GitHub Parameters
| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.github.repository` | SLUG of the GitHub Repo |
<!-- sonarqube -->
| `sonar.pullrequest.github.endpoint` | The API url for your GitHub instance.<br/> Ex.: `https://api.github.com/` or `https://github.company.com/api/v3/` |
<!-- /sonarqube -->

Note: if you were relying on the GitHub Plugin, its properties are no longer required and they must be removed from your configuration: `sonar.analysis.mode`, `sonar.github.repository`, `sonar.github.pullRequest`, `sonar.github.oauth`.

<!-- sonarcloud -->
#### Bitbucket Cloud Parameters
| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.bitbucketcloud.repository` | SLUG or UUID of the Bitbucket Cloud Repo |
| `sonar.pullrequest.bitbucketcloud.owner` | SLUG or UUID of the Bitbucket Cloud Owner |
<!-- /sonarcloud -->

<!-- sonarqube -->
#### Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > General](/#sonarqube-admin#/admin/settings)**) must be set correctly. Otherwise the links will default to `localhost`.
<!-- /sonarqube -->
