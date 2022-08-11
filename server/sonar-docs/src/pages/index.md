---
title: SonarQube Documentation
url: /
---

[SonarQube](http://www.sonarqube.org/), is a self-managed, automatic code review tool that systematically helps you deliver Clean Code. As a core element of our [Sonar solution](https://www.sonarsource.com/), SonarQube integrates into your existing workflow and detects issues in your code to help you perform continuous code inspections of your projects. The tool analyses [30+ different programming languages](https://rules.sonarsource.com/) and integrates into your [CI pipeline](/analysis/ci-integration-overview/) and [DevOps platform](/analysis/github-integration/) to ensure that your code meets high-quality standards.


## Writing Clean Code

Writing Clean Code is essential to maintaining a healthy codebase. We define Clean Code as code that meets a certain defined standard, i.e. code that is reliable, secure, maintainable, readable, and modular, in addition to having other key attributes. This applies to all code: source code, test code, Infrastructure as Code, glue code, scripts, etc.

Sonar's [Clean as You Code](/user-guide/clean-as-you-code/) approach eliminates many of the pitfalls that arise from reviewing code at a late stage in the development process. The Clean as You Code approach uses your [Quality Gate](/user-guide/quality-gates/) to alert/inform you when there’s something to fix or review in your [New Code (code that has been added or changed](/project-administration/new-code-period/)), allowing you to maintain high standards and focus on code quality.


## Developing with Sonar

![Development Cycle](/images/dev-cycle.png)

The Sonar solution performs checks at every stage of the development process:

* [SonarLint](https://www.sonarlint.org/) provides immediate feedback in your IDE as you write code so you can find and fix issues before a commit.
* SonarQube’s [PR analysis](/analysis/pull-request/) fits into your CI/CD workflows with SonarQube’s PR analysis & use of Quality Gates.
* [Quality Gates](/user-guide/quality-gates/) keep code with issues from being released to production, a key tool in helping you incorporate the Clean as You Code methodology.
* The [Clean as You Code](/user-guide/clean-as-you-code/) approach helps you focus on submitting new, Clean Code for production, knowing that your existing code will be improved over time.

Learn more about the [types of issues](/user-guide/issues/) that SonarQube detects.

Organizations start off with a default set of rules and metrics called the [Sonar Way Quality Profile](/instance-administration/quality-profiles/). This can be customized per project to satisfy different technical requirements. Issues raised in the analysis are compared against the conditions defined in the Quality Profile to establish your Quality Gate.

A [Quality Gate](/user-guide/quality-gates/) is an indicator of code quality that can be configured to give a go/no-go signal on the current release-worthiness of the code. It indicates whether your code is clean and can move forward.

* A passing (green) Quality Gate means the code meets your standard and is ready to be merged.
* A failing (red) Quality Gate means there are issues to address.

SonarQube provides feedback through its UI, email, and in decorations on pull or merge requests (in commercial editions) to notify your team that there are issues to address. Feedback can also be obtained in SonarLint supported IDEs when running in [Connected Mode](/user-guide/connected-mode/). SonarQube also provides in-depth guidance on the issues telling you why each issue is a problem and how to fix it, adding a valuable layer of education for developers of all experience levels. Developers can then address issues effectively, so code is only promoted when the code is clean and passes the Quality Gate.


## Getting Started

Now that you've heard about how [SonarQube](https://www.sonarqube.org/) can help you write Clean Code, you are ready to [try out SonarQube](/setup/get-started-2-minutes/) for yourself. You can run a local non-production instance of SonarQube and initial project analysis. Installing a local instance gets you up and running quickly, so you can experience SonarQube firsthand. Then, when you're ready to set up SonarQube in production, you'll need to [Install the Server](/setup/install-server/) before configuring your first code analysis.

The [Analyzing Source Code](https://docs.sonarqube.org/latest/analysis/overview/) section explains how to set up all flavors of analysis, including how to analyze your project’s branches and pull requests.


## More Getting Started Resources

* [How to set up and upgrade](/setup/overview/)
* [How to administer a project](/project-administration/project-existence/)
* [How to administer an instance](/instance-administration/quality-profiles/)
* [How to set up portfolios](/project-administration/managing-portfolios/)


## Learn More

Check out the entire suite of Sonar Products: [SonarQube](https://www.sonarsource.com/products/sonarqube/), [SonarCloud](https://www.sonarsource.com/products/sonarcloud/), and [SonarLint](https://www.sonarsource.com/products/sonarlint/).


## Staying Connected

Use the following links to get help and support:

* [Get help in the community](https://www.sonarqube.org/community/)
* [Source code](https://github.com/SonarSource)
* [Issue tracker](https://jira.sonarsource.com/)
