---
title: Bitbucket Cloud Integration
url: /analysis/bitbucket-cloud-integration/
---

SonarQube's integration with Bitbucket Cloud allows you to maintain code quality and security in your Bitbucket Cloud repositories.

With this integration, you'll be able to:

- **Import your BitBucket Cloud repositories** – Import your Bitbucket Cloud repositories into SonarQube to easily set up SonarQube projects.
- **Analyze projects with Bitbucket Pipelines** – Integrate analysis into your build pipeline. SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner (branch and pull request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)).
- **Report your Quality Gate status to your pull requests** – (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in Bitbucket Cloud so you know if it's safe to merge your changes.
- **Authenticate with Bitbucket Cloud** - Sign in to SonarQube with your Bitbucket Cloud credentials.

## Importing your Bitbucket Cloud repositories into SonarQube

Setting up the import of BitBucket Cloud repositories into SonarQube allows you to easily create SonarQube projects from your Bitbucket Cloud repositories. This is also the first step in adding authentication and, starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), the first step in reporting your analysis and Quality Gate status to your pull requests.

[[info]]
| To import your Bitbucket repositories into SonarQube, you can only have one global configuration of Bitbucket, including Bitbucket Server and Bitbucket Cloud. See the **Configuring multiple DevOps Platform instances** section below for more information.

To set up the import of BitBucket Cloud repositories:

1. Create an OAuth consumer.
1. Set your global DevOps Platform integration settings.
1. Add your Bitbucket username and an app password.

