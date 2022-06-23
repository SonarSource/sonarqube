---
title: Java Test Coverage
url: /analysis/test-coverage/java-test-coverage/
---

SonarQube supports the reporting of test coverage as part of the analysis of your Java project.

However, SonarQube does not generate the coverage report itself.
Instead, you must set up a third-party tool to produce the report as part of your build process.
You then need to configure your analysis to tell the SonarScanner where the report is located so that it can pick it up and send it to SonarQube, where it will be displayed on your project dashboard along with the other analysis metrics.

For Java projects, SonarQube directly supports the JaCoCo coverage tool
(see [Generic Test Data](/analysis/generic-test/) for information on integrating other coverage tools).


## Adjust your setup

To enable coverage, you need to:

* Adjust your build process so that JaCoCo report generation step runs _before_ the SonarScanner step.
* Make sure that JacCoCo writes its report file to a defined path in the build environment.
* Configure the scanning step of your build so that the SonarScanner picks up the report file from that defined path.


## Add coverage in a single-module Maven project

To add coverage to your Maven project you need to use the [`jacoco-maven-plugin`](https://mvnrepository.com/artifact/org.jacoco/jacoco-maven-plugin) and its `report` goal to create a code coverage report.

Typically, you would create a specific Maven profile for executing the unit tests with instrumentation and producing the coverage report only on demand. 

In the most basic case, we will need to execute two goals: `jacoco:prepare-agent`, which allows coverage info to be collected during unit tests execution, and `jacoco:report`, which uses data collected during unit test execution to generate a report.
By default, the tool generates XML, HTML, and CSV versions of the report.
Here, we explicitly specify XML, since that is the only one we need for SonarQube.
The `<profile>` section of your `pom.xml` should look something like this:

```
<profile>
  <id>coverage</id>
  <build>
   <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
     <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.7</version>
      <executions>
        <execution>
          <id>prepare-agent</id>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <goals>
            <goal>report</goal>
          </goals>
          <configuration>
            <formats>
              <format>XML</format>
            </formats>
          </configuration>
        </execution>
      </executions>
    </plugin>
    ...
   </plugins>
  </build>
</profile>
```

By default the generated report will be saved under `target/site/jacoco/jacoco.xml`.
This location will be checked automatically by the scanner, so no further configuration is required.
Just launch: 

```
mvn sonar:sonar -Pcoverage
```

as usual and the report will be picked up.

If you need to change the directory where the report is generated, you can set the property either on the command line using Maven’s `-D` switch:

```
mvn -Dsonar.coverage.jacoco.xmlReportPaths=
      ../app-it/target/site/jacoco-aggregate/jacoco.xml
    sonar:sonar -Pcoverage
```

or in your `pom.xml`:

```
<properties>
  <sonar.coverage.jacoco.xmlReportPaths>
    ../app-it/target/site/jacoco-aggregate/jacoco.xml
  </sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

Wildcards and a comma-delimited list of paths are supported.
See [Coverage Analysis Parameters](/analysis/test-coverage/test-coverage-parameters/) for details.
The path can be either absolute or relative to the project root.


## Add coverage in a multi-module Maven project

For multi-module Maven projects, you configure the `jacoco-maven-plugin` in a profile in the parent pom just as in the single module case, above. By default, a separate coverage report will be generated for each module.

If you want to aggregate all the module-specific reports into one project-level report, the easiest solution is to create a special Maven module (alongside the ones you already have), that contains nothing except a `pom.xml` that uses the `report-aggregate` goal. Here is an example:

```
<project>
  <artifactId>my-project-report-aggregate</artifactId>
  <name>My Project</name>
  <description>Aggregate Coverage Report</description>
  <dependencies>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>my-module-1</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>my-module-2</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>report-aggregate</id>
            <phase>verify</phase>
            <goals>
              <goal>report-aggregate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

When you invoke `maven clean verify` in the `report-aggregate-module` directory the aggregated report will be generated and placed inside that directory at the standard location `target/site/jacoco-aggregate/jacoco.xml`.
Then, in the top level `pom.xml` you set `sonar.coverage.jacoco.xmlReportPaths` to this location:

```
<properties>/
  <sonar.coverage.jacoco.xmlReportPaths>
    ${project.basedir}/report-aggregate/target/site/
      jacoco-aggregate/jacoco.xml
  </sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

Wildcards and a comma-delimited list of paths are supported.
See [Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/) for details.


## Add coverage in a Gradle project

To set up code coverage for your Gradle files, you just need to apply the JaCoCo plugin together with the SonarScanner for Gradle to the `build.gradle` file of your project as the JaCoCo is already integrated into the default gradle distribution:

```
plugins {
    id "jacoco"
    id "org.sonarqube" version "3.3"
}

jacocoTestReport {
    reports {
        xml.enabled true
    }
}
```

Your report will be automatically saved in the `build/reports/jacoco` directory.
The SonarQube plugin automatically detects this location so no further configuration is required.
To import coverage, launch:

```
  gradle test jacocoTestReport sonarqube
```

For more details, see the [Gradle JaCoCo Plugin documentation](https://docs.gradle.org/current/userguide/jacoco_plugin.html) and


## Coverage parameter can also be set in the UI

The `sonar.coverage.jacoco.xmlReportPaths` parameter can also be set in the SonarQube interface under

  **_Your Project_ > Project Settings > General Settings > JaCoCo**

  for project-level settings, and

  **Administration > Configuration > General Settings > JaCoCo**

  for global settings (applying to all projects).


## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).
