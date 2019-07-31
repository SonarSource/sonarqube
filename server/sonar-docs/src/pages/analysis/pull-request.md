---
title: Pull Request Analysis
url: /analysis/pull-request/
---



_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._


Pull Requests (PRs) are visible in {instance} from the branches and pull requests dropdown menu of your project.

PR analysis allows you to:

* see your PR's Quality Gate status in the {instance} UI.
* automatically decorate your PRs with {instance} issues in your SCM provider's interface.

## Quality Gate

The PR quality gate:
* **Focuses on new code** – The PR quality gate only uses your project's quality gate conditions that apply to "on New Code" metrics.
* **Assigns a status** – Each PR shows a quality gate status reflecting whether it Passed (green) or Failed (red).

When PR decoration is enabled, {instance} publishes the status of the analysis (Quality Gate) on the PR.

PR analyses on {instance} are deleted automatically after 30 days with no analysis. This can be updated in **Configuration > General > Number of days before purging inactive short living branches**. 

## Analysis Parameters

These parameters enable PR analysis:

| Parameter Name        | Description |
| --------------------- | ------------------ |
| `sonar.pullrequest.key` | Unique identifier of your PR. Must correspond to the key of the PR in GitHub or Azure DevOps. <br/> E.G.: `sonar.pullrequest.key=5` |
| `sonar.pullrequest.branch` | The name of the branch that contains the changes to be merged.<br/> Ex: `sonar.pullrequest.branch=feature/my-new-feature`|
| `sonar.pullrequest.base` | The long-lived branch into which the PR will be merged. <br/> Default: master <br/> E.G.: `sonar.pullrequest.base=master`|

## PR Decoration
This section details how to decorate your PRs with {instance} issues in your SCM provider's interface.

### Specifying Your PR Provider

Specify your PR provider in your global settings at [**Administration > General Settings > Pull Requests > General > Provider**](/#sonarqube-admin#/sonarqube/admin/settings?category=pull_request/). This is the name of the system managing your PR. When using the {instance} Extension for Azure DevOps, the provider is automatically populated.

### GitHub Enterprise PR Decoration

[[info]]
| *Minimum GitHub Enterprise version* 2.14

To add PR decoration to Checks on GitHub Enterprise, you need to create a GitHub App and configure your SonarQube instance and update your project-level settings.

#### Creating Your GitHub App
An instance administrator needs to create a GitHub App and configure your SonarQube instance. See [GitHub Enterprise Integration](/instance-administration/github-application/) for instructions.

#### Updating Your GitHub Project Settings
In your project settings, set your project repository identifier (for example, SonarSource/sonarqube) at **Administration > General Settings > Pull Requests > Integration with GitHub > Repository identifier**.

### Bitbucket Server PR Decoration

[[info]]
| *Minimum BitBucket Server version* 5.15

To add PR decoration on Bitbucket Server, you need to set a personal access token and update some settings. 

#### Setting Your Personal Access Token

In your global settings, set the personal access token of the user that will be used to decorate the PRs in the SonarQube UI:

* Set the token at [**Administration > General Settings > Pull Requests > Integration with Bitbucket Server > Personal access token**](/#sonarqube-admin#/admin/settings?category=pull_request/)
* The user that will be used to decorate PRs needs write permission.

#### Updating Your Bitbucket Server Settings

In your global settings, set your Bitbucket Server URL (for example, `https://myinstance.mycompany.com/`) at [**Administration > General Settings > Pull Requests > Integration with Bitbucket Server > The URL of the Bitbucket Server**](/#sonarqube-admin#/admin/settings?category=pull_request/). This is the base URL for your Bitbucket Server instance.

In your project settings at **Administration > General Settings > Pull Requests > Integration with Bitbucket Server** update the following Bitbucket Server settings:

* Bitbucket Server project key. You can find it in the Bitbucket Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
For projects in a personal space, the project key is "~" followed by your username (for example,  `~YourUsername`).
* Bitbucket Server repository slug. You can find it in the Bitbucket Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).

### Azure DevOps Server PR Decoration

To add PR decoration on Azure DevOps Server, you need to set a personal access token.

#### Setting Your Personal Access Token

In global and project settings, set the personal access token of the user that will be used to decorate the PRs in the SonarQube UI at [**Administration > General Settings > Pull Requests > Integration with Azure DevOps > Personal access token**](/#sonarqube-admin#/admin/settings?category=pull_request/). 

The user that will be used to decorate PRs needs to be authorized for the scope: 'Code (read and write)'.	

### Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > General](/#sonarqube-admin#/admin/settings)**) must be set correctly. Otherwise the links will default to `localhost`.
