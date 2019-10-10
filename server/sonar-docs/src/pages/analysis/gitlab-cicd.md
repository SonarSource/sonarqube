---
title: Running Analysis with GitLab CI/CD
url: /analysis/gitlab-cicd/
---

_Merge Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in GitLab CI/CD Jobs will automatically detect branches or Merge Requests being built so you don't need to specifically pass them as parameters to the scanner. 

## Activating builds  
By default, GitLab will build all branches but not Merge Requests.

### Developer Edition and above
To build Merge Requests, you need to update the `.gitlab-ci.yml` file by adding `merge_requests` to the `only` parameter. See the following examples for more information.

### Community Edition
Because Community Edition doesn't support branches, you should only analyze your main branch. You can restrict analysis to that branch by adding the branch name to the `only` parameter.

## Example configurations
The following examples show you how to configure the execution of SonarScanner for Gradle, SonarScanner for Maven, and SonarScanner CLI with GitLab CI/CD.

### SonarScanner for Gradle:

```
image: gradle:alpine
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-instance.org"
sonarqube-check:
  stage: test
  script: gradle sonarqube
  only:
    - merge_requests
    - branches
```
 
### SonarScanner for Maven:
 
```
image: maven:latest
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-url"
sonarqube-scan:
  script:
    - mvn verify sonar:sonar
  only:
    - merge_requests
    - branches
```

### SonarScanner CLI:

```
image:
  name: sonarsource/sonar-scanner-cli:latest
  entrypoint: [""]
variables:
  SONAR_TOKEN: "your-sonarqube-token"
  SONAR_HOST_URL: "http://your-sonarqube-instance.org"
sonarqube-check:
  stage: test
  script:
    - sonar-scanner
  only:
    - merge_requests
    - branches
```  

**Note:** A project key has to be provided through `sonar-project.properties` or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) page.

## Setting environment variables for all builds  
Instead of specifying environment variables in your `.gitlab-ci.yml` file (such as `SONAR_TOKEN` and `SONAR_HOST_URL`), you can set them securely for all pipelines in GitLab's settings. See [Creating a Custom Environment Variable](https://docs.gitlab.com/ee/ci/variables/#creating-a-custom-environment-variable) for more information.

## For more information
For more information on configuring your build with GitLab CI/CD, see the [GitLab CI/CD Pipeline Configuration Reference](https://gitlab.com/help/ci/yaml/README.md).
