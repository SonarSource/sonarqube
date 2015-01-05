# SonarQube

Put your technical debt under control. For more information please see:

* Website [sonarqube.org][1]
* [Issue tracker][2]
* [Wiki][3]
* [Developer Toolset](https://github.com/SonarSource/sonar-developer-toolset) for the configuration of Git and IDE

### Build

* Install JDK 6 or greater
* Install Maven 3.0.5 or greater
* Execute `build.sh`. ZIP file of application is generated into sonar-application/target/

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
 [3]: http://docs.sonarqube.org/display/SONAR
