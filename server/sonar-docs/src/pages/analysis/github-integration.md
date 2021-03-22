---
title: GitHub Integration
url: /analysis/github-integration/
---

SonarQube's integration with GitHub Enterprise and GitHub.com allows you to maintain code quality and security in your GitHub repositories.

With this integration, you'll be able to:

- **Import your GitHub repositories** - Import your GitHub repositories into SonarQube to easily set up SonarQube projects.  
- **Analyze projects with GitHub Actions** - Integrate analysis into your build pipeline. Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), SonarScanners running in GitHub Actions jobs can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner.
- **Add pull request decoration** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in GitHub so you know if it's safe to merge your changes.
- **Authenticate with GitHub** - Sign in to SonarQube with your GitHub credentials.  

## Prerequisites
To add pull request decoration to Checks in GitHub Enterprise, you must be running GitHub Enterprise version 2.15+.

### Branch Analysis
Community Edition doesn't support the analysis of multiple branches, so you can only analyze your main branch. With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and pull requests.

## Importing your GitHub repositories to SonarQube
You need to use a GitHub App to connect SonarQube and GitHub so you can import your GitHub repositories into SonarQube. This is also the first step in adding authentication and, if you're using [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above, the first step in adding pull request decoration.

If you want to set up authentication without importing your GitHub repositories, see the **Creating a dedicated app for authentication** section below for instructions on setting up authentication.

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
- If setting up **GitHub Authentication**, in addition to the aforementioned Repository permissions, grant access for the following **User permissions**:

  | Permission          | Access       |
  |---------------------|--------------|
  | Email addresses     | Read-only    |

  And grant access for the following **Organization permissions**:

  | Permission          | Access       |
  |---------------------|--------------|
  | Members             | Read-only    |
  | Projects            | Read-only    |

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

## Analyzing projects with GitHub Actions
SonarScanners running in GitHub Actions can automatically detect branches and pull requests being built so you don't need to specifically pass them as parameters to the scanner.

To analyze your projects with GitHub Actions, you need to:
- Create your GitHub Secrets.
- Create your workflow YAML file.
- Commit and push your code to start the analysis

### Community Edition

Community Edition doesn't support multiple branches, so you should only analyze your main branch. You can restrict analysis to your main branch by setting it as the only branch in your `on.push.branches` configuration in your workflow YAML file, and not using the `on.pull_request` part.

### Creating your GitHub Secrets
You can create repository secrets from your GitHub repository. See GitHub's documentation on [Encrypted secrets](https://docs.github.com/en/actions/reference/encrypted-secrets) for more information. 

You need to set the following GitHub repository secrets to analyze your projects with GitHub Actions:

- `SONAR_TOKEN` – Generate a SonarQube [token](/user-guide/user-token/) and, in GitHub, create a new repository secret in GitHub with `SONAR_TOKEN` as the **Name** and the token you generated as the **Value**.

- `SONAR_HOST_URL` – In GitHub, create a new repository secret with `SONAR_HOST_URL` as the **Name** and your SonarQube server URL as the **Value**.

### Creating your configuration file
The following examples show you how to configure your `.github/workflows/build.yml` file. Click the scanner you're using below to expand the example configuration:

[[collapse]]
| ## SonarScanner for Maven
|
| **Note:** A project key might have to be provided through a `pom.xml` file, or through the command line parameter. For more information, see the [SonarScanner for Maven](/analysis/scan/sonarscanner-for-maven/) documentation.
| 
| Write the following in your workflow YAML file:
|
|```
| name: Build
| on:
|   push:
|     branches:
|       - master # or the name of your main branch
|   pull_request:
|     types: [opened, synchronize, reopened]
| jobs:
|   build:
|     name: Build
|     runs-on: ubuntu-latest
|     steps:
|       - uses: actions/checkout@v2
|         with:
|           fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis
|       - name: Set up JDK 11
|         uses: actions/setup-java@v1
|         with:
|           java-version: 11
|       - name: Cache SonarQube packages
|         uses: actions/cache@v1
|         with:
|           path: ~/.sonar/cache
|           key: ${{ runner.os }}-sonar
|           restore-keys: ${{ runner.os }}-sonar
|       - name: Cache Maven packages
|         uses: actions/cache@v1
|         with:
|           path: ~/.m2
|           key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
|           restore-keys: ${{ runner.os }}-m2
|       - name: Build and analyze
|         env:
|           GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # Needed to get PR information, if any
|           SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
|           SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
|         run: mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
| ```

