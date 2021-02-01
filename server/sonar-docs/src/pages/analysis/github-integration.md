---
title: GitHub Integration
url: /analysis/github-integration/
---

SonarQube's integration with GitHub Enterprise and GitHub.com allows you to maintain code quality and security in your GitHub repositories.

With this integration, you'll be able to:

- **Import your GitHub repositories** - Import your GitHub repositories into SonarQube to easily set up SonarQube projects.  
- **Add pull request decoration** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in GitHub so you know if it's safe to merge your changes.
- **Authenticate with GitHub** - Sign in to SonarQube with your GitHub credentials.  

## Prerequisites
To add pull request decoration to Checks in GitHub Enterprise, you must be running GitHub Enterprise version 2.15+.

## Importing your GitHub repositories to SonarQube
You need to use a GitHub App to connect SonarQube and GitHub so you can import your GitHub repositories into SonarQube. If you're using [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above, this is also the first step in adding pull request decoration.

[[info]]
|If you're using Community Edition or want to set up authentication without importing your GitHub repositories, see the **Creating a dedicated app for authentication** section below for instructions on setting up authentication.

In this section, you'll complete the following steps to connect SonarQube and GitHub with a GitHub App:

1. Create your GitHub App.
1. Install your GitHub App in your organization.
1. Update your SonarQube global settings with your GitHub App information.

### Step 1: Creating your GitHub App
See GitHub's documentation on [creating a GitHub App](https://docs.github.com/apps/building-github-apps/creating-a-github-app/) for general information on creating your app. 

Specify the following settings in your app:

- **GitHub App Name** – Your app's name.
- **Homepage URL** – You can use any URL, such as `https://www.sonarqube.org/`.
- **User authorization callback URL** – Your instance's base URL. For example, `https://yourinstance.sonarqube.com`.
- **Webhook URL** – Your instance's base URL. For example, `https://yourinstance.sonarqube.com`.
- Grant access for the following **Repository permissions**:

	| Permission          | Access       |
	|---------------------|--------------|
	| Checks              | Read & write |
	| **GitHub Enterprise:** Repository metadata <br> **GitHub.com:** Metadata <br> (this setting is automatically set by GitHub)| Read-only |
	| Pull Requests       | Read & write |
	| Commit statuses     | Read-only    |

- Under "Where can this GitHub App be installed?," select **Any account**.

[[warning]]
| For security reasons, make sure you're using `HTTPS` protocol for your URLs in your app.

### Step 2: Installing your GitHub App in your organization
Next, you need to install your GitHub App in your organizations. See GitHub's documentation on [installing GitHub Apps](https://docs.github.com/en/free-pro-team@latest/developers/apps/installing-github-apps) for more information.

### Step 3: Updating your SonarQube global settings with your GitHub App information
After you've created and installed your GitHub App, update your global SonarQube settings to finish integration and allow for the import of GitHub projects.

Navigate to **Administration > Configuration > General Settings > ALM Integrations > GitHub** and specify the following settings:

- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your GitHub configuration at the project level. Use something succinct and easily recognizable.
- **GitHub URL** – For example, `https://github.company.com/api/v3` for GitHub Enterprise or `https://api.github.com/` for GitHub.com.
- **GitHub App ID** – The App ID is found on your GitHub App's page on GitHub at **Settings > Developer Settings > GitHub Apps**. 
- **Client ID** – The Client ID is found on your GitHub App's page.
- **Client secret** – The Client secret is found on your GitHub App's page.
- **Private Key** – Your GitHub App's private key. You can generate a `.pem` file from your GitHub App's page under **Private keys**. Copy and paste the contents of the file here.

## Adding pull request decoration to GitHub
After creating and installing your GitHub App above, you can add pull request decoration to show your Quality Gate and analysis metrics directly in GitHub: 

![pull request decoration](/images/github-branch-decoration.png)

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

The simplest way to add pull request decoration is by adding a project from GitHub by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting **GitHub**.

Then, follow the steps in SonarQube to analyze your project. The project settings for pull request decoration are set automatically.

