SonarQube [![Build Status](https://travis-ci.org/SonarSource/sonarqube.svg?branch=master)](https://travis-ci.org/SonarSource/sonarqube) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.sonarqube%3Asonarqube&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.sonarqube%3Asonarqube)
=========

Continuous Inspection
---------------------
SonarQube provides the capability to not only show health of an application but also to highlight issues newly introduced. With a Quality Gate in place, you can [Clean As You Code](https://blog.sonarsource.com/clean-as-you-code) and therefore improve code quality systematically.

Links
-----

* [Website](https://www.sonarqube.org)
* [Download](https://www.sonarqube.org/downloads/)
* [Documentation](https://docs.sonarqube.org)
* [Twitter](https://twitter.com/SonarQube)
* [SonarSource](https://www.sonarsource.com), author of SonarQube
* [Issue tracking](https://jira.sonarsource.com/browse/SONAR/), read-only. Only SonarSourcers can create tickets.
* [Responsible Disclosure](https://community.sonarsource.com/t/responsible-vulnerability-disclosure/9317)
* [Dogfood](https://next.sonarqube.com/sonarqube) instance of the next SonarQube version

Have Question or Feedback?
--------------------------

For support questions ("How do I?", "I got this error, why?", ...), please first read the [documentation](https://docs.sonarqube.org) and then head to the [SonarSource Community](https://community.sonarsource.com/c/help/sq/10). The answer to your question has likely already been answered! 🤓

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. 😄


Contributing
------------

If you would like to see a new feature, please create a new Community thread: ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing (Travis build is executed for each pull request).

Willing to contribute to SonarSource products? We are looking for smart, passionate, and skilled people to help us build world-class code quality solutions. Have a look at our current [job offers here](https://www.sonarsource.com/company/jobs/)!

Building
--------

Run in cloud without installation [![TeamCode try-it-now](https://static01.teamcode.com/badge/demo.svg)](https://www.teamcode.com/tin/clone?applicationId=270968602867703808)

To build sources locally follow these instructions.

### Build and Run Unit Tests

Execute from project base directory:

    ./gradlew build

The zip distribution file is generated in `sonar-application/build/distributions/`. Unzip it and start server by executing:

    # on linux
    bin/linux-x86-64/sonar.sh start 
    # or on MacOS
    bin/macosx-universal-64/sonar.sh start
    # or on Windows
    bin\windows-x86-64\StartSonar.bat 

### Open in IDE

If the project has never been built, then build it as usual (see previous section) or use the quicker command:

    ./gradlew ide
    
Then open the root file `build.gradle` as a project in Intellij or Eclipse.

### Gradle Hints

| ./gradlew command | Description |
|---|---|
| `dependencies`| list dependencies |
| `dependencyCheckAnalyze` | list vulnerable dependencies |
| `dependencyUpdates` | list the dependencies that could be updated |
| `licenseFormat --rerun-tasks` | fix source headers by applying HEADER.txt |
| `wrapper --gradle-version 5.2.1` | upgrade wrapper |

License
-------

Copyright 2008-2021 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
