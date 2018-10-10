---
title: GitHub
url: /integrations/github/
---

You can connect to SonarCloud using your GitHub account. On the [login page](/#sonarcloud#/sessions/new), just click on the "Log in with GitHub" button.

## Trigger analyses

SonarCloud currently does not trigger analyses automatically. It's up to you to launch them inside your
existing CI scripts. Please follow the [tutorial](/#sonarcloud#/onboarding) to get started.

### Using Travis CI?

If you are using Travis CI, the SonarCloud Travis Add-on will make it easier to activate analyses: 
* Read the [guide to integrate with Travis CI](https://docs.travis-ci.com/user/sonarcloud/)
* Check out the [various sample projects](https://github.com/SonarSource/sonarcloud_examples) (Java, TypeScript, C/C++, Go, ... etc) that are analyzed on SonarCloud on a frequent basis

## Activating pull request decoration

To have your pull requests decorated by SonarCloud in GitHub, you need to [install the SonarCloud application](https://github.com/apps/sonarcloud) on your GitHub organization(s).

Once installed, there is nothing more to do if you are using the Travis Add-on. In any other case, you will need
to pass the following properties in your script during the analysis:

```
sonar.pullrequest.base=master
sonar.pullrequest.branch=feature/my-new-feature
sonar.pullrequest.key=5
sonar.pullrequest.provider=GitHub
sonar.pullrequest.github.repository=my-company/my-repo
```
