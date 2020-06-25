---
title: Decorating Pull Requests
url: /analysis/pr-decoration/
---

_Pull Request decoration is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

You can add SonarQube analysis and a Quality Gate to your Pull Requests in your ALM provider's interface.

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

## Pull Request Decoration by provider

Click your ALM provider below to expand the instructions on configuring SonarQube for Pull Request decoration.

[[collapse]]
| ## GitHub Enterprise and GitHub.com
|
|*Minimum GitHub Enterprise version is 2.14*
|
| ### Creating a GitHub App
|
| You can add Pull Request decoration under the GitHub **Checks** tab and/or **Conversation** tab. To do this, an instance administrator needs to first create a GitHub App:
|
| 1. Follow Steps 1–4 [here](https://developer.github.com/apps/building-github-apps/creating-a-github-app/) to start creating your GitHub App.
| 1. Under **GitHub App name**, give your app a name, such as SonarQubePRChecks.
| 1. Add a **Homepage URL**. GitHub requires this, but it isn't important for Pull Request decoration. You can use any URL, such as `https://www.sonarqube.org/`.
| 1. Enter your **User authorization callback URL**. Set this to your instance's base URL. For example, `yourinstance.sonarqube.com`.
| 1. Add a **Webhook URL**. Set this to your instance's base URL. For example, `yourinstance.sonarqube.com`.
| 1. Grant access for the following **Permissions**:
|
|	| Permission          | Access       |
|	|---------------------|--------------|
|	| Checks              | Read & write |
|	| **GitHub Enterprise:** Repository metadata <br/> **GitHub.com:** Metadata | Read-only    |
|	| Pull Requests       | Read & write |
|	| Commit statuses     | Read-only    |
|
| 1. Under "Where can this GitHub App be installed?," select **Any account**.  
| 2. Click **Create GitHub App**. This will take you to your new GitHub App's page.  
| 3. Scroll down to the bottom of your app page and click **Generate Private Key**. This downloads a `.pem` file that you'll use in the **Setting your global settings** section.  
|
| ### Installing your app 
|
| Install your GitHub App from the app's settings page. See the [GitHub instructions](https://developer.github.com/apps/installing-github-apps/) for more information.
|
| ### Setting your global settings
|
| To set your global settings in SonarQube, navigate to **Administration > Configuration > General Settings > ALM Integrations** and select the **GitHub** tab.
|
| From here, set your:
|
| - **Configuration Name** – If you're using Enterprise Edition or above, create a configuration name that is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration. If you're using Developer Edition, your configuration will be named based on your ALM.
| - **GitHub URL** – Add the GitHub URL. For example, `https://github.company.com/api/v3` for GitHub Enterprise or `https://api.github.com/` for GitHub.com.
| - **GitHub App ID** – The App ID is found on your GitHub App's page on GitHub at **Settings > Developer Settings > GitHub Apps**. 
| - **Client ID** – the Client ID is found below the GitHub App ID on your GitHub App's page.
| - **Client secret** – the Client secret is found below the Client ID on your GitHub App's page.
| - **Private Key** – Your GitHub App's private key from the `.pem` file that was generated above in the **Creating a GitHub App** section.
|
| ### Setting your project settings
|
| #### **Projects imported from a GitHub repository**
| If you create a project imported from a GitHub repository, SonarQube automatically configures your project settings for Pull Request decoration.
|
| #### **Manually created projects and existing projects**
| 
| If you're creating a new project **Manually** or want to add Pull Request decoration to an existing project, set your project settings at **Project Settings > General Settings > Pull Request Decoration**. 
|
| From here, set your: 
| - **Configuration name** – The configuration name that corresponds to your ALM instance. 
| - **Repository identifier** – The path of your repository URL.
|
| #### **Enable analysis summary under the GitHub Conversation tab**
| By default, **Enable analysis summary under the GitHub Conversation tab** is on and your Pull Request analysis will be shown under the **Conversation** and **Checks** tabs in GitHub. Turning this setting off will make it so Pull Request analysis is only shown under the **Checks** tab.

[[collapse]]
| ## Bitbucket Server
|
| *Minimum BitBucket Server version 5.15*
|
| A Bitbucket Server user account is used to decorate Pull Requests. We recommend using a dedicated Bitbucket Server account with Administrator permissions. You need a [Personal Access Token](https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html) from this account with **Write** permission for the repositories that will be analyzed.
|
| ### Setting your global settings
|
| To set your global settings in SonarQube, navigate to **Administration > Configuration > General Settings > ALM Integrations** and select the **Bitbucket Server** tab.
|
| From here, set your:
|- **Configuration Name** – If you're using Enterprise Edition or above, create a configuration name that is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration. If you're using Developer Edition, your configuration will be named based on your ALM.
| - **Bitbucket Server URL** 
| - **Personal Access Token** - Token of the account you're using to decorate your Pull Requests.
|
| ### Setting your project settings
|
| #### **Projects imported from a Bitbucket Server repository**
|
| If you create a project imported from a BitBucket server repository, SonarQube automatically configures your project settings for Pull Request decoration. When creating your project, you'll need to provide a Personal Access Token from your user account with Read permissions for both projects and repositories. This Personal Access Token will be stored in SonarQube until you revoke it on the Bitbucket Server side.
|
| #### **Manually created projects and existing projects**
|
| If you create a new project **Manually** or want to add Pull Request decoration to an existing project, you need to set your project settings at **Project Settings > General Settings > Pull Request Decoration**. 
|
| From here, set your:
| - **Configuration name** – The configuration name that corresponds to your ALM instance.
| - **Project Key** – Part of your BitBucket Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
| - **Repo Slug** – Part of your BitBucket Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).
|
| ### Blocking Pull Request merges
|*Blocking Pull Request merges is available starting with version 6.9 of Bitbucket Server*
|
| After setting up Pull Request analysis, you can block Pull Requests from being merged if they don't meet your quality standards and the Quality Gate is failing. To do this:
| 
| 1. From Bitbucket Server, navigate to **Repository settings > Code Insights**. 
| 1. Add a Required report called `com.sonarsource.sonarqube`, select **Must pass** as the Required status, and **Must not have any annotations** as the Annotation requirements.

