SonarQube [![Build Status](https://travis-ci.org/SonarSource/sonarqube.svg?branch=master)](https://travis-ci.org/SonarSource/sonarqube) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.sonarqube%3Asonarqube&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.sonarqube%3Asonarqube)
=========

Continuous Inspection
---------------------
SonarQube provides the capability to not only show health of an application but also to highlight issues newly introduced. With a Quality Gate in place, you can [fix the leak](https://blog.sonarsource.com/water-leak-changes-the-game-for-technical-debt-management) and therefore improve code quality systematically.

Links
-----

* [Website](https://www.sonarqube.org)
* [Download](https://www.sonarqube.org/downloads/)
* [Documentation](https://docs.sonarqube.org)
* [Twitter](https://twitter.com/SonarQube)
* [SonarSource](https://www.sonarsource.com), editor of SonarQube
* [Issue tracking](https://jira.sonarsource.com/browse/SONAR/), read-only. Only SonarSourcers can create tickets.
* [Demo](https://next.sonarqube.com/sonarqube) of the next version to be released

Have Question or Feedback?
--------------------------

For support questions ("How do I?", "I got this error, why?", ...), please first read the [documentation](https://docs.sonarqube.org) and then head to the [SonarSource forum](https://community.sonarsource.com/). There are chances that a question similar to yours has already been answered. 

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)


Contributing
------------

If you would like to see a new feature, please create a new thread in the forum ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing (Travis build is executed for each pull request).

Building
--------

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

Copyright 2008-2019 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