[[collapse]]
| ## SonarScanner for Gradle
|
| **Note:** A project key might have to be provided through a `build.gradle` file, or through the command line parameter. For more information, see the [SonarScanner for Gradle](/analysis/scan/sonarscanner-for-gradle/) documentation.
|
| Add the following to your `build.gradle` file:
|
| ```
| plugins {
|   id "org.sonarqube" version "3.1"
| }
| ```
|
| Write the following in your workflow YAML file:
|
| ```
| name: Build
| on:
|   push:
|     branches:
|       - master # or the name of your main branch
|   pull_request:
|     types: [opened, synchronize, reopened]
| jobs:
|   build:
|     name: Build
|     runs-on: ubuntu-latest
|     steps:
|       - uses: actions/checkout@v2
|         with:
|           fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis
|       - name: Set up JDK 11
|         uses: actions/setup-java@v1
|         with:
|           java-version: 11
|       - name: Cache SonarQube packages
|         uses: actions/cache@v1
|         with:
|           path: ~/.sonar/cache
|           key: ${{ runner.os }}-sonar
|           restore-keys: ${{ runner.os }}-sonar
|       - name: Cache Gradle packages
|         uses: actions/cache@v1
|         with:
|           path: ~/.gradle/caches
|           key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
|           restore-keys: ${{ runner.os }}-gradle
|       - name: Build and analyze
|         env:
|           GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # Needed to get PR information, if any
|           SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
|           SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
|         run: ./gradlew build sonarqube --info 
| ```


[[collapse]]
| ## SonarScanner for .NET
| 
| Write the following in your workflow YAML file:
| 
| ```
| name: Build
| on:
|   push:
|     branches:
|       - master # or the name of your main branch
|   pull_request:
|     types: [opened, synchronize, reopened]
| jobs:
|   build:
|     name: Build
|     runs-on: windows-latest
|     steps:
|       - name: Set up JDK 11
|         uses: actions/setup-java@v1
|         with:
|           java-version: 1.11
|       - uses: actions/checkout@v2
|         with:
|           fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis
|       - name: Cache SonarQube packages
|         uses: actions/cache@v1
|         with:
|           path: ~\sonar\cache
|           key: ${{ runner.os }}-sonar
|           restore-keys: ${{ runner.os }}-sonar
|       - name: Cache SonarQube scanner
|         id: cache-sonar-scanner
|         uses: actions/cache@v1
|         with:
|           path: .\.sonar\scanner
|           key: ${{ runner.os }}-sonar-scanner
|           restore-keys: ${{ runner.os }}-sonar-scanner
|       - name: Install SonarQube scanner
|         if: steps.cache-sonar-scanner.outputs.cache-hit != 'true'
|         shell: powershell
|         run: |
|           New-Item -Path .\.sonar\scanner -ItemType Directory
|           dotnet tool update dotnet-sonarscanner --tool-path .\.sonar\scanner
|       - name: Build and analyze
|         env:
|           GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # Needed to get PR information, if any
|         shell: powershell
|         run: |
|           .\.sonar\scanner\dotnet-sonarscanner begin /k:"example" /d:sonar.login="${{ secrets.SONAR_TOKEN }}" /d:sonar.host.url="${{ secrets.SONAR_HOST_URL }}"
|           dotnet build
|           .\.sonar\scanner\dotnet-sonarscanner end /d:sonar.login="${{ secrets.SONAR_TOKEN }}"
| ```


[[collapse]]
| ## SonarScanner CLI
| 
| **Note:** A project key has to be provided through a `sonar-project.properties` file, or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.
| 
| Write the following in your workflow YAML file:
|
| ```
| name: Build
| on:
|   push:
|     branches:
|       - master # or the name of your main branch
|   pull_request:
|     types: [opened, synchronize, reopened]
| jobs:
|   build:
|     name: Build
|     runs-on: ubuntu-latest
|     steps:
|       - uses: actions/checkout@v2
|         with:
|           fetch-depth: 0
|       - uses: docker://sonarsource/sonar-scanner-cli:latest
|         env:
|           GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
|           SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
|           SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
| ```

