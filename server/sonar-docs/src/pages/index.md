---
title: SonarQube Documentation
url: /
---

Welcome to the SonarQube documentation! 

[SonarQube](https://www.sonarqube.org/)® is an automatic code review tool to detect bugs, vulnerabilities, and code smells in your code. It can integrate with your existing workflow to enable continuous code inspection across your project branches and pull requests.

If you want to try out SonarQube, check out the [Try out SonarQube](/setup/get-started-2-minutes/) page for instructions on installing a local instance and analyzing a project.

If you're ready to set up a production instance, check out the [Install the Server](/setup/install-server/) documentation.

Otherwise, you can also find an overview and common scenarios below or navigate through and search the full documentation in the left pane.

## Overview

![SonarQube Instance Components](/images/dev-cycle.png)

In a typical development process:  

1. Developers develop and merge code in an IDE (preferably using [SonarLint](https://www.sonarlint.org/) to receive immediate feedback in the editor) and check-in their code to their DevOps Platform.
1. An organization’s continuous integration (CI) tool checks out, builds, and runs unit tests, and an integrated SonarQube scanner analyzes the results.
1. The scanner posts the results to the SonarQube server which provides feedback to developers through the SonarQube interface, email, in-IDE notifications (through SonarLint), and decoration on pull or merge requests (when using [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and above).

## Installing, monitoring, and upgrading

See the [installing](/setup/install-server/) and [upgrading](/setup/upgrading/) pages for setting up your production instance.

When your instance is up and running, see the [Monitoring](/instance-administration/monitoring/) documentation for information on keeping your instance running smoothly.

If you're using SonarQube Data Center Edition, see the [Configure & Operate a Cluster](/setup/operate-cluster/) documentation for more information on running your instance as a cluster.

## Setting up analysis

Analyzing your code starts with installing and configuring a SonarQube scanner. The scanner can either run on your build or as part of your continuous integration (CI) pipeline performing a scan whenever your build process is triggered. For more information, see [Analyzing Source Code](/analysis/overview/). 

### Analyzing branches

Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze your branches in SonarQube, and ensure that your code quality is consistent all the way down to the branch level in your projects. For more information, see [Branch Analysis](/branches/overview/).

### Analyzing pull requests

Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can integrate SonarQube to be part of your pull or merge request process. Issuing a pull request can trigger a branch analysis and add pull request decoration to see your branch analysis directly in your DevOps Platforms's interface in addition to the SonarQube interface. For more information, see the [Pull Request Analysis Overview](/analysis/pull-request/).

## Writing Clean and Safe Code

SonarQube gives you the tools you need to write clean and safe code:

- [SonarLint](https://www.sonarlint.org/) – SonarLint is a companion product that works in your editor giving immediate feedback so you can catch and fix issues before they get to the repository.
- [Quality Gate](/user-guide/quality-gates/) – The Quality Gate lets you know if your project is ready for production. 
- [Clean as You Code](/user-guide/clean-as-you-code/) – Clean as You Code is an approach to code quality that eliminates a lot of the challenges that come with traditional approaches. As a developer, you focus on maintaining high standards and taking responsibility specifically in the New Code you're working on.
- [Issues](/user-guide/issues/) – SonarQube raises issues whenever a piece of your code breaks a coding rule, whether it's an error that will break your code (bug), a point in your code open to attack (vulnerability), or a maintainability issue (code smell).
- [Security Hotspots](/user-guide/security-hotspots/) – SonarQube highlights security-sensitive pieces of code that need to be reviewed. Upon review, you'll either find there is no threat or you need to apply a fix to secure the code.
 
## Administering a Project

If you have the **Create Projects** permission (a global administrator can set permissions at **Administration > Security > Global Permissions**), you can create and administer projects. See [Project Settings](/project-administration/project-settings/) for general information on setting up projects. 

A project is automatically added on the first analysis. However, you can provision projects (set up permissions, Quality Profiles, etc.) before running the first analysis. See [Project Existence](/project-administration/project-existence/) for more information on provisioning a project and handling provisioned projects.

You also want to make sure SonarQube's results are relevant. To do this you need to [Narrowing the Focus](/project-administration/narrowing-the-focus/) or configure what to analyze for each project.

You can also set up [Webhooks](/project-administration/webhooks/) to notify external services when a project analysis is complete.

## Administering an Instance

If you're a global administrator, you can set up authentication, administrator access, and authorization. See [Security](/instance-administration/security/) for more information.

You can also set up email [notifications](/instance-administration/notifications/) that developers can subscribe to that are sent at the end of each analysis. 

When you run new analyses on your projects, some data is cleaned out of the database to save space and improve performance. See [Housekeeping](/instance-administration/housekeeping/) for information on what data is cleaned and how to change these settings.

Starting in [Enterprise Edition](https://www.sonarqube.org/enterprise-edition/), you can set up [Portfolios](/user-guide/portfolios/) to get a high-level overview on the releasability of a group of projects.  

## Staying Connected

Use the following links to get help and keep up with SonarQube:

- [Get help in the community](https://www.sonarqube.org/community/)
- [Source code](https://github.com/SonarSource)
- [Issue tracker](https://jira.sonarsource.com/)