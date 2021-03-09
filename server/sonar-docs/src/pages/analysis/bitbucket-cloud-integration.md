---
title: Bitbucket Cloud Integration
url: /analysis/bitbucket-cloud-integration/
---

SonarQube's integration with Bitbucket Cloud allows you to maintain code quality and security in your Bitbucket Cloud repositories.

With this integration, you'll be able to:

- **Analyze projects with Bitbucket Pipelines** - Integrate analysis into your build pipeline. SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner (branch and pull request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)).
- **Add pull request decoration** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in Bitbucket Cloud so you know if it's safe to merge your changes.

## Analyzing projects with Bitbucket Pipelines
SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner.

### Activating builds  
Set up your build according to your SonarQube edition:

- **Community Edition** – Community Edition doesn't support multiple branches, so you should only analyze your main branch. You can restrict analysis to your main branch by setting it as the only branch in your `branches` pipeline in your `bitbucket-pipelines.yml` file and not using the `pull-requests` pipeline.
- **Developer Edition and above** – Bitbucket Pipelines can build specific branches and pull requests if you use the `branches` and `pull-requests` pipelines as shown in the example configurations below.

### Setting environment variables
You can set environment variables securely for all pipelines in Bitbucket Cloud's settings. See [User-defined variables](https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/#User-defined-variables) for more information.

[[info]]
| You may need to commit your `bitbucket-pipelines.yml` before being able to set environment variables for pipelines.

You need to set the following environment variables in Bitbucket Cloud for analysis:

- `SONAR_TOKEN` – Generate a SonarQube [token](/user-guide/user-token/) for Bitbucket Cloud and create a custom **secured** environment variable in Bitbucket Cloud with `SONAR_TOKEN` as the **Name** and the token you generated as the **Value**.
- `SONAR_HOST_URL` – Create a custom environment variable with `SONAR_HOST_URL` as the **Name** and your SonarQube server URL as the **Value**.

### Configuring your bitbucket-pipelines.yml file
The following examples show you how to configure your `bitbucket-pipelines.yml` file.

Click the scanner you're using below to expand the example configuration:

**Note:** This assumes a typical Gitflow workflow. See [Use glob patterns on the Pipelines yaml file](https://support.atlassian.com/bitbucket-cloud/docs/use-glob-patterns-on-the-pipelines-yaml-file/) provided by Atlassian for more information on customizing what branches or pull requests trigger an analysis.

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
| Write the following in your `bitbucket-pipelines.yml`:
|
| ```
| image: openjdk:8
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,develop}':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - gradle
|             - sonar
|           script:
|             - bash ./gradlew sonarqube -Dsonar.qualitygate.wait=true
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - gradle
|             - sonar
|           script:
|             - bash ./gradlew sonarqube -Dsonar.qualitygate.wait=true
|
| definitions:
|   caches:
|     sonar: ~/.sonar
| ```

[[collapse]]
| ## SonarScanner for Maven
|
| **Note:** A project key might have to be provided through a `pom.xml` file, or through the command line parameter. For more information, see the [SonarScanner for Maven](/analysis/scan/sonarscanner-for-maven/) documentation.
|
| Write the following in your `bitbucket-pipelines.yml`:
|
| ```
| image: maven:3.3.9
|
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,develop}':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - maven
|             - sonar
|           script:
|             - mvn verify sonar:sonar -Dsonar.qualitygate.wait=true
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - maven
|             - sonar
|           script:
|             - mvn verify sonar:sonar -Dsonar.qualitygate.wait=true
|
| definitions:
|   caches:
|     sonar: ~/.sonar
| ```

[[collapse]]
| ## SonarScanner CLI
|
| **Note:** A project key has to be provided through a `sonar-project.properties` file, or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.
|
| Write the following in your `bitbucket-pipelines.yml`:
|
| ```
| clone:
|   depth: full
|
| pipelines:
|   branches:
|     '{master,develop}':
|       - step:
|           name: SonarQube analysis
|           image: sonarsource/sonar-scanner-cli:latest
|           caches:
|             - sonar
|           script:
|             - sonar-scanner -Dsonar.qualitygate.wait=true
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           image: sonarsource/sonar-scanner-cli:latest
|           caches:
|             - sonar
|           script:
|             - sonar-scanner -Dsonar.qualitygate.wait=true
|
| definitions:
|   caches:
|     sonar: /opt/sonar-scanner/.sonar
| ```

#### **Failing the pipeline job when the Quality Gate fails**
In order for the Quality Gate to fail the pipeline when it is red on the SonarQube side, the scanner needs to wait for the SonarQube Quality Gate status. To enable this, set the `sonar.qualitygate.wait=true` parameter in the `bitbucket-pipelines.yml` file, just as in the examples above. If you don't want to fail your pipeline based on the Quality Gate, you can omit the `sonar.qualitygate.wait` parameter.

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

### For more information
For more information on configuring your build with Bitbucket Pipelines, see the [Configure bitbucket-pipelines.yml](https://support.atlassian.com/bitbucket-cloud/docs/configure-bitbucket-pipelinesyml/) documentation provided by Atlassian.

## Adding Pull Request decoration to Bitbucket cloud

Pull request decoration shows your Quality Gate and analysis metrics directly in Bitbucket Cloud. To set up pull request decoration, you need to do the following:

1. Set up a dedicated OAuth consumer to decorate your pull requests.
1. Set your global **ALM Integration** settings.
1. Set your project-level **Pull Request Decoration** settings.

[[info]]
| To decorate Pull Requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for Pull Request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

### Setting up your OAuth consumer
SonarQube uses a dedicated OAuth consumer to decorate pull requests. You need to create the OAuth consumer in your Bitbucket Cloud workspace settings and specify the following:

- **Name** – the name of your OAuth consumer
- **Callback URL** – Bitbucket Cloud requires this field, but it's not used by SonarQube so you can use any URL.
- **This is a private consumer** – Your OAuth consumer needs to be private. Make sure this check box is selected.
- **Permissions** – Grant **Read** access for the **Pull requests** permission.

### Setting your global ALM Integration settings
To set your global ALM Integration settings, navigate to **Administration > ALM Integrations**, select the **Bitbucket** tab, and select **Bitbucket Cloud** as the variant you want to configure. From here, specify the following settings:

- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your GitHub configuration at the project level. Use something succinct and easily recognizable.
- **Workspace ID** – The workspace ID is part of your bitbucket cloud URL `https://bitbucket.org/{WORKSPACE-ID}/{repository-slug}`
- **OAuth Key** – Bitbucket automatically creates an OAuth key when you create your OAuth consumer. You can find it in your Bitbucket Cloud workspace settings under **OAuth consumers**.
- **OAuth Secret** – Bitbucket automatically creates an OAuth secret when you create your OAuth consumer. You can find it in your Bitbucket Cloud workspace settings under **OAuth consumers**.

### Setting your project-level Pull Request Decoration settings
From your project **Overview**, navigate to **Project Settings > General Settings > Pull Request Decoration**.

From here, set your:

- **Configuration name** – The configuration name that corresponds to your Bitbucket Cloud instance.
- **Repository SLUG** – The repository SLUG is part of your bitbucket cloud URL `https://bitbucket.org/{workspace-id}/{REPOSITORY-SLUG}`