[[collapse]]
| ## Azure DevOps Server
|
| An Azure DevOps Server user account is used to decorate Pull Requests. We recommend using a dedicated Azure DevOps Server account with Administrator permissions. You need a [Personal Access Token](https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=tfs-2017&tabs=preview-page) from this account with the scope authorized for **Code > Read & Write** for the repositories that will be analyzed.
|
| To add Pull Request decoration on Azure DevOps Server, you also need to update your global and project settings. 
|
| ### Setting your global settings
|
| To set your global settings in SonarQube, navigate to **Administration > Configuration > General Settings > ALM Integrations** and select the **Azure DevOps Server** tab.
|
| From here, set your **Configuration name** and the **Personal Access Token** of the account you're using to decorate your Pull Requests.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Project Settings > General Settings > Pull Request Decoration** and select your **Configuration name**.

[[collapse]]
| ## GitLab Self-Managed and GitLab.com
|
|*For GitLab Self-Managed, the minimum version is 11.7*
|
| A GitLab user account is used to decorate Merge Requests. We recommend using a dedicated GitLab account with at least **Reporter** [permissions](https://docs.gitlab.com/ee/user/permissions.html) (the account needs permission to leave comments). You need a Personal Access Token from this account with the scope authorized for **api** for the repositories that will be analyzed.
|
| To add Merge Request decoration to GitLab, you also need to update your global and project settings.
|
| ### Setting your global settings
|
| To set your global settings in SonarQube, navigate to **Administration > Configuration > General Settings > ALM Integrations** and select the **GitLab** tab.  
|
| From here, set your **Configuration name** and the **Personal Access Token** of the account you're using to decorate your Merge Requests. If you're using [GitLab CI](/analysis/gitlab-cicd/) to scan your project, you can leave the **GitLab URL** blank, as it will be auto-detected. If you're using a different CI tool (e.g.: Jenkins, Travis CI, etc), or run the analysis manually, provide the base URL for GitLab's API.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Project Settings > General Settings > Pull Request Decoration** and select your **Configuration name**. If you're using [GitLab CI](/analysis/gitlab-cicd/), you can leave **Project ID** blank, as it will be auto-detected. If you're using a different CI tool (e.g.: Jenkins, Travis CI, etc), or run the analysis manually, provide the project's numerical ID.

## Multiple ALM instances

It's possible to decorate Pull Requests from multiple ALM instances. To do this, you can create a configuration (as shown in the previous section) for each of your ALM instances. That instance configuration can then be assigned to the appropriate projects.

As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each ALM. 

As part of [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/), you can create multiple configurations for each ALM.
[[warning]]
| If you have multiple Bitbucket Server or GitHub Enterprise configurations connected to SonarQube, you cannot use the automated create a new project **From a Bitbucket Server/GitHub Enterprise repository** option and will have to create projects manually.

## Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) must be set correctly. Otherwise, the links will default to `localhost`.
