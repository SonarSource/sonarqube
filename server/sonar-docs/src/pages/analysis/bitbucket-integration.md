---
title: Bitbucket Server Integration
url: /analysis/bitbucket-integration/
---
SonarQube's integration with Bitbucket Server allows you to maintain code quality and security in your Bitbucket Server repositories.

Once you've set up your integration, you'll be able to:

- **Import your BitBucket repositories** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) Import your Bitbucket repositories into SonarQube to easily set up SonarQube projects.  
- **Add pull request decoration** - (starting in Developer Edition) See your Quality Gate and code metric results right in Bitbucket so you know if it's safe to merge your changes.

## Prerequisites
- Integration with Bitbucket Server requires at least Bitbucket Server version 5.15.

## Importing your Bitbucket Server repositories into SonarQube
Setting up repository import with Bitbucket Server allows you to easily create SonarQube projects from your Bitbucket Server repositories. This is also the first step in adding pull request decoration.

To import your Bitbucket Server projects into SonarQube, you need to first set your global SonarQube settings. Navigate to **Administration > Configuration > General Settings > ALM Integrations**, select the **Bitbucket** tab, and specify the following settings:
 
- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your Bitbucket Server configuration at the project level. Use something succinct and easily recognizable.
- **Bitbucket Server URL** – your instances URL. For example, `https://bitbucket-server.your-company.com`.
- **Personal Access Token** – A Bitbucket Server user account is used to decorate Pull Requests. We recommend using a dedicated Bitbucket Server account with Administrator permissions. You need a [Personal Access Token](https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html) from this account with **Write** permission for the repositories that will be analyzed.

## Adding pull request decoration to Bitbucket Server
Pull request decoration shows your Quality Gate and analysis metrics directly in Bitbucket Server:

![pull request decoration](/images/github-branch-decoration.png) 

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

After you've set up SonarQube to import your Bitbucket Server repositories as shown in the previous section, the simplest way to add pull request decoration is by importing a Bitbucket Server repository:

![import a Bitbucket project](/images/add-bitbucket-project.png)

Follow the steps in SonarQube to automatically sets your project settings for pull request decoration. When creating your project, you'll need to provide a personal access token from your user account with `Read` permissions for both projects and repositories. This personal access token will be stored in SonarQube until you revoke it on the Bitbucket Server side.

### Adding pull request decoration to a manually created or existing project
To add pull request decoration to a manually created or existing project, after you've created and installed your GitHub App and updated your global ALM Integration settings as shown above, set your project settings at **Project Settings > General Settings > Pull Request Decoration**. 

From here, set your: 
- **Configuration name** – The configuration name that corresponds to your ALM instance.
- **Project Key** – Part of your BitBucket Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
- **Repo Slug** – Part of your BitBucket Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).

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
