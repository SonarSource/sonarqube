---
title: Azure DevOps Server Integration
url: /analysis/azuredevops-integration/
---
SonarQube's integration with Azure DevOps Server allows you to maintain code quality and security in your Azure DevOps Server repositories.

Once you've set up your integration, you'll be able to:

- **Import your Azure DevOps repositories** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) Import your Azure DevOps repositories into SonarQube to easily set up SonarQube projects.  
- **Add pull request decoration** - (starting in Developer Edition) See your Quality Gate and code metric results right in Azure DevOps so you know if it's safe to merge your changes.

## Prerequisites
- See the **Compatibility** section of the [SonarScanner for Azure DevOps](/analysis/scan/sonarscanner-for-azure-devops/) page for version requirements. 

## Importing your Azure DevOps repositories into SonarQube
Setting up repository import with Azure DevOps Server allows you to easily create SonarQube projects from your Azure DevOps Server repositories. This is also the first step in adding pull request decoration.

To import your Azure DevOps repositories into SonarQube, you need to first set your global SonarQube settings. Navigate to **Administration > Configuration > General Settings > ALM Integrations**, select the **Azure DevOps** tab, and specify the following settings:
 
- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your Azure DevOps configuration at the project level. Use something succinct and easily recognizable.
- **Personal Access Token** – An Azure DevOps Server user account is used to decorate Pull Requests. We recommend using a dedicated Azure DevOps Server account with Administrator permissions. You need a [personal access token](https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=tfs-2017&tabs=preview-page) from this account with the scope authorized for **Code > Read & Write** for the repositories that will be analyzed.

## Adding pull request decoration to Azure DevOps Server
Pull request decoration shows your Quality Gate and analysis metrics directly in Azure DevOps Server.

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

After you've set up SonarQube to import your Azure DevOps Server repositories as shown in the previous section, the simplest way to add pull request decoration is by importing an Azure DevOps Server repository.

Follow the steps in SonarQube to automatically set your project settings for merge request decoration. When creating your project, you'll need to provide a personal access token from your user account with the `Code (Read & Write)` scope. This personal access token will be stored in SonarQube until you revoke it on the Azure DevOps Server side.

### Adding pull request decoration to a manually created or existing project
To add pull request decoration to a manually created or existing project, after you've created and installed your GitHub App and updated your global ALM Integration settings as shown above, set your project settings at **Project Settings > General Settings > Pull Request Decoration**. 

From here, set your: 
- **Configuration name** – The configuration name that corresponds to your ALM instance.
- **Project Key** – Part of your Azure DevOps Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
- **Repo Slug** – Part of your Azure DevOps Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).

### Advanced pull request decoration configuration

[[collapse]]
| ## **Configuring multiple ALM instances**
|You can decorate pull requests from multiple ALM instances by creating a configuration for each ALM instance and then assigning that instance configuration to the appropriate projects. 
|
|- As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each ALM. 
|
|- Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can create multiple configurations for each ALM. If you have multiple configurations of the same ALM connected to SonarQube, you have to create projects manually.

[[collapse]]
| ## **Linking issues**
| During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. For this to work correctly, you need to set the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) correctly. Otherwise, the links will default to `localhost`.