#### **Failing the pipeline job when the Quality Gate fails**
In order for the Quality Gate to fail on the GitLab side when it fails on the SonarQube side, the scanner needs to wait for the SonarQube Quality Gate status. To enable this, set the `sonar.qualitygate.wait` property to `true` (check the above scanners' documentation to know where to set this property).

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

### Commit and push your code
Commit and push your code to start the analysis. Each new push you make on your branches or pull requests will trigger a new analysis in SonarQube.

## Adding pull request decoration to GitHub
After creating and installing your GitHub App above, you can add pull request decoration to show your Quality Gate and analysis metrics directly in GitHub: 

The simplest way to add pull request decoration is by adding a project from GitHub by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting **GitHub**.

Then, follow the steps in SonarQube to analyze your project. The project settings for pull request decoration are set automatically.

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

### Adding pull request decoration to a manually created or existing project
To add pull request decoration to a manually created or existing project, after you've created and installed your GitHub App and updated your global ALM Integration settings as shown in the **Importing your GitHub repositories into SonarQube** section above, set the following project settings at **Project Settings > General Settings > Pull Request Decoration**: 

- **Configuration name** – The configuration name that corresponds to your GitHub instance. 
- **Repository identifier** – The path of your repository URL.

### Advanced pull request decoration configuration

[[collapse]]
| ## Adding pull request decoration to projects that are part of a mono repository
|
| _Pull request decoration for a mono repository setup is supported starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html)._
|
| In a mono repository setup, multiple SonarQube projects, each corresponding to a separate mono repository project, are all bound to the same GitHub repository. You'll need to set up pull request decoration for each SonarQube project that is part of a mono repository.
|
| To add pull request decoration to a project that's part of a mono repository, set your project up manually as shown in the **Adding pull request decoration to a manually created or existing project** section above. You also need to set the **Enable mono repository support** setting to true. 
|
| After setting your project settings, you need to ensure the correct project is being analyzed by adjusting the analysis scope and pass your project names to the scanner. See the following sections for more information.
|
| ### Ensuring the correct project is analyzed
| You need to adjust the analysis scope to make sure SonarQube doesn't analyze code from other projects in your mono repository. To do this set up a **Source File Inclusion** for your  project at **Project Settings > Analysis Scope** with a pattern that will only include files from the appropriate folder. For example, adding `./MyFolderName/**/*` to your inclusions would only include analysis of code in the `MyFolderName` folder. See [Narrowing the Focus](/project-administration/narrowing-the-focus/) for more information on setting your analysis scope.
|
| ### Passing project names to the scanner
| Because of the nature of a mono repository, SonarQube scanners might read all project names of your mono repository as identical. To avoid having multiple projects with the same name, you need to pass the `sonar.projectName` parameter to the scanner. For example, if you're using the Maven scanner, you would pass `mvn sonar:sonar -Dsonar.projectName=YourProjectName`.

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
If you want to use a dedicated app for GitHub authentication, you can create a GitHub OAuth app. You'll find general instructions for creating a GitHub OAuth App [here](https://docs.github.com/en/free-pro-team@latest/developers/apps/creating-an-oauth-app). Specify the following settings in your OAuth App:

- **Homepage URL** – the public URL of your SonarQube server. For example, `https://sonarqube.mycompany.com`. For security reasons, HTTP is not supported, and you must use HTTPS. The public URL is configured in SonarQube at **[Administration > General > Server base URL](/#sonarqube-admin#/admin/settings)**.
- **Authorization callback URL** – your instance's base URL. For example, `https://yourinstance.sonarqube.com`.

After creating your app, update your global SonarQube settings: 

Navigate to **Administration > Configuration > General Settings > ALM Integrations > GitHub > GitHub Authentication** and update the following:

1. **Enabled** – set the switch to `true`.
1. **Client ID** – the Client ID is found below the GitHub App ID on your GitHub App's page.
1. **Client Secret** – the Client secret is found below the Client ID on your GitHub App's page.
  
Now, from the login page, your users can connect their GitHub accounts with the new "Log in with GitHub" button.
