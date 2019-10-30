---
title: Decorating Pull Requests
url: /analysis/pr-decoration/
---

_Pull Request decoration is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

You can add SonarQube analysis and a Quality Gate to your Pull Requests (PR) directly in your ALM provider's interface.

## Pull Request Decoration by provider

Click your ALM provider below to expand the instructions on decorating your Pull Requests.

[[collapse]]
| ## GitHub Enterprise  
|
|*Minimum GitHub Enterprise Version 2.14*
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
|	| Repository metadata | Read-only    |
|	| Pull Requests       | Read-only    |
|	| Commit statuses     | Read-only    |
|
| 1. Under "Where can this GitHub App be installed?," select **Any account**.
| 1. Click **Create GitHub App**. This will take you to your new GitHub App's page.
| 1. Scroll down to the bottom of your app page and click **Generate Private Key**. This downloads a `.pem` file that you'll use in the **Setting your global settings** section.  
|
| ### Installing your app
| To install your app in your GitHub organizations:
|
| 1. Go to your GitHub App URL. GitHub App URLs are formatted as: `https://<your-github-enterprise-address>/github-apps/<YourAppName>`.  
|	For example, if your GitHub Enterprise address is `github-enterprise-1.yoursite.com` and your app name is `SonarQubePRChecks`, your GitHub App URL will be `https://github-enterprise-1.yoursite.com/github-apps/SonarQubePRChecks`.
| 2. From your GitHub App page, click the **Install** or **Configure** button.
| 3. Choose the organization where you want to install your app from the list.
| 4. Click the **Install** button. 
|
| ### Setting your global settings
|
| Go to **[Administration > Configuration > General Settings > Pull Requests](/#sonarqube-admin#/admin/settings?category=pull_request_decoration/)**, select the **GitHub Enterprise** tab, and click the **Create configuration** button to set your Configuration Name, ALM Instance URL, GitHub App ID, and your GitHub App's Private Key (that was generated above in the **Creating a GitHub App** section).
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request decoration**, select your Configuration Name (created in the previous section), then set your Repository identifier.

[[collapse]]
| ## Bitbucket Server
|
| *Minimum BitBucket Server version 5.15*
|
| To add PR decoration on Bitbucket Server, you need to update your global and project settings. 
|
| ### Setting your global settings
|
| Go to **[Administration > Configuration > General Settings > Pull Requests](/#sonarqube-admin#/admin/settings?category=pull_request_decoration/)**, select the **Bitbucket Server** tab, and click the **Create configuration** button to set your  Configuration name, Bitbucket Server URL, and Personal Access token.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request decoration**, select your Configuration name, then set your Project Key and Repo Slug.

[[collapse]]
| ## Azure DevOps Server
|
|To add PR decoration on Azure DevOps Server, you need to update your global and project settings. 
|
| ### Setting your global settings
|
| Go to **[Administration > Configuration > General Settings > Pull Requests](/#sonarqube-admin#/admin/settings?category=pull_request_decoration/)**, select the **Azure DevOps Server** tab, and click the **Create configuration** button to set your  Configuration name and Personal Access token.
|
| **Note:** Make sure the Configuration name is succinct and easily recognizable as it will be used at the project level to identify the correct ALM configuration.
|
| ### Setting your project settings
|
| Go to **Administration > General Settings > Pull Request decoration** and select your Configuration name.

## Multiple ALM instances
SonarQube lets you decorate Pull Requests from multiple ALM instances. To do this, you can create a configuration (as shown in the previous section) for each of your ALM instances. That instance configuration can then be assigned to the appropriate projects. 

## Issue links
During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. However, for this to work correctly, the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) must be set correctly. Otherwise the links will default to `localhost`.
