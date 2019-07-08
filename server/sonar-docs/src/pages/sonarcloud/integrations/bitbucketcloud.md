---
title: Get started with Bitbucket Cloud
nav: Bitbucket Cloud
url: /integrations/bitbucketcloud/
---

## Sign up and set up your first project
1. On the [login page](/#sonarcloud#/sessions/new), click on the "Log in with Bitbucket" button and connect to SonarCloud using your Bitbucket Cloud account.
2. Click on "Analyze your code" and follow the path to set up a first project
3. You will be asked to install the SonarCould application on your team or user account, which will allow you to 
  choose which repositories you want to analyze.

## Analyzing your repository

* [With Bitbucket Pipelines](/integrations/bitbucketcloud/bitbucket-pipelines/)
* [With Azure Pipelines](/integrations/bitbucketcloud/azure-pipelines/), if you analyze .NET applications and want to benefit from Azure DevOps features.

## Quality widget

SonarCloud can provide a widget that shows the current quality metrics of your project directly on the repository's Overview page on Bitbucket Cloud.

If you want to see this widget, you can go to the "Settings > SonarCloud" page of your repository and check "Show repository overview widget".

## FAQ

**Do you have sample projects on Bitbucket Cloud?**
You can take a look at these various projects: [Sample projects analyzed on SonarCloud](https://bitbucket.org/account/user/sonarsource/projects/SAMPLES)

**I don't see the widget with quality information whereas I configured everything**
Make sure that your browser is not using some extensions like AdBlocks. They tend to break the integration of third-party applications in BitBucket Cloud.

## Upcoming features and improvements

There are various areas in which you can expect new features and improvements:

* Pull request decoration with inline comments to show the issues within the PR
* Better and easier team onboarding
* Automatic analysis (i.e. no need to configure anything from Pipelines)
