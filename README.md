SonarQube [![Build Status](https://travis-ci.org/SonarSource/sonarqube.svg?branch=master)](https://travis-ci.org/SonarSource/sonarqube)
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
* [Demo](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.sonarqube%3Asonarqube) of the next version to be released

Have Question or Feedback?
--------------------------

For support questions ("How do I?", "I got this error, why?", ...), please first read the [documentation](https://docs.sonarqube.org) and then head to [Stackoverflow](http://stackoverflow.com/questions/tagged/sonarqube). We actively follow the `sonarqube` tag there, and there are chances that we have already answered to a question similar to yours. 

To provide feedback (request a feature, report a bug etc.) use the [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube). Be aware that this group is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)


Contributing
------------

### Pull Request

Please create a new thread in [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube) when contributing a new feature. You have to be sure that the feature complies with our roadmap and expectations. 

To submit a code contribution, create a pull request for this repository. Please explain your motives to contribute this change (if it's not a new feature): what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all [tests](#testing) are passing (Travis build is executed for each pull request).


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
    
Then simply open the root file `build.gradle` as a project in Intellij or Eclipse.

### Run Integration Tests

Integration tests are grouped into categories, listed in [tests/build.gradle]().
A single category should be run at a time, by executing from project base directory: 

    ./gradlew integrationTest -Dcategory=<category>
    
Example:

    ./gradlew integrationTest -Dcategory=Category1
    
### Configure Integration Tests

Environment of tests can be configured with command-line properties and file `~/.sonar/orchestrator/orchestrator.properties`, if it exists.
Here is a template example:

    # Token used to download SonarSource private artifacts from https://repox.sonarsource.com,
    # required for the category "Plugins".
    # Generate your API key at https://repox.sonarsource.com/webapp/#/profile
    #orchestrator.artifactory.apiKey=

    # Personal access token used to request SonarSource development licenses at https://github.com/sonarsource/licenses,
    # required for the category "Plugins". 
    # Generate a token from https://github.com/settings/tokens
    #github.token=
      
    # Browser to be used in Selenium tests. 
    # Values are:  
    # - "firefox" (default). Supports only versions <= 46
    # - "marionette", for versions of Firefox greater than 46
    # - "chrome". Requires the Chrome driver to be installed (see https://sites.google.com/a/chromium.org/chromedriver/). 
    #             On MacOS, simply run "brew install chromedriver".
    #orchestrator.browser=firefox
    
    # Port of SonarQube server, for example 10000. Default value is 0 (random).
    #orchestrator.container.port=0
    
    # Maven installation, used by the tests running Scanner for Maven.
    # By default Maven binary is searched in $PATH
    #maven.home=/usr/local/Cellar/maven/3.5.0/libexec
    
    # Database connection. Embedded H2 is used by default.
   
    # Example for PostgreSQL:
    #sonar.jdbc.dialect=postgresql
    #sonar.jdbc.url=jdbc:postgresql://localhost:15432/sonar
    #sonar.jdbc.rootUsername=postgres
    #sonar.jdbc.rootPassword=sonarsource
    #sonar.jdbc.rootUrl=jdbc:postgresql://localhost:15432/postgres
    #sonar.jdbc.username=sonar
    #sonar.jdbc.password=sonar    
    #sonar.jdbc.schema=public
    
    # Example for Oracle 12c:
    #sonar.jdbc.dialect=oracle
    #sonar.jdbc.url=jdbc:oracle:thin:@localhost:1521/ORCL
    #sonar.jdbc.rootUrl=jdbc:oracle:thin:@localhost:1521/ORCL
    #sonar.jdbc.rootUsername=SYSTEM
    #sonar.jdbc.rootPassword=system
    #sonar.jdbc.username=sonar
    #sonar.jdbc.password=sonar
    #sonar.jdbc.driverMavenKey=com.oracle.jdbc:ojdbc8:12.2.0.1.0
    
    # Example for SQLServer
    #sonar.jdbc.dialect=mssql
    #sonar.jdbc.url=jdbc:jtds:sqlserver://localhost/sonar;SelectMethod=Cursor
    #sonar.jdbc.rootUrl=jdbc:jtds:sqlserver://localhost;SelectMethod=Cursor
    #sonar.jdbc.rootUsername=admin
    #sonar.jdbc.rootPassword=admin
    #sonar.jdbc.username=sonar
    #sonar.jdbc.password=sonar
    
The path to a custom configuration file can be provided with command-line property `-Dorchestrator.configUrl=file:///path/to/orchestrator.properties` or with
environment variable `ORCHESTRATOR_CONFIG_URL=file:///path/to/orchestrator.properties`.

### Find available updates of dependencies

Execute from project base directory:

    ./gradlew dependencyUpdates

### Update the files missing the license header

Execute from project base directory:

    ./gradlew licenseFormat --rerun-tasks

License
-------

Copyright 2008-2018 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
