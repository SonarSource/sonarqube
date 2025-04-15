# SonarQube [![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonarqube.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonarqube) [![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=sonarqube&metric=alert_status&token=d95182127dd5583f57578d769b511660601a8547)](https://next.sonarqube.com/sonarqube/dashboard?id=sonarqube) [![AI Code Assurance](https://next.sonarqube.com/sonarqube/api/project_badges/ai_code_assurance?project=org.sonarsource.sonarqube%3Asonarqube-private&token=sqb_c0e2fa9ac4ef89f9a8403c6ba235e108ceb1dce1)](https://next.sonarqube.com/sonarqube/dashboard?id=sonarqube)

## Continuous Inspection

SonarQube provides the capability to not only show the health of an application but also to highlight issues newly introduced. With a Quality Gate in place, you can [achieve Clean Code](https://www.sonarsource.com/solutions/clean-code/) and therefore improve code quality systematically.

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
| `licenseFormat --rerun-tasks`    | fix source headers by applying HEADER.txt |
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

Copyright 2008-2025 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
