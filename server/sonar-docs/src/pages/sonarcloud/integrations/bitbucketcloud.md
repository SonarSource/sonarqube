---
title: Bitbucket Cloud
url: /integrations/bitbucketcloud/
---

You can connect to SonarCloud using your Bitbucket Cloud account. On the [login page](/#sonarcloud#/sessions/new), just click on the "Log in with Bitbucket" button.

## Install SonarCloud add-on for Bitbucket Cloud

Our Bitbucket Cloud application allows users to automate the SonarCloud analysis with Pipelines. It also allows users to view their SonarCloud metrics directly on Bitbucket Cloud via our Code Quality widget and the decoration of pull-requests.

In Bitbucket Cloud, go to your team's "Settings > Find integrations" page, search for "SonarCloud" in the "Code Quality" category and click on "Add" to install the application.

## Analyzing with Pipelines

1.  On SonarCloud, once your project is created, generate a token to allow to publish analysis from Bitbucket Pipelines. To generate a token, follow the first step of the tutorial on the dashboard of the project, or go to your user security page.

2.  On Bitbucket Cloud, go to the "Settings > Pipelines > Account variables" page of your team, and add a new SONAR_TOKEN variable that contains the value of the SonarCloud token (something like `9ad01c85336b265406fa6554a9a681a4b281135f`). **Make sure that you click on the "Lock" icon to encrypt and hide this token.**

3.  Edit the `bitbucket-pipelines.yml` file of your repository to trigger the SonarCloud analysis. If you have a Maven or Gradle project, the simplest way is to use our dedicated plugins. For most other projects, you need our command line scanner to trigger analyses. See [various example projects](https://bitbucket.org/account/user/sonarsource/projects/SAMPLES).

When a change on `bitbucket-pipelines.yml` is committed and pushed, Pipelines should automatically run a new build and therefore trigger the analysis of the repository. Shortly after, your project will appear on SonarCloud in your organization.

## Analyzing pull requests with Pipelines

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

## Quality widget

SonarCloud can provide a widget that shows the current quality metrics of your project directly on the repository's Overview page on Bitbucket Cloud.

If you want to see this widget, you can go to the "Settings > SonarCloud" page of your repository and check "Show repository overview widget".

## FAQ

**Do you have sample projects on Bitbucket Cloud?**
You can take a look at these various projects: [Sample projects analysed on SonarCloud](https://bitbucket.org/account/user/sonarsource/projects/SAMPLES)

**I don't see the widget with quality information whereas I configured everything**
Make sure that your browser is not using some extensions like AdBlocks. They tend to break the integration of third-party applications in BitBucket Cloud.

## Upcoming features and improvements

There are various areas in which you can expect new features and improvements:

* Pull request decoration with inline comments to show the issues within the PR
* Better and easier team onboarding
* Automatic analysis (i.e. no need to configure anything from Pipelines)
