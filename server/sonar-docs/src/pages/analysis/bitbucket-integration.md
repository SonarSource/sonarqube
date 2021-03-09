---
title: Bitbucket Server Integration
url: /analysis/bitbucket-integration/
---
SonarQube's integration with Bitbucket Server allows you to maintain code quality and security in your Bitbucket Server repositories.

With this integration, you'll be able to:

- **Import your BitBucket Server repositories** - Import your Bitbucket Server repositories into SonarQube to easily set up SonarQube projects.  
- **Add pull request decoration** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in Bitbucket Server so you know if it's safe to merge your changes.

## Prerequisites
Integration with Bitbucket Server requires at least Bitbucket Server version 5.15.

### Branch Analysis
Community Edition doesn't support the analysis of multiple branches, so you can only analyze your main branch. Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and pull requests.

## Importing your Bitbucket Server repositories into SonarQube
Setting up the import of BitBucket Server repositories into SonarQube allows you to easily create SonarQube projects from your Bitbucket Server repositories. If you're using [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above, this is also the first step in adding pull request decoration.

To set up the import of BitBucket Server repositories:

1. Set your global ALM integration settings
1. Add a personal access token for importing repositories

### Setting your global ALM Integration settings
To set your global ALM Integration settings, navigate to **Administration > ALM Integrations**, select the **Bitbucket** tab, and select **Bitbucket Server** as the variant you want to configure. From here, specify the following settings:

- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your Bitbucket Server configuration at the project level. Use something succinct and easily recognizable.
- **Bitbucket Server URL** – your instances URL. For example, `https://bitbucket-server.your-company.com`.
- **Personal Access Token** – A Bitbucket Server user account is used to decorate Pull Requests. We recommend using a dedicated Bitbucket Server account with Administrator permissions. You need a [Personal Access Token](https://confluence.atlassian.com/bitbucketserver0515/personal-access-tokens-961275199.html) from this account with **Write** permission for the repositories that will be analyzed. This personal access token is used for pull request decoration, and you'll be asked for another personal access token for importing projects in the following section.

### Adding a personal access token for importing repositories
After setting your global settings, you can add a project from Bitbucket Server by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting **Bitbucket**.

Then, you'll be asked to provide a personal access token from your user account with `Read` permissions for both projects and repositories. This token will be stored in SonarQube and can be revoked at anytime in Bitbucket Server.

After saving your personal access token, you'll see a list of your Bitbucket Server projects that you can **set up** to add them to SonarQube. Setting up your projects this way also sets your project settings for pull request decoration.

## Adding pull request decoration to Bitbucket Server
Pull request decoration shows your Quality Gate and analysis metrics directly in Bitbucket Server:

After you've set up SonarQube to import your Bitbucket Server repositories as shown in the previous section, the simplest way to add pull request decoration is by adding a project from Bitbucket Server by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting **Bitbucket**.

Then, follow the steps in SonarQube to analyze your project. The project settings for pull request decoration are set automatically.

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

### Adding pull request decoration to a manually created or existing project
To add pull request decoration to a manually created or existing project, make sure your global ALM Integration settings are configured as shown in the **Importing your Bitbucket Server repositories into SonarQube** section above, and set the following project settings at **Project Settings > General Settings > Pull Request Decoration**:

- **Configuration name** – The configuration name that corresponds to your ALM instance.
- **Project Key** – the project key is part of your BitBucket Server repository URL (.../projects/**{KEY}**/repos/{SLUG}/browse).
- **Repository SLUG** – The repository slug is part of your BitBucket Server repository URL (.../projects/{KEY}/repos/**{SLUG}**/browse).

[[info]]
| If you add your project manually, your main branch defaults to the name "master". You can rename it from the project settings at **Administration > Branches and Pull Requests**.

### Advanced pull request decoration configuration

[[collapse]]
| ## Adding pull request decoration to projects that are part of a mono repository
|
| In a mono repository setup, multiple SonarQube projects, each corresponding to a separate mono repository project, are all bound to the same BitBucket Server repository. You'll need to set up pull request decoration for each SonarQube project that is part of a mono repository.
|
| Pull request decoration for a mono repository setup is supported starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html). Decorating pull requests in Developer Edition might lead to unexpected behavior.
|
| To add pull request decoration to a project that's part of a mono repository, set your project up manually as shown in the **Adding pull request decoration to a manually created or existing project** above. You also need to set the **Enable mono repository support** setting to true.
|
| After setting your project settings, you need to ensure the correct project is being analyzed by adjusting the analysis scope and pass your project names to the scanner. See the following sections for more information.
|
| ### Ensuring the correct project is analyzed
| You need to adjust the analysis scope to make sure SonarQube doesn't analyze code from other projects in your mono repository. To do this set up a **Source File Inclusion** for your  project at **Project Settings > Analysis Scope** with a pattern that will only include files from the appropriate folder. For example, adding `./MyFolderName/**/*` to your inclusions would only include analysis of code in the `MyFolderName` folder. See [Narrowing the Focus](/project-administration/narrowing-the-focus/) for more information on setting your analysis scope.
|
| ### Passing project names to the scanner
| Because of the nature of a mono repository, SonarQube scanners might read all project names of your mono repository as identical. To avoid having multiple projects with the same name, you need to pass the `sonar.projectName` parameter to the scanner. For example, if you're using the Maven scanner, you would pass `mvn sonar:sonar -Dsonar.projectName=YourProjectName`.

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

## Preventing pull request merges when the Quality Gate fails
After setting up pull request analysis, you can block pull requests from being merged if it is failing the Quality Gate. To do this:
1. In Bitbucket Server, navigate to **Repository settings > Code Insights**.
2. Add a **Required report** called `com.sonarsource.sonarqube`

[[info]]
|If your SonarQube project is configured as part of a mono repository in Enterprise Edition or above, you need to use a **Required report** that uses a SonarQube project key (`com.sonarsource.sonarqube_{sq-project-key}` instead of `com.sonarsource.sonarqube`).

3. Select **Must pass** as the **Required status**.
4. Select **Must not have any annotations** as the **Annotation requirements**.
