---
title: Decorating Pull Requests
url: /analysis/pr-decoration/
---

_Pull Request decoration is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

You can add SonarQube analysis and a Quality Gate to your Pull Requests (PR) in your ALM provider's interface.

## Pull Request Decoration by provider

Click your ALM provider below to expand the instructions on decorating your Pull Requests.

[[collapse]]
| ## GitHub Enterprise and GitHub.com
|
|*For GitHub Enterprise, the minimum version is 2.14*
|
| ### Creating a GitHub App
|
| To add Pull Request decoration in GitHub checks, an instance administrator needs to create a GitHub App:
|
| 1. Follow Steps 1–4 [here](https://developer.github.com/apps/building-github-apps/creating-a-github-app/) to start creating your GitHub App.
| 1. Under **GitHub App name**, give your app a name (such as SonarQubePRChecks).
| 1. GitHub requires a **Homepage URL** and a **Webhook URL**. These values aren't important for Pull Request decoration, so you can use any URL (such as `https://www.sonarqube.org/`).
| 1. Grant access for the following **Permissions**
|
|	| Permission          | Access       |
|	|---------------------|--------------|
|	| Checks              | Read & write |
|	| **GitHub Enterprise:** Repository metadata <br/> **GitHub.com:** Metadata | Read-only    |
|	| Pull Requests       | Read-only    |
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
| From here, set your **Configuration Name**, **GitHub Instance URL**, **GitHub App ID**, and your GitHub App's **Private Key** (that was generated above in the **Creating a GitHub App** section).
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Project Settings > General Settings > Pull Request Decoration**, select your **Configuration Name**, and set your **Repository identifier**.

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
| From here, set your  **Configuration name**, **Bitbucket Server URL**, and the **Personal Access Token** of the account you're using to decorate your Pull Requests.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| #### **Projects created from a Bitbucket Server repository**
|
| If you create a project **From a Bitbucket Server repository**, SonarQube automatically configures your project settings for Pull Request decoration. You will need to provide a Personal Access Token from your user account with Read permissions for both projects and repositories. This Personal Access Token will be stored in SonarQube until you revoke it on the Bitbucket Server side.
|
| #### **Projects created manually**
|
| If you create a new project **Manually** or want to add Pull Request decoration to an existing project, you need to set your project settings at **Project Settings > General Settings > Pull Request Decoration**. Here you'll select your **Configuration name** and set your **Project Key** and **Repo Slug**.
|
| ### Blocking Pull Request merges
|*Blocking Pull Request merges is available starting with version 6.9 of Bitbucket Server
|
| After setting up Pull Request analysis, you can block Pull Requests from being merged if they don't meet your quality standards (if the Quality Gate is failing). To do this:
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
| If you have multiple Bitbucket Server configurations connected to SonarQube, you cannot use the automated create a new project **From a Bitbucket Server repository** option and will have to create projects manually.

## Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) must be set correctly. Otherwise, the links will default to `localhost`.
