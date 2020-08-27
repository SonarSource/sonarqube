---
title: GitLab CI/CD
url: /analysis/gitlab-cicd/
---
_Merge Request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

SonarScanners running in GitLab CI/CD jobs can automatically detect branches or Merge Requests being built so you don't need to specifically pass them as parameters to the scanner.

[[warning]]
| You need to disable git shallow clone to make sure the scanner has access to all of your history when running analysis with GitLab CI/CD. For more information, see [Git shallow clone](https://docs.gitlab.com/ee/user/project/pipelines/settings.html#git-shallow-clone).

## Activating builds  
Set up your build according to your SonarQube edition as described below.

### Community Edition
Because Community Edition doesn't support multiple branches, you should only analyze your main branch. You can restrict analysis to your main branch by adding the branch name to the `only` parameter.

### Developer Edition and above
By default, GitLab will build all branches but not Merge Requests. To build Merge Requests, you need to update the `.gitlab-ci.yml` file by adding `merge_requests` to the `only` parameter. See the example configurations below for more information.

## Analyzing your repository

### Setting environment variables 
You can set environment variables securely for all pipelines in GitLab's settings. See [Creating a Custom Environment Variable](https://docs.gitlab.com/ee/ci/variables/#creating-a-custom-environment-variable) for more information.
 
You need to set the following environment variables in GitLab for analysis:
 
- `SONAR_TOKEN` – Generate a SonarQube [token](/user-guide/user-token/) for GitLab and create a custom environment variable in GitLab with `SONAR_TOKEN` as the **Key** and the token you generated as the **Value**. 

- `SONAR_HOST_URL` – Create a custom environment variable with `SONAR_HOST_URL` as the **Key** and your SonarQube server URL as the **Value**.

### Configuring your `gitlab-ci.yml` file
The following examples show you how to configure your GitLab CI/CD `gitlab-ci.yml` file. 

In the following examples, the `allow_failure` parameter allows a job to fail without impacting the rest of the CI suite.

Click your scanner below to expand the example configuration:

[[collapse]]
| ## SonarScanner for Gradle
| ```
| sonarqube-check:
|   image: gradle:jre11-slim
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script: gradle sonarqube -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```
 
[[collapse]]
| ## SonarScanner for Maven
| 
| ```
| sonarqube-check:
|   image: maven:3.6.3-jdk-11
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script:
|     - mvn verify sonar:sonar -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```

[[collapse]]
| ## SonarScanner CLI
| 
| ```
| sonarqube-check:
|   image:
|     name: sonarsource/sonar-scanner-cli:latest
|     entrypoint: [""]
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script:
|     - sonar-scanner -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```
|
| **Note:** A project key has to be provided through `sonar-project.properties` or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.

### Failing the pipeline job when the SonarQube Quality Gate fails
In order for the Quality Gate to fail on the GitLab side when the Quality Gate fails on the SonarQube side, the scanner needs to wait for the SonarQube Quality Gate status. To enable this, set the `sonar.qualitygate.wait=true` parameter in the `.gitlab-ci.yml` file. 

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

See the example configurations below for more information.

## For more information
For more information on configuring your build with GitLab CI/CD, see the [GitLab CI/CD Pipeline Configuration Reference](https://gitlab.com/help/ci/yaml/README.md).
