---
title: Running Analysis with GitLab CI/CD
url: /analysis/gitlab-cicd/
---

_Merge Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in GitLab CI/CD Jobs will automatically detect branches or Merge Requests being built so you don't need to specifically pass them as parameters to the scanner.

[[warning]]
| You need to disable shallow clone to make sure the scanner has access to all of the branches when running analysis with GitLab CI/CD. See [Git shallow clone](https://docs.gitlab.com/ee/user/project/pipelines/settings.html#git-shallow-clone) for more on disabling shallow clone.

## Activating builds  
Set up your build according to the section below that corresponds to your SonarQube edition.

### Developer Edition and above
By default, GitLab will build all branches but not Merge Requests. To build Merge Requests, you need to update the `.gitlab-ci.yml` file by adding `merge_requests` to the `only` parameter. See the **Example configurations** below for more information.

### Community Edition
Because Community Edition doesn't support branches, you should only analyze your main branch. You can restrict analysis to your main branch by adding the branch name to the `only` parameter.

## Failing the pipeline job when the SonarQube Quality Gate fails
In order for the Quality Gate to fail on the GitLab side when the Quality Gate fails on the SonarQube side, the scanner needs to wait for the SonarQube Quality Gate status. To enable this, set the `sonar.qualitygate.wait=true` parameter in the `.gitlab-ci.yml` file. 

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

See the **Example configurations** below for more information.

## Example configurations
The following example configurations show you how to configure the execution of SonarScanner for Gradle, SonarScanner for Maven, and SonarScanner CLI with GitLab CI/CD.

In the example configurations:

The `allow_failure` parameter allows a job to fail without impacting the rest of the CI suite.

The `SONAR_TOKEN` and `SONAR_HOST_URL` variables are included. If you don't have environment variables set for all builds in GitLab's settings (as shown in **Setting environment variables for all builds** below), you need to set the variables to pass a [token](/user-guide/user-token/) and the URL of your SonarQube server.

### SonarScanner for Gradle:

```
image: gradle:alpine
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-instance.org"
  GIT_DEPTH: 0
sonarqube-check:
  stage: test
  script: gradle sonarqube -Dsonar.qualitygate.wait=true
  allow_failure: true
  only:
    - merge_requests
    - master
```
 
### SonarScanner for Maven:
 
```
image: maven:latest
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-url"
  GIT_DEPTH: 0
sonarqube-check:
  script:
    - mvn verify sonar:sonar -Dsonar.qualitygate.wait=true
  allow_failure: true
  only:
    - merge_requests
    - master
```

### SonarScanner CLI:

```
image:
  name: sonarsource/sonar-scanner-cli:latest
  entrypoint: [""]
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-instance.org"
  GIT_DEPTH: 0
sonarqube-check:
  stage: test
  script:
    - sonar-scanner -Dsonar.qualitygate.wait=true
  allow_failure: true
  only:
    - merge_requests
    - master
```  

**Note:** A project key has to be provided through `sonar-project.properties` or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.

## Setting environment variables for all builds  
Instead of specifying environment variables in your `.gitlab-ci.yml` file (such as `SONAR_TOKEN` and `SONAR_HOST_URL`), you can set them securely for all pipelines in GitLab's settings. See [Creating a Custom Environment Variable](https://docs.gitlab.com/ee/ci/variables/#creating-a-custom-environment-variable) for more information.

## For more information
For more information on configuring your build with GitLab CI/CD, see the [GitLab CI/CD Pipeline Configuration Reference](https://gitlab.com/help/ci/yaml/README.md).
