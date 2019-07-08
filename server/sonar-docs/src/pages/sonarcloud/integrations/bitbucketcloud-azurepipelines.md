---
title: Analyze your repository with Azure Pipelines
nav: With Azure Pipelines
url: /integrations/bitbucketcloud/azure-pipelines/
---

If you are analyzing .NET applications and want to benefit from the Azure DevOps / Pipelines experience and features, you have the possibility to setup a build pipeline, targeting your Bitbucket Cloud repository, and connected to SonarCloud.

## Analyzing branches

Please be advised that the team where the Bitbucket Cloud repository is has to be bound to your SonarCloud organization in order to get this work.

1. Install the SonarCloud extension for Azure DevOps in your Azure DevOps organization : [SonarCloud extension](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarcloud). You can have a look a [this chapter](https://docs.microsoft.com/en-us/labs/devops/sonarcloudlab/index?tutorial-step=1) of the global tutorial for Azure DevOps.

2. Configure a new build pipeline (YAML or classic editor), targeting your Bitbucket Cloud repository. You will have to create a new service connection to that repository.

3. Configure the Prepare SonarCloud configuration task just the way you will do for a regular Azure Git Repository.

4. Go to the triggers tab of the pipeline configuration, click on the repository below `Continuous Integration` then click on `Enable continuous integration`, add a new branch filter with following configuration :
   * Type : Include
   * Branch specification : master

Want to see how it is working ? Have a look at our [sample .NET project](https://bitbucket.org/sonarsource/sample-dotnet-project-azuredevops)

## Analyzing pull requests

Pre-requisites :

* Follow the initiation steps of Analyzing branches with Azure pipelines above.
* Version 1.6.4+ of the Azure DevOps extension is needed.

As for branches, you can trigger an analysis for Pull requests with an Azure DevOps pipeline and get your PR decorated.

1. On the Azure pipeline that will be used, click on the `Triggers` tab, then click on the repository below `Pull request validation`

2. Click on `Enable pull request validation` then configure the proper branch filters.
