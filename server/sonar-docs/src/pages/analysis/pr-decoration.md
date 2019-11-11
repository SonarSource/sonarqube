---
title: Decorating Pull Requests
url: /analysis/pr-decoration/
---

_Pull Request decoration is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

You can add SonarQube analysis and a Quality Gate to your Pull Requests (PR) in your ALM provider's interface.

## Pull Request Decoration by provider

Click your ALM provider below to expand the instructions on decorating your Pull Requests.

[[collapse]]
| ## GitHub Enterprise  and GitHub.com
|
|*For GitHub Enterprise, the minimum version is Version 2.14*
|
| ### Creating a GitHub App
|
| To enable PR decoration in GitHub checks, an instance administrator needs to create a GitHub App:
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
| 1. Click **Create GitHub App**. This will take you to your new GitHub App's page.
| 1. Scroll down to the bottom of your app page and click **Generate Private Key**. This downloads a `.pem` file that you'll use in the **Setting your global settings** section.  
|
| ### Installing your app 
|
| Install your GitHub App from the app's settings page. See the [GitHub instructions](https://developer.github.com/apps/installing-github-apps/) for more information.
|
| ### Setting your global settings
|
| To set your global settings in SonarQube:
|
| 1. Navigate to **Administration > Configuration > General Settings > Pull Request Decoration**.
| 1. Select the **GitHub** tab.
| 1. Click the **Create configuration** button.
|
| From here, set your **Configuration Name**, **GitHub Instance URL**, **GitHub App ID**, and your GitHub App's **Private Key** (that was generated above in the **Creating a GitHub App** section).
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request Decoration**, select your **Configuration Name**, and set your **Repository identifier**.

[[collapse]]
| ## Bitbucket Server
|
| *Minimum BitBucket Server version 5.15*
|
| To add PR decoration on Bitbucket Server, you need to update your global and project settings. 
|
| ### Setting your global settings
|
| A Bitbucket Server user account is used to decorate Pull Requests. You need a [Personal Access Token](https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html) from this account with **Write** permission.
| 
| To set your global settings in SonarQube:  
| 1. Navigate to **Administration > Configuration > General Settings > Pull Request Decoration**.
| 1. Select the **Bitbucket Server** tab.
| 1. Click the **Create configuration** button. 
|
| From here, set your  **Configuration name**, **Bitbucket Server URL**, and the **Personal Access Token** of the account you're using to decorate your Pull Requests.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request Decoration**, select your **Configuration name**, and set your **Project Key** and **Repo Slug**.

[[collapse]]
| ## Azure DevOps Server
|
|To add PR decoration on Azure DevOps Server, you need to update your global and project settings. 
|
| ### Setting your global settings
|
| An Azure DevOps Server user account is used to decorate Pull Requests. You need a [Personal Access Token](https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=tfs-2017&tabs=preview-page) from this account with the scope authorized for **Code > Read & Write**.
|
| To set your global settings in SonarQube:
| 1. Navigate to **Administration > Configuration > General Settings > Pull Request Decoration**.
| 1. Select the **Azure DevOps Server** tab.
| 1. Click the **Create configuration** button.
|
| From here, set your **Configuration name** and the **Personal Access Token** of the account you're using to decorate your Pull Requests.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request Decoration** and select your **Configuration name**.

## Multiple ALM instances
SonarQube lets you decorate Pull Requests from multiple ALM instances. To do this, you can create a configuration (as shown in the previous section) for each of your ALM instances. That instance configuration can then be assigned to the appropriate projects. 

## Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) must be set correctly. Otherwise the links will default to `localhost`.
