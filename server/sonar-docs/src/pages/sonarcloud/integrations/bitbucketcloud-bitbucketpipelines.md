---
title: Analyze your repository with Bitbucket Pipelines
nav: With Bitbucket Pipelines
url: /integrations/bitbucketcloud/bitbucket-pipelines/
---

## Analyzing branches

Once your project is created and initiated from the repository you selected:

1. Generate a token to allow to publish analysis from Bitbucket Pipelines. To generate a token, follow the first step of the tutorial on the dashboard of the project, or go to your user security page.

2. On Bitbucket Cloud, go to the "Settings > Pipelines > Account variables" page of your team, and add a new SONAR_TOKEN variable that contains the value of the SonarCloud token (something like `9ad01c85336b265406fa6554a9a681a4b281135f`).
   * **Make sure that you click on the "Lock" icon to encrypt and hide this token.**

3. Edit the `bitbucket-pipelines.yml` file of your repository to trigger the SonarCloud analysis.

Once those changes are pushed, Pipelines will automatically trigger analyses on the repository.

You can see our multiple sample projects to see how it is working :

  * [Built with Gradle](https://bitbucket.org/sonarsource/sample-gradle-project)
  * [Built with Maven](https://bitbucket.org/sonarsource/sample-maven-project)
  * [Javascript project](https://bitbucket.org/sonarsource/sample-nodejs-project)

If you target a .NET application, see a [sample .NET project](https://bitbucket.org/sonarsource/sample-dotnet-project-azuredevops) built with Azure Pipelines

## Analyzing pull requests

In order to trigger SonarCloud analysis on each pull request update, you have to supply the same command in the `pull-requests` section of `bitbucket-pipelines.yml` (check [Configure bitbucket-pipelines.yml](https://confluence.atlassian.com/bitbucket/configure-bitbucket-pipelines-yml-792298910.html#Configurebitbucket-pipelines.yml-ci_pull-requests) for more details about that section). Here is a sample configuration:
```
pipelines:
  ...
  pull-requests:
    feature/*:
      - step:
          script:
            - mvn sonar:sonar
  ...
```

In order to avoid duplication between the different sections of your `bitbucket-pipelines.yml`, you can use [yaml anchors and aliases](https://confluence.atlassian.com/bitbucket/yaml-anchors-960154027.html).