### Creating your OAuth consumer
SonarQube uses a dedicated [OAuth consumer](https://support.atlassian.com/bitbucket-cloud/docs/use-oauth-on-bitbucket-cloud/) to import repositories and display your Quality Gate status on pull requests. Create the OAuth consumer in your Bitbucket Cloud workspace settings and specify the following:

- **Name** – the name of your OAuth consumer
- **Callback URL** – Bitbucket Cloud requires this field, but it's not used by SonarQube so you can use any URL.
- **This is a private consumer** – Your OAuth consumer needs to be private. Make sure this check box is selected.
- **Permissions** – Grant **Read** access for the **Pull requests** permission.

### Setting your global DevOps Platform Integration settings
To set your global DevOps Platform Integration settings, navigate to **Administration > DevOps Platform Integrations**, select the **Bitbucket** tab, and select **Bitbucket Cloud** as the variant you want to configure. From here, specify the following settings:

- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your Bitbucket Cloud configuration at the project level. Use something succinct and easily recognizable.
- **Workspace ID** – The workspace ID is part of your bitbucket cloud URL `https://bitbucket.org/{WORKSPACE-ID}/{repository-slug}`
- **OAuth Key** – Bitbucket automatically creates an OAuth key when you create your OAuth consumer. You can find it in your Bitbucket Cloud workspace settings under **OAuth consumers**.
- **OAuth Secret** – Bitbucket automatically creates an OAuth secret when you create your OAuth consumer. You can find it in your Bitbucket Cloud workspace settings under **OAuth consumers**. Administrators can encrypt this secret at **Administration > Configuration > Encryption**. See the **Settings Encryption** section of the [Security](/instance-administration/security/) page for more information.

### Adding your Bitbucket username and an app password
After setting your global settings, you can add a project from Bitbucket Cloud by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting **Bitbucket**.

Then, you'll be asked to provide your Bitbucket username and an [app password](https://support.atlassian.com/bitbucket-cloud/docs/app-passwords/). Your app password needs the **repository:read** permission.

After adding your Bitbucket username and app password, you'll see a list of your Bitbucket Cloud projects that you can **set up** to add them to SonarQube. Setting up your projects this way also sets your project settings for displaying your Quality Gate status on pull requests.

## Analyzing projects with Bitbucket Pipelines
SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner.

To analyze your projects with Bitbucket Pipelines, you need to:
- Set your environment variables.
- Configure your `bitbucket-pipelines.yml file`.

### Setting environment variables
You can set environment variables securely for all pipelines in Bitbucket Cloud's settings. See [User-defined variables](https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/#User-defined-variables) for more information.

[[info]]
| You may need to commit your `bitbucket-pipelines.yml` before being able to set environment variables for pipelines.

You need to set the following environment variables in Bitbucket Cloud for analysis:

- `SONAR_TOKEN` – Generate a SonarQube [token](/user-guide/user-token/) for Bitbucket Cloud and create a custom **secured** environment variable in Bitbucket Cloud with `SONAR_TOKEN` as the **Name** and the token you generated as the **Value**.
- `SONAR_HOST_URL` – Create a custom environment variable with `SONAR_HOST_URL` as the **Name** and your SonarQube server URL as the **Value**.

### Configuring your bitbucket-pipelines.yml file
This section shows you how to configure your `bitbucket-pipelines.yml` file.

You'll set up your build according to your SonarQube edition:

- **Community Edition** – Community Edition doesn't support multiple branches, so you should only analyze your main branch. You can restrict analysis to your main branch by setting it as the only branch in your `branches` pipeline in your `bitbucket-pipelines.yml` file and not using the `pull-requests` pipeline.
- **Developer Edition and above** – Bitbucket Pipelines can build specific branches and pull requests if you use the `branches` and `pull-requests` pipelines as shown in the example configurations below.

Click the scanner you're using below to expand the example configuration:

**Note:** This assumes a typical Gitflow workflow. See [Use glob patterns on the Pipelines YAML file](https://support.atlassian.com/bitbucket-cloud/docs/use-glob-patterns-on-the-pipelines-yaml-file/) provided by Atlassian for more information on customizing what branches or pull requests trigger an analysis.

[[collapse]]
| ## SonarScanner for Gradle
|
| **Note:** A project key might have to be provided through a `build.gradle` file, or through the command line parameter. For more information, see the [SonarScanner for Gradle](/analysis/scan/sonarscanner-for-gradle/) documentation.
|
| Add the following to your `build.gradle` file:
|
| ```
| plugins {
|   id "org.sonarqube" version "3.4.0.2513"
| }
| ```
|
| Write the following in your `bitbucket-pipelines.yml`:
|
| ```
| image: openjdk:8
|
| definitions:
|   steps:
|     - step: &build-step
|         name: SonarQube analysis
|         caches:
|           - gradle
|           - sonar
|         script:
|           - bash ./gradlew sonarqube
|   caches:
|     sonar: ~/.sonar
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,main,develop}':
|       - step: *build-step
|
|   pull-requests:
|     '**':
|       - step: *build-step
| ```

[[collapse]]
| ## SonarScanner for Maven
|
| **Note:** A project key might have to be provided through the command line parameter. For more information, see the [SonarScanner for Maven](/analysis/scan/sonarscanner-for-maven/) documentation.
|
| Write the following in your `bitbucket-pipelines.yml`:
|
| ```
| image: maven:3.3.9
|
| definitions:
|   steps:
|     - step: &build-step
|         name: SonarQube analysis
|         caches:
|           - maven
|           - sonar
|         script:
|           - mvn verify sonar:sonar
|   caches:
|     sonar: ~/.sonar
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,main,develop}':
|       - step: *build-step
|
|   pull-requests:
|     '**':
|       - step: *build-step
| ```

[[collapse]]
| ## SonarScanner for .NET
|
| Write the following in your `bitbucket-pipelines.yml`:
| 
| ```
| image: mcr.microsoft.com/dotnet/core/sdk:latest
|
| definitions:
|   steps:
|     - step: &build-step
|         name: SonarQube analysis
|         caches:
|           - dotnetcore
|           - sonar
|         script:
|           - apt-get update
|           - apt-get install --yes openjdk-11-jre
|           - dotnet tool install --global dotnet-sonarscanner
|           - export PATH="$PATH:/root/.dotnet/tools"
|           - dotnet sonarscanner begin /k:"YOUR_PROJECT_KEY*" /d:"sonar.login=${SONAR_TOKEN}"  /d:"sonar.host.url=${SONAR_HOST_URL}"
|           - dotnet build 
|           - dotnet sonarscanner end /d:"sonar.login=${SONAR_TOKEN}"
|   caches:
|     sonar: ~/.sonar
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,main,develop}':
|       - step: *build-step
|   pull-requests:
|     '**':
|       - step: *build-step
| ```

[[collapse]]
| ## SonarScanner CLI
|
| You can set up the SonarScanner CLI configuration the following ways:
|
| - **SonarQube Scan Bitbucket Pipe** – Using the SonarQube Scan Bitbucket Pipe is an easy way to set up a basic configuration. You'll find the Bitbucket Pipe and configuration instructions on the [SonarQube Scan Bitbucket Pipe](https://bitbucket.org/sonarsource/sonarqube-scan/) page.
|
| - **Advanced Configuration** – If you need an advanced setup that allows for scanner caching, you can add the following to your `bitbucket-pipelines.yml` file:
|
|   [[info]]
|   | This configuration is an alternative to the SonarQube Scan Bitbucket Pipe. If you do not need a setup that allows for scanner caching, we recommend using the Bitbucket Pipe.
|
| ```
| image: maven:3.3.9
|
| definitions:
|   steps: &build-step
|     - step:
|         name: SonarQube analysis
|         image: sonarsource/sonar-scanner-cli:latest
|         caches:
|           - sonar
|         script:
|           - sonar-scanner
|   caches:
|     sonar: /opt/sonar-scanner/.sonar
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,main,develop}':
|       - step: *build-step
|
|   pull-requests:
|     '**':
|       - step: *build-step
| ```
|
| [[info]]
| | A project key has to be provided through a `sonar-project.properties` file, or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.

#### **Failing the pipeline job when the Quality Gate fails**
You can use the [SonarQube Quality Gate Check Bitbucket Pipe](https://bitbucket.org/sonarsource/sonarqube-quality-gate) to ensure your code meets your quality standards by failing your pipeline job when your [Quality Gate](/user-guide/quality-gates/) fails.

If you do not want to use the SonarQube Quality Gate Check Pipe, you can instruct the scanner to wait for the SonarQube Quality Gate status at the end of the analysis. To enable this, pass the `-Dsonar.qualitygate.wait=true` parameter to the scanner in the `bitbucket-pipelines.yml` file.

This will make the analysis step poll SonarQube regularly until the Quality Gate is computed. This will increase your pipeline duration. Note that, if the Quality Gate is red, this will make the analysis step fail, even if the actual analysis itself is successful. We advise only using this parameter when necessary (for example, to block a deployment pipeline if the Quality Gate is red). It should not be used to report the Quality Gate status in a pull request.

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

### For more information
For more information on configuring your build with Bitbucket Pipelines, see the [Configure bitbucket-pipelines.yml](https://support.atlassian.com/bitbucket-cloud/docs/configure-bitbucket-pipelinesyml/) documentation provided by Atlassian.

## Reporting your Quality Gate status in Bitbucket Cloud

After creating and installing your OAuth consumer above, SonarQube can report your Quality Gate status and analysis metrics directly to your Bitbucket Cloud pull requests.

To do this, add a project from Bitbucket by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and select **Bitbucket** from the drop-down menu.

Then, follow the steps in SonarQube to analyze your project. SonarQube automatically sets the project settings required to show your Quality Gate in your pull requests.

[[info]]
| To report your Quality Gate status in your pull requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for pull request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

If you're creating your projects manually or adding Quality Gate reporting to an existing project, see the following section.

### Reporting your Quality Gate status in manually created or existing projects
SonarQube can also report your Quality Gate status to Bitbucket Cloud pull requests for existing and manually-created projects. After you've created and installed your OAuth consumer and updated your global settings as shown in the **Importing your Bitbucket Cloud repositories into SonarQube** section above, set the following project settings at **Project Settings > General Settings > DevOps Platform Integration**: 

- **Configuration name** – The configuration name that corresponds to your GitHub instance. 
- **Repository SLUG** – The Repository SLUG is part of your Bitbucket Cloud URL. For example, `https://bitbucket.org/{workspace}/{repository}`

### Advanced configuration

[[collapse]]
| ## Reporting your Quality Gate status on pull requests in a mono repository
|
| _Reporting Quality Gate statuses to pull requests in a mono repository setup is supported starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html)._
|
| In a mono repository setup, multiple SonarQube projects, each corresponding to a separate project within the mono repository, are all bound to the same Bitbucket Cloud repository. You'll need to set up each SonarQube project that's part of a mono repository to report your Quality Gate status.
|
| You need to set up projects that are part of a mono repository manually as shown in the **Displaying your Quality Gate status in manually created or existing project** section above. You also need to set the **Enable mono repository support** setting to true at **Project Settings > General Settings > DevOps Platform Integration**.
|
| After setting your project settings, ensure the correct project is being analyzed by adjusting the analysis scope and pass your project names to the scanner. See the following sections for more information.
|
| ### Ensuring the correct project is analyzed
| You need to adjust the analysis scope to make sure SonarQube doesn't analyze code from other projects in your mono repository. To do this set up a **Source File Inclusion** for your  project at **Project Settings > Analysis Scope** with a pattern that will only include files from the appropriate folder. For example, adding `./MyFolderName/**/*` to your inclusions would only include analysis of code in the `MyFolderName` folder. See [Narrowing the Focus](/project-administration/narrowing-the-focus/) for more information on setting your analysis scope.
|
| ### Passing project names to the scanner
| Because of the nature of a mono repository, SonarQube scanners might read all project names of your mono repository as identical. To avoid having multiple projects with the same name, you need to pass the `sonar.projectName` parameter to the scanner. For example, if you're using the Maven scanner, you would pass `mvn sonar:sonar -Dsonar.projectName=YourProjectName`.

[[collapse]]
| ## Configuring multiple DevOps Platform instances
| SonarQube can report your Quality Gate status to multiple DevOps Platform instances. To do this, you need to create a configuration for each DevOps Platform instance and assign that configuration to the appropriate projects. 
|
| - As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each DevOps Platform. 
|
| - Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can create multiple configurations for each DevOps Platform. If you have multiple configurations of the same DevOps Platform connected to SonarQube, you have to create projects manually.

[[collapse]]
| ## Linking issues
| When adding a Quality Gate status to your pull requests, individual issues will be linked to their SonarQube counterparts automatically. For this to work correctly, you need to set the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) correctly. Otherwise, the links will default to `localhost`.

## Authenticating with Bitbucket Cloud
To allow users to log in with Bitbucket Cloud credentials, you need to use an [OAuth consumer](https://support.atlassian.com/bitbucket-cloud/docs/use-oauth-on-bitbucket-cloud/) and set the authentication settings in SonarQube. You can either use the OAuth consumer that you created above in the **Importing your Bitbucket Cloud repositories into SonarQube** section or create a new OAuth consumer specifically for authentication. See the following sections for more on setting up authentication.

### Setting your OAuth consumer settings
Create or update your OAuth consumer in your Bitbucket Cloud workspace settings and specify the following:

- **Name** – the name of your OAuth consumer.
- **Callback URL** – your SonarQube instance URL.
- **Permissions**: 
	* **Account**: **Read** and **Email** access.
	* **Workspace membership**: **Read** access.

[[info]]
| If you're using the same OAuth consumer for authentication and importing projects/reporting status to pull requests, make sure that the **This is a private consumer** box is checked and **Read** access for the **Pull requests** permission is granted. 

### Setting your authentication settings in SonarQube
To set your global authentication settings, navigate to **Administration > DevOps Platform Integrations** and select the **Bitbucket** tab. Scroll down to the **Bitbucket Authentication** section and update the following settings:

- **Enabled** - set to true.
- **OAuth consumer key** - enter the **Key** from your OAuth consumer page in Bitbucket.
- **OAuth consumer secret** - enter the **Secret** from your OAuth consumer page in Bitbucket.
- **Workspaces** - Only users from Bitbucket Workspaces that you add here will be able to authenticate in SonarQube. This is optional, but _highly_ recommended to ensure only the users you want to log in with Bitbucket credentials are able to. 
