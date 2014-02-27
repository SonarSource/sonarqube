# SonarQube

Put your technical debt under control. For more information please see:

* Website [sonarqube.org][1]
* [Issue tracker][2]
* [Wiki][3]

## Sources

This Git repository is core platform. Plugins are hosted in SonarCommunity and SonarSource organisations.

### Checkout Sources

If you have never used Git before, you need to do some setup first. Run the following commands so that GIT knows your name and email.

    git config --global user.name "Your Name"
    git config --global user.email "your@email.com"

Setup line endings preferences:

    # For Unix/Mac users
    git config --global core.autocrlf input
    git config --global core.safecrlf true

    # For Windows users
    git config --global core.autocrlf true
    git config --global core.safecrlf true

Get sources by executing:

    git clone https://github.com/SonarSource/sonar.git
    
Committers must configure their SSH key (see GitHub documentation for Windows and Mac) and clone repository:

    git clone git@github.com:SonarSource/sonar.git

### Build

* Install JDK 6 or greater
* Install Maven 3.0.5 or greater
* Execute `mvn clean install`. ZIP file of application is generated into sonar-application/target/
* To speed-up the build, http://nodejs.org must be installed (simply execute `brew install nodejs` on MacOS). node executable node must be available in PATH.

### Edit Ruby Code

The development mode is used to edit Ruby on Rails code. The application is automatically reloaded when Ruby files are saved. It avoids restarting the server. Changes are reloaded on the fly. Execute one of the following commands from the sonar-server/ directory to start server:

    # for embedded database
    sonar-server/h2-start.sh
    
    # or for other dbs
    sonar-server/mysql-start.sh
    sonar-server/postgresql-start.sh

Then Ruby code can be directly edited from sonar-server/src/main/webapp/WEB-INF/app.

### Debug Maven Analysis

Debug Maven analysis by executing `mvnDebug sonar:sonar`. Then attach your IDE to the remote process (the listening port is 8000).

Example in Intellij Idea : Run -> Edit configurations -> Add new configuration -> Remote -> port 8000.

### Profile Maven Analysis with JProfiler

Duplicate $MAVEN_HOME/bin/mvnDebug to mvnJProfiler and replace the property MAVEN_DEBUG_OPTS by:

    MAVEN_DEBUG_OPTS="-Xint -agentlib:jprofilerti=port=8849 -Xbootclasspath/a:/path/to/jprofiler/bin/agent.jar"

Then start JProfiler -> Connect to an application on a remote computer

## License

Copyright 2008-2014 SonarSource.

Licensed under the GNU Lesser General Public License, Version 3.0: http://www.gnu.org/licenses/lgpl.txt

 [1]: http://www.sonarqube.org/
 [2]: http://jira.codehaus.org/browse/SONAR
 [3]: http://docs.codehaus.org/display/SONAR
 
