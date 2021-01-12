---
title: Bitbucket Cloud Integration
url: /analysis/bitbucket-cloud-integration/
---

SonarQube's integration with Bitbucket Cloud allows you to maintain code quality and security in your Bitbucket Cloud repositories.

With this integration, you'll be able to:

- **Analyze projects with Bitbucket Pipelines** - Integrate analysis into your build pipeline. SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner (branch and pull request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)).

## Analyzing projects with Bitbucket Pipelines
SonarScanners running in Bitbucket Pipelines can automatically detect branches or pull requests being built so you don't need to specifically pass them as parameters to the scanner.

### Activating builds  
Set up your build according to your SonarQube edition:

- **Community Edition** – Community Edition doesn't support multiple branches, so you should only analyze your main branch. You can restrict analysis to your main branch by using the `branches.master` pipeline in your `bitbucket-pipelines.yml` file and not using the `pull-requests` pipeline.
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
|             - bash ./gradlew sonarqube
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - gradle
|             - sonar
|           script:
|             - bash ./gradlew sonarqube
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
|             - mvn verify sonar:sonar
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           caches:
|             - maven
|             - sonar
|           script:
|             - mvn verify sonar:sonar
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
|             - sonar-scanner
|
|   pull-requests:
|     '**':
|       - step:
|           name: SonarQube analysis
|           image: sonarsource/sonar-scanner-cli:latest
|           caches:
|             - sonar
|           script:
|             - sonar-scanner
|
| definitions:
|   caches:
|     sonar: /opt/sonar-scanner/.sonar
| ```

### For more information
For more information on configuring your build with Bitbucket Pipelines, see the [Configure bitbucket-pipelines.yml](https://support.atlassian.com/bitbucket-cloud/docs/configure-bitbucket-pipelinesyml/) documentation provided by Atlassian.
