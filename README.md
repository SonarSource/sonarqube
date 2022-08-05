sonarqube9.4 metric new_lines has a bug ,when I scan my project new_lines=18,655, but after I  increase 147 lines ，I scan again new_lines=18803,
Normally it would be 147。
![image](https://user-images.githubusercontent.com/4475925/183040463-5c59cb10-f02d-4a43-bddf-bee58c10aaf0.png)
![image](https://user-images.githubusercontent.com/4475925/183041099-b7e6a003-ea1a-4bbd-8fd0-00bd204e4f55.png)


SonarQube [![Build Status](https://app.travis-ci.com/SonarSource/sonarqube.svg?branch=master)](https://app.travis-ci.com/SonarSource/sonarqube) [![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=sonarqube&metric=alert_status&token=d95182127dd5583f57578d769b511660601a8547)](https://next.sonarqube.com/sonarqube/dashboard?id=sonarqube)
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
* [Next](https://next.sonarqube.com/sonarqube) instance of the next SonarQube version

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
| `licenseFormat --rerun-tasks` | fix source headers by applying HEADER.txt |
| `wrapper --gradle-version 5.2.1` | upgrade wrapper |

License
-------

Copyright 2008-2022 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
