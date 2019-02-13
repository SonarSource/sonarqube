---
title: Get started with GitHub
nav: GitHub
url: /integrations/github/
---

## Sign up and set up your first project

1. On the [login page](/#sonarcloud#/sessions/new), click on the "Log in with GitHub" button and connect to SonarCloud using your GitHub account.
2. Click on "Analyze your code" and follow the path to set up a first project
3. You will be asked to install the SonarCould application on your organization, which will allow you to choose which
   repository you want to analyze.

## Trigger analyses

For GitHub repositories, there are 2 ways to have your code analyzed:

### ... with AutoScan

With AutoScan, SonarCloud will autonomously pull your code and scan your default branch and your pull requests.
Please read the ["AutoScan Beta Feature"](/autoscan/) documentation page to get the details.

![](/images/exclamation.svg) This is currently a Beta feature which does not work for all languages and comes with limitations. 

### ... using your CI service

If AutoScan does not make sense yet for your repository, you need to configure your CI service to trigger the analysis.

**If you are using Travis CI**, the SonarCloud Travis Add-on will make it easier to activate analyses:

* Read the [guide to integrate with Travis CI](https://docs.travis-ci.com/user/sonarcloud/)
* Check out the [various sample projects](https://github.com/SonarSource/sonarcloud_examples) (Java, TypeScript, C/C++, Go, ... etc) that are analyzed on SonarCloud on a frequent basis

**If you are using another CI service**, you will need to read:

* the ["Analyzing Source Code" overview page](/analysis/overview/)
* the ["Branches" overview page](/branches/overview/)
* the ["Pull Request Analysis" page](/analysis/pull-request/)

Here is an example of configuration for pull requests when you are not on Travis CI and you need to configure your CI jobs:
```
sonar.pullrequest.base=master
sonar.pullrequest.branch=feature/my-new-feature
sonar.pullrequest.key=5
sonar.pullrequest.provider=GitHub
sonar.pullrequest.github.repository=my-company/my-repo
```

[[info]]
| Pull request decoration works only if [the SonarCloud application](https://github.com/apps/sonarcloud) is installed on your GitHub organization(s) and configured to have acccess to the repositories.
