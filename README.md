# SonarQube

Put your technical debt under control. For more information please see:

* Website [sonarqube.org][1]
* [Issue tracker][2]
* [Wiki][3]

## Sources

This Git repository is core platform. Plugins are hosted in SonarCommunity and SonarSource organisations.

### Build

* Install Maven 3.0.5 or greater
* Execute `mvn clean install`. To quickly build in development environment, the script `quick-build.sh` does not execute unit tests and compile GWT components for Firefox/Chrome only.
* ZIP file of application is generated in sonar-application/target/

## License

Copyright 2008-2013 SonarSource.

Licensed under the GNU Lesser General Public License, Version 3.0: http://www.gnu.org/licenses/lgpl.txt

 [1]: http://www.sonarqube.org/
 [2]: http://jira.codehaus.org/browse/SONAR
 [3]: http://docs.codehaus.org/display/SONAR
 
