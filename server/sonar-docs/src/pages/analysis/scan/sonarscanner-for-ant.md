---
title: SonarScanner for Ant
url: /analysis/scan/sonarscanner-for-ant/
---

[[info]]
| **Download SonarScanner for Ant 2.6** - Compatible with SonarQube 6.7+ (LTS)  
| By [SonarSource](https://www.sonarsource.com/) – GNU LGPL 3 – [Issue Tracker](https://jira.sonarsource.com/browse/ANTTASK) – [Source](https://github.com/SonarSource/sonar-scanner-ant) 
|
| [Download](https://binaries.sonarsource.com/Distribution/sonarqube-ant-task/sonarqube-ant-task-2.6.0.1426.jar)

The SonarScanner for Ant provides a `task` to allow integration of SonarQube analysis into an Apache Ant build script.

The SonarScanner for Ant is an Ant Task that is wrapper of [SonarScanner](/analysis/scan/sonarscanner/), which works by invoking SonarScanner and passing to it all [properties](/analysis/analysis-parameters/) named following a `sonar.*` convention. This has the downside of not being very Ant-y, but the upside of providing instant availability of any new analysis parameter introduced by a new version of a plugin or of SonarQube itself. Therefore, successful use of the SonarScanner for Ant requires strict adherence to the property names shown below.


## Use
Define a new sonar Ant target in your Ant build script:
```
<!-- build.xml -->
<project name="My Project" default="all" basedir="." xmlns:sonar="antlib:org.sonar.ant">
...
  
<!-- Define the SonarQube global properties (the most usual way is to pass these properties via the command line) -->
<property name="sonar.host.url" value="http://localhost:9000" />
 
...
  
<!-- Define the SonarQube project properties -->
<property name="sonar.projectKey" value="org.sonarqube:sonarqube-scanner-ant" />
<property name="sonar.projectName" value="Example of SonarScanner for Ant Usage" />
<property name="sonar.projectVersion" value="1.0" />
<property name="sonar.sources" value="src" />
<property name="sonar.java.binaries" value="build" />
<property name="sonar.java.libraries" value="lib/*.jar" />
...
 
<!-- Define SonarScanner for Ant Target -->
<target name="sonar">
    <taskdef uri="antlib:org.sonar.ant" resource="org/sonar/ant/antlib.xml">
        <!-- Update the following line, or put the "sonarqube-ant-task-*.jar" file in your "$HOME/.ant/lib" folder -->
        <classpath path="path/to/sonar/ant/task/lib/sonarqube-ant-task-*.jar" />
    </taskdef>
 
    <!-- Execute SonarScanner for Ant Analysis -->
    <sonar:sonar />
</target>
```
Run the following command from the project base directory to launch the analysis:
```
ant sonar
```


## Sample Project
To help you get started, a simple project sample is available here: https://github.com/SonarSource/sonar-scanning-examples/tree/master/sonarqube-scanner-ant

## Troubleshooting
**Enable Debug Logs**  
To enable debug logs, use the regular Ant verbose option: `-v`
```
ant sonar -v
```
