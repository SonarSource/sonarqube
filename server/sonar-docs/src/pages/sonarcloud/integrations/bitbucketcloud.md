---
title: Bitbucket Cloud
url: /integrations/bitbucketcloud/
---

You can connect to SonarCloud using your Bitbucket Cloud account. On the [login page](/#sonarcloud#/sessions/new), just click on the "Log in with Bitbucket" button.

## Install SonarCloud add-on for Bitbucket Cloud

Our Bitbucket Cloud application allows users to automate the SonarCloud analysis with Pipelines. It also allows users to view their SonarCloud metrics directly on Bitbucket Cloud via our Code Quality widget and the decoration of pull-requests.

In Bitbucket Cloud, go to your team's "Settings > Find integrations" page, search for "SonarCloud" in the "Code Quality" category and click on "Add" to install the application.

## Analyzing with Pipelines

SonarCloud integrates with Bitbucket Pipelines to make it easier to trigger analyses. Follow these steps:

1.  On SonarCloud, once your project is created, follow the tutorial on the dashboard of the project. Copy-paste the command line displayed at the end but without the `sonar.login` setting.

2.  On Bitbucket Cloud, go to the "Settings > Pipelines > Account variables" page of your team, and add a new SONAR_TOKEN variable that contains the value of the SonarCloud token which you created during the tutorial (something like `9ad01c85336b265406fa6554a9a681a4b281135f`). **Make sure that you click on the "Lock" icon to encrypt and hide this token.**

3.  Inside the `bitbucket-pipelines.yml` file of your repository paste the command you copied in step 1. For example, for a Java Maven-based project, you should have something like:

```
script:
  -mvn sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.projectKey=my-project -Dsonar.organization=my-team-org
```

When this change on `bitbucket-pipelines.yml` is committed and pushed, Pipelines should automatically run a new build and therefore trigger the analysis of the repository. Shortly after, your project will appear on SonarCloud in your organization. 

## Analyzing pull requests with Pipelines

In order to trigger SonarCloud analysis on each pull request update, you have to supply the copied command in `pull-requests` section of `bitbucket-pipelines.yml` (check [Configure bitbucket-pipelines.yml](https://confluence.atlassian.com/bitbucket/configure-bitbucket-pipelines-yml-792298910.html#Configurebitbucket-pipelines.yml-ci_pull-requests) for more details about that section). Here is a sample configuration:
```
pipelines:
  ...
  pull-requests:
    feature/*:
      - step:
          script:
            - mvn -B verify sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.projectKey=... -Dsonar.organization=...
  ...
```

## Quality widget

SonarCloud provides a widget that shows the current quality metrics of your project directly on the repository's Overview page on Bitbucket Cloud.

If you want to hide this widget (e.g. because your repository is not analyzed on SonarCloud), you can go to the "Settings > SonarCloud" page of your repository and check "Hide repository overview widget".

## FAQ

**Do you have a sample project on Bitbucket Cloud?**
For the time being, you can take a look at this very simple JS project: [Sample project analysed on SonarCloud](https://bitbucket.org/bellingard/fab)

**Pipelines can't find sonar-scanner**
If you want to analyze a non-Java project (JS, TS, PHP, Python, Go, ...), you will need to download and install the [Scanner CLI](https://redirect.sonarsource.com/doc/install-configure-scanner.html) during the execution of your build prior to the actual code scan. You have two options:

* You can download it (with curl for instance) from the links available on the documentation page and unpack it (preferably in a cached folder for later reuse).
* On Node environments, you can rely on a [community NPM module](https://www.npmjs.com/package/sonarqube-scanner) to install it globally and therefore make it available in the PATH.

**I don't see the any quality information whereas I configured everything**
Make sure that your browser is not using some extensions like AdBlocks. They tend to break the integration of third-party applications in BitBucket Cloud.

## Upcoming features and improvements

There are various areas in which you can expect new features and improvements:

* Tighter integration with Pipelines (less parameters to pass on the CLI, availability of the scanner, ...)
* Pull request decoration with inline comments to show the issues within the PR
* Better and easier team onboarding
* Automatic analysis (i.e. no need to configure anything from Pipelines)
