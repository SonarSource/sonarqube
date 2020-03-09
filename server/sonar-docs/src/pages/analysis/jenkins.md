---
title: Jenkins
url: /analysis/jenkins/
---

_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in Jenkins can automatically detect branches and Merge or Pull Requests in certain jobs.

## Configuring Single Branch Pipeline jobs
With Community Edition, you can only analyze a single branch. For more information, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

## Configuring Multibranch Pipeline jobs  
With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), you can analyze multiple branches and Pull Requests. Automatic Configuration relies on environment variables available in Multibranch Pipeline jobs. These are set based on information exported by Jenkins plugins. Depending on your ALM provided, you'll need the BitBucket, GitHub, or GitLab Branch Source plugin. This feature is enabled by default with any SonarScanner, even if the SonarScanner for Jenkins is not used. 

The following example shows how to configure your Multibranch Pipeline with SonarScanner for Jenkins. 

```
pipeline {
  agent none
  stages {
    stage("build & SonarQube analysis") {
      agent any
      steps {
        withSonarQubeEnv('your-sq-instance') {
          sh 'mvn clean package sonar:sonar'
        }
      }
    }
  }
}
```

For more examples, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

## Detecting changed code in Pull Requests
SonarScanners need access to a Pull Request's target branch to detect code changes in the Pull Request. If you're using a Jenkins Pull Request discovery strategy that only fetches the Pull Request and doesn't merge with the target branch, the target branch is not fetched and is not available in the local git clone for the scanner to read. 

In this case, the code highlighted as “new” in the Pull Request may be inaccurate, and you’ll see the following warning in the scanner’s log:

```
File '[name]' was detected as changed but without having changed lines
```

To fix this, either change the discovery strategy or manually fetch the target branch before running the SonarScanner. For example:

```
git fetch +refs/heads/${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}
```