---
title: Overview
url: /analysis/branch-pr-analysis-overview/
---

_Merge and Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in GitLab CI/CD, Azure Pipelines, Cirrus CI, and Jenkins with a Branch Source plugin configured can automatically detect branches and merge or pull requests by using environment variables set in the jobs.

[[warning]]
| Automatic configuration is disabled if any branch or pull request properties have been set manually.

## Failing a pipeline job when the Quality Gate fails
If you're using Jenkins or Azure Pipelines, there are specific ways to fail the pipeline when your Quality Gate is failing for your CI tool. For more information, see the corresponding section below.

For other CIs, you can use the `sonar.qualitygate.wait=true` analysis parameter in your configuration file. Using the `sonar.qualitygate.wait` parameter forces the analysis step to poll the SonarQube instance and wait for the Quality Gate status. This increases the pipeline duration and causes the analysis step to fail any time the Quality Gate is failing, even if the actual analysis is successful. You should only use this parameter if it's necessary.

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds.

## GitLab CI/CD
For GitLab CI/CD configuration, see the [GitLab ALM integration](/analysis/gitlab-integration/) page.

## Azure Pipelines
For Azure Pipelines configuration, see the [Azure DevOps integration](/analysis/azuredevops-integration/) page.

## Bitbucket Pipelines
For Bitbucket Pipelines configuration, see the [Bitbucket Cloud integration](/analysis/bitbucket-cloud-integration/) page.

## Jenkins
For Jenkins configuration, see [Jenkins](/analysis/jenkins/).