### Adding pull request decoration to a manually created or existing project
To add pull request decoration to a manually created or existing project, after you've created and installed your GitHub App and updated your global ALM Integration settings as shown above, set your project settings at **Project Settings > General Settings > Pull Request Decoration**. 

From here, set your: 

- **Configuration name** – The configuration name that corresponds to your GitHub instance. 
- **Repository identifier** – The path of your repository URL.

### Advanced pull request decoration configuration

[[collapse]]
| ## Adding pull request decoration to projects that are part of a mono repository
|
| In a mono repository setup, multiple SonarQube projects, each corresponding to a separate mono repository project, are all bound to the same repository. You'll need to set up pull request decoration for each SonarQube project.
|
| In Developer Edition, analyzing a pull request for a project that is part of a mono repository will override the current pull request decoration even if you're analyzing a different project.
| 
| In [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and above, you can show pull request decoration for multiple projects simultaneously. 
|
| To do this, set your global ALM Integration settings as shown in the **Importing your GitHub repositories into SonarQube** section above, and set your project settings at **Project Settings > General Settings > Pull Request Decoration**.
|
| From here, set your: 
|
| - **Configuration name** – The configuration name that corresponds to your ALM instance.
| - **Project Key** – Part of your BitBucket Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
| - **Repo Slug** – Part of your BitBucket Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).
| - **Enable mono repository support** – (Enterprise Edition and above only) Set this to true. 

[[collapse]]
| ## Showing the analysis summary under the GitHub Conversation tab
| By default, **Enable analysis summary under the GitHub Conversation tab** is on and your pull request analysis will be shown under both the **Conversation** and **Checks** tabs in GitHub. When off, your pull request analysis summary is only shown under the **Checks** tab.

[[collapse]]
| ## Configuring multiple ALM instances
|You can decorate pull requests from multiple ALM instances by creating a configuration for each ALM instance and then assigning that instance configuration to the appropriate projects. 
|
|- As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each ALM. 
|
|- Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can create multiple configurations for each ALM. If you have multiple configurations of the same ALM connected to SonarQube, you have to create projects manually.

[[collapse]]
| ## Linking issues
| During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. For this to work correctly, you need to set the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) correctly. Otherwise, the links will default to `localhost`.

## Setting up GitHub authentication
To allow users to log in with GitHub credentials, use the GitHub App that you created above (see the **Importing your GitHub repositories using a GitHub App** section for more information) and update your global SonarQube settings.

[[info]]
| If you're using Community Edition or you want to use a dedicated app for GitHub authentication, see the **Creating a dedicated app for authentication** section below.

To update your global SonarQube settings:

Navigate to **Administration > Configuration > General Settings > ALM Integrations > GitHub > GitHub Authentication** and update the following:

1. **Enabled** – set the switch to `true`.
1. **Client ID** – the Client ID is found below the GitHub App ID on your GitHub App's page.
1. **Client Secret** – the Client secret is found below the Client ID on your GitHub App's page.
  
Now, from the login page, your users can connect their GitHub accounts with the new "Log in with GitHub" button.

### Creating a dedicated app for authentication
If you're using Community Edition or you want to use a dedicated app for GitHub authentication, you can create a GitHub OAuth app. You'll find general instructions for creating a GitHub OAuth App [here](https://docs.github.com/en/free-pro-team@latest/developers/apps/creating-an-oauth-app). Specify the following settings in your OAuth App:

- **Homepage URL** – the public URL of your SonarQube server. For example, `https://sonarqube.mycompany.com`. For security reasons, HTTP is not supported, and you must use HTTPS. The public URL is configured in SonarQube at **[Administration > General > Server base URL](/#sonarqube-admin#/admin/settings)**.
- **Authorization callback URL** – your instance's base URL. For example, `https://yourinstance.sonarqube.com`.

After creating your app, update your global SonarQube settings: 

Navigate to **Administration > Configuration > General Settings > ALM Integrations > GitHub > GitHub Authentication** and update the following:

1. **Enabled** – set the switch to `true`.
1. **Client ID** – the Client ID is found below the GitHub App ID on your GitHub App's page.
1. **Client Secret** – the Client secret is found below the Client ID on your GitHub App's page.
  
Now, from the login page, your users can connect their GitHub accounts with the new "Log in with GitHub" button.