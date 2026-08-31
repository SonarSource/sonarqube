<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/a23fc7ba-23f0-489a-829d-ed88c0748521/Sonar_Logo_Dark%20Backgrounds.svg">
    <img src="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/82c13eba-d95c-4bb8-8007-7ce77c14e043/Sonar_Logo_Light%20Backgrounds.svg" alt="Sonar logo" width="400">
  </picture>
</p>

# SonarQube

<p>
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/19f97554-c5ec-4cf1-87f7-878c02a19702/SQ_Logo_Server_Dark%20Backgrounds.svg">
    <img src="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/4a785d22-7141-409d-95a2-695c42595f90/SQ_Logo_Server_Light%20Backgrounds.png" alt="SonarQube Server logo" width="400">
  </picture>
</p>

[![Build](https://github.com/SonarSource/sonarqube/actions/workflows/build.yml/badge.svg)](https://github.com/SonarSource/sonarqube/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sonarqube&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sonarqube)
[![AI Code Assurance](https://next.sonarqube.com/sonarqube/api/project_badges/ai_code_assurance?project=org.sonarsource.sonarqube%3Asonarqube-private&token=sqb_c0e2fa9ac4ef89f9a8403c6ba235e108ceb1dce1)](https://next.sonarqube.com/sonarqube/dashboard?id=sonarqube)
[![Release](https://img.shields.io/github/v/release/SonarSource/sonarqube)](https://github.com/SonarSource/sonarqube/releases)
[![Docker pulls](https://img.shields.io/docker/pulls/library/sonarqube)](https://hub.docker.com/_/sonarqube)
[![License](https://img.shields.io/badge/license-LGPL%20v3-blue)](#license)
[![Community](https://img.shields.io/badge/community-forum-blue)](https://community.sonarsource.com/c/sq/10)

SonarQube is the algorithmic verification platform for code quality and security. Its static analysis applies techniques such as symbolic execution and data and control flow analysis to inspect your code, find bugs, vulnerabilities, and structural problems, and tell you exactly what to fix and why, in your IDE, your pull requests, and your CI pipeline.

This repository holds the source of the **SonarQube Community Build**, the free, open-source edition of the SonarQube platform. It shares the same analysis used across the SonarQube product line.

Trusted by more than 7 million developers and 22,000 organizations, SonarQube analyzes over 750 billion lines of code every day.

## Built for the AI era

AI writes code faster than teams can review it, creating verification debt: code reaching production before anyone has confirmed what it does. SonarQube applies the same consistent, explainable analysis to every line, whether a developer or an agent wrote it. Vibe, then verify: generate fast, then verify what reaches production. It is the verification stage of the Agent Centric Development Cycle, running in the outer CI verification loop to verify code before it merges.

## What SonarQube finds

- **Bugs and reliability issues** that break behavior at runtime.
- **Security vulnerabilities and security hotspots**, with clear guidance on the risk and the fix.
- **Maintainability and structural issues** that make code harder to change over time.
- **Coverage on new code**, so quality improves with every commit instead of stalling behind a backlog.

It analyzes 40+ programming languages and frameworks. Analysis is repeatable, auditable, and explainable: the same code always produces the same findings, every finding is traceable, and each one tells you what the problem is and why it matters.

## Commercial editions

SonarQube Server and SonarQube Cloud include everything in the Community Build and add:

**Detect more**

- More bugs, vulnerabilities, code quality issues, and architecture issues, through broader coverage and deeper analysis.
- More languages and frameworks than the Community Build.
- Software composition analysis (SCA) for vulnerable and risky open-source dependencies.
- Advanced security with deep taint analysis (SAST) that traces vulnerabilities across data flows.
- Secrets detection for leaked credentials, tokens, and keys.
- Infrastructure as code (IaC) analysis for Terraform, Kubernetes, Docker, and CloudFormation.
- Architecture management to define architectural constraints and catch structural violations.

**Analyze your whole workflow**

- Branch analysis and pull request decoration, so every change is verified before it merges.

**Govern, report, and see across teams**

- Portfolios and applications that roll up quality and security across many projects.
- Executive dashboards and trend reporting.
- Security and compliance reports, including OWASP Top 10, CWE, and PCI DSS.
- Enterprise governance: enforce Quality Gates and permissions across teams, with full audit trails.

**Fix**

- SonarQube Remediation Agent to reduce technical debt by fixing SonarQube issues for you, opening verified fix pull requests you can review and merge.

Some capabilities are part of the SonarQube Advanced Security add-on. See the [product line](#the-sonarqube-product-line) for details.

## Quality gates

A Quality Gate is a pass-or-fail check on your new code. Set the standard once, and SonarQube enforces it automatically in every pull request and pipeline, so issues are caught before they merge rather than found in production.

## The SonarQube product line

The Community Build is free and open source. The wider SonarQube line applies the same analysis across your workflow:

- **[SonarQube Server](https://www.sonarsource.com/products/sonarqube/server/)**, self-managed, with more languages, deeper security analysis, and branch and pull request analysis.
- **[SonarQube Cloud](https://www.sonarsource.com/products/sonarqube/cloud/)**, hosted, with the same capabilities as a managed service.
- **[SonarQube for IDE](https://www.sonarsource.com/products/sonarqube/ide/)**, real-time analysis as you write or generate code.
- **[SonarQube MCP Server](https://github.com/SonarSource/sonarqube-mcp-server)**, bringing SonarQube analysis into your AI agent's context.
- **[SonarQube CLI](https://docs.sonarsource.com/sonarqube-cli)**, running analysis from the command line.

See the [product page](https://www.sonarsource.com/products/sonarqube/server/) for a full comparison.

## Links

- [Website](https://www.sonarsource.com/products/sonarqube)
- [Download](https://www.sonarsource.com/products/sonarqube/downloads)
- [Documentation](https://docs.sonarsource.com/sonarqube)
- [Webapp source code](https://github.com/SonarSource/sonarqube-webapp)
- [X](https://twitter.com/SonarQube)
- [SonarSource](https://www.sonarsource.com), author of SonarQube
- [Issue tracking](https://jira.sonarsource.com/browse/SONAR/), read-only. Only SonarSourcers can create tickets.
- [Responsible Disclosure](https://community.sonarsource.com/t/responsible-vulnerability-disclosure/9317)
- [Next](https://next.sonarqube.com/sonarqube) instance of the next SonarQube version

## FAQ

**Is the Community Build free?** Yes. It is free and open source, and this repository is its source.

**What is the difference between the Community Build and SonarQube Server?** The Community Build is the free, self-managed engine. SonarQube Server and SonarQube Cloud are the commercial editions. They cover more languages and add branch and pull request analysis, software composition analysis (SCA), architecture management, advanced security with taint analysis (SAST), secrets detection, infrastructure as code (IaC) analysis, the SonarQube Remediation Agent for verified fix pull requests, and enterprise reporting. Some capabilities are part of the SonarQube Advanced Security add-on. See the [product line](#the-sonarqube-product-line) for details.

**Does it work on AI-generated code?** Yes. SonarQube applies the same analysis to all code, whether a developer or an agent wrote it.

## Have Questions or Feedback?

For support questions ("How do I?", "I got this error, why?", ...), please first read the [documentation](https://docs.sonarsource.com/sonarqube) and then head to the [SonarSource Community](https://community.sonarsource.com/c/help/sq/10). The answer to your question has likely already been answered! 🤓

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. 😄

## Contributing

If you would like to see a new feature or report a bug, please create a new thread in our [forum](https://community.sonarsource.com/c/sq/10).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing (Travis build is executed for each pull request).

Willing to contribute to SonarSource products? We are looking for smart, passionate, and skilled people to help us build world-class code-quality solutions. Have a look at our current [job offers here](https://www.sonarsource.com/company/jobs/)!

## Building

To build sources locally follow these instructions.

### Requirements

- Java 17 - Required to build the project
- Native Git - Must be installed and available in your PATH
- Tests - Can be disabled if needed by adding `-x test` to the gradle command (useful if you just want to build without running tests)

### Build and Run Unit Tests

Execute from the project base directory:

    ./gradlew build

The zip distribution file is generated in `sonar-application/build/distributions/`. Unzip it and start the server by executing:

    # on Linux
    bin/linux-x86-64/sonar.sh start
    # or on MacOS
    bin/macosx-universal-64/sonar.sh start
    # or on Windows
    bin\windows-x86-64\StartSonar.bat

### Open in IDE

If the project has never been built, then build it as usual (see previous section) or use the quicker command:

    ./gradlew ide

Then open the root file `build.gradle` as a project in IntelliJ or Eclipse.

### Gradle Hints

| ./gradlew command                | Description                               |
| -------------------------------- | ----------------------------------------- |
| `dependencies`                   | list dependencies                         |
| `spotlessApply`                  | fix source headers by applying HEADER     |
| `wrapper --gradle-version 5.2.1` | upgrade wrapper                           |

## Building with UI changes

The SonarQube UI (or webapp as we call it), is located in another repository: [sonarqube-webapp](https://github.com/SonarSource/sonarqube-webapp).

When building the `sonarqube` repository, the webapp is automatically downloaded from Maven Central as a dependency, it makes it easy for you to contribute backend changes without having to care about the webapp.

But if your contribution also contains UI changes, you must clone the `sonarqube-webapp` repository, do your changes there, build it locally and then build the `sonarqube` repository using the `WEBAPP_BUILD_PATH` environment variable to target your custom build of the UI.

Here is an example of how to do it:

```bash
cd /path/to/sonarqube-webapp/server/sonar-web
# do your changes

# install dependencies, only needed the first time
yarn

# build the webapp
yarn build


cd /path/to/sonarqube

# build the sonarqube repository using the custom build of the webapp
WEBAPP_BUILD_PATH=/path/to/sonarqube-webapp/server/sonar-web/build/webapp ./gradlew build
```

You can also target a specific version of the webapp by updating the `webappVersion` property in the `./gradle.properties` file and then building the `sonarqube` repository normally.

## Translations files

Historically our translations were stored in `sonar-core/src/main/resources/org/sonar/l10n/core.properties`, but this file is now deprecated and not updated anymore.
Default translations (in English) are now defined in the webapp repository, here:
https://github.com/SonarSource/sonarqube-webapp/blob/master/libs/sq-server-shared/src/l10n/default.ts

The format has changed but you can still have it as a `.properties` file format by running the following command:

```bash
cd /path/to/sonarqube-webapp/server/sonar-web

# install dependencies, only needed the first time
yarn

# generate a backward compatible .properties file with all the translation keys
yarn generate-translation-keys
```

Note that contributing extensions for translations into other languages still work the same way as before. It's just the source of truth for the default translations that changed.

## License

Copyright (C) SonarSource Sàrl.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
