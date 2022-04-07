---
title: Scanner Environment
url: /analysis/scanner-environment/
---

A Java runtime environment is always required to run the scanner that performs (CI-based) analysis.
This applies to all scanner variants (CLI, CI-specific, etc.)

Additionally, in order to analyze JavaScript, TypeScript or CSS, the scanner also requires a Node.js runtime.

The required versions for these runtimes change with successive versions of the scanner.
The current requirements and recommendations are:

* You must use either **Java 11 or 17**.
* You should use at least **Node.js 14**, though we recommend that you use the **latest Node.js LTS**, which is currently **Node.js 16**.

## Scanner vs project

The requirements above refer only to the versions of Java and Node.js *used by the scanner* itself to run.
It does not restrict the versions of Java, JavaScript, TypeScript or CSS that can be analyzed by the scanner.


## Java configuration

### GitHub Actions

The SonarQube GitHub Action can be configured for different target build technologies (.NET, Gradle, Maven, etc).


### Maven / Gradle

If your whole Maven or Gradle build doesn't run on Java 11 or 17, we suggest first to try to base the whole build on one of those two versions of Java.
If it's not compatible, then you can override the JAVA_HOME environment variable just before the analysis step, as shown here:

```
# Maven build
mvn verify ...
export JAVA_HOME=/path/to/java-11-or-17
mvn sonar:sonar ...
```

```
# Gradle build
gradle build ...
export JAVA_HOME=/path/to/java-11-or-17
gradle sonarqube ...
```

### Azure DevOps

All VM images available in Azure Pipelines for Microsoft-hosted agents already contain Java 11.
There is no further action required.
For self-hosted agents you must ensure that you are using Java 11 or 17.
You can either modify your build pipeline to ensure that it runs with Java 11 or 17 by default, or override the JAVA_HOME environment variable just before running the analysis.


#### Xamarin

For the specific case of Xamarin, which only allows Java 8, you will need to specify a Java 8 path separately when invoking MSBuild (using, for example, XAMARIN_JAVA_HOME), and then leave the JAVA_HOME environment variable for the scanner only.

```
$env:JAVA_HOME=/path/to/java-11-or-17
$env:XAMARIN_JAVA_HOME=/path/to/java-8
msbuild.exe  /p:JavaSdkDirectory=$env:XAMARIN_JAVA_HOME
```


### Dockerfile

Multiple base images can be used to run your build with Java 11 or 17, here are some examples:

* `openjdk:11-jre-slim`
* `debian:buster and above`
* `gradle:jre11-slim`

If your build is not compatible with Java 11 or 17, then you can override the `JAVA_HOME` environment variable to point to Java 11 or 17 immediately before running the analysis.


### Jenkins

You can define a new JDK in **Manage Jenkins > Global Tool Configuration**, if you have the [JDK Tool Plugin](https://plugins.jenkins.io/jdk-tool/) installed.


#### Declarative Pipelines

If you are using a declarative pipeline with different stages, you can add a 'tools' section to the stage in which the code scan occurs.
This will make the scanner use the JDK version that is specified.

```
stage('SonarQube analysis') {
    tools {
        jdk "jdk11" // the name you have given the JDK installation in Global Tool Configuration
    }
    environment {
        scannerHome = tool 'SonarQube Scanner' // the name you have given the Sonar Scanner (in Global Tool Configuration)
    }
    steps {
        withSonarQubeEnv(installationName: 'SonarQube') {
            sh "${scannerHome}/bin/sonar-scanner -X"
        }
    }
}
```

If you are analyzing a Java 8 project, you probably want to continue using Java 8 to build your project.
The following example allows you to continue building in Java 8, but will use Java 11 to scan the code:

```
stage('Build') {
 tools {
        jdk "jdk8" // the name you have given the JDK installation using the JDK manager (Global Tool Configuration)
    }
    steps {
        sh 'mvn compile'
    }
}
stage('SonarQube analysis') {
    tools {
        jdk "jdk11" // the name you have given the JDK installation using the JDK manager (Global Tool Configuration)
    }
    environment {
        scannerHome = tool 'SonarQube Scanner' // the name you have given the Sonar Scanner (Global Tool Configuration)
    }
    steps {
        withSonarQubeEnv(installationName: 'SonarQube') {
            sh 'mvn sonar:sonar'
        }
    }
}
```

This example is for Maven but it can be easily modified to use Gradle.

#### Classical pipelines

**Set Job JDK version**

You can easily set the JDK version to be used by a job in the **General** section of your configuration.
This option is only visible if you have configured multiple JDK versions under **Manage Jenkins > Global Tool Configuration**.

**Set 'Execute SonarQube Scanner' JDK version**

If you are using the **Execute SonarQube Scanner** step in your configuration, you can set the JDK for this step in the configuration dialog.
By using this approach, you can use JDK 11 or 17 only for the code scanning performed by SonarQube.
All the other steps in the job will use the globally configured JDK.

**Java 8 projects**

Jenkins does not offer functionality to switch JDKs when using a **Freestyle project** or **Maven project** configuration.
To build your project using Java 8, you have to manually set the `JAVA_HOME` variable to Java 11 or 17 when running the analysis.

To do this use the [Tool Environment Plugin](https://plugins.jenkins.io/toolenv/). This plugin lets expose the location of the JDK you added under **Manage Jenkins > Global Tool Configuration**.
The location of the JDK can then be used to set the `JAVA_HOME` variable in a post step command, like this:

```
export JAVA_HOME=$OPENJDK_11_HOME/Contents/Home
mvn $SONAR_MAVEN_GOAL
```

## Node.js configuration

### GitHub Actions

The SonarQube GitHub Action already uses Node.js 14+. If you are using the official SonarQube Action, there is nothing further to do. If you are using your own GitHub Action and invoke the SonarScanner manually within that Action, then you should ensure that you are also using at least Node.js 14. See **Other cases** below.


### Bitbucket Pipelines

The `sonarqube-scan` Bitbucket Pipe uses Node.js 14+. We recommend using the latest version of the pipe declaration in your `bitbucket-pipelines.yml`. For example:

`- pipe: sonarsource/sonarqube-scan:1.1.0`


### Azure Pipelines

All VM images available in Azure Pipelines for Microsoft-hosted agents already contain Node.js 14+. There is no further action required. For self-hosted agents you must ensure that you are using Node.js 14+.


### GitLab CI/CD

The recommended setup for your `.gitlab-ci.yml` specifies `sonar-scanner-cli:latest` which already uses Node.js 14+. If you are using the recommended setup there is nothing further to do.


### Jenkins

You should ensure that the Node.js version used by your Jenkins jobs is at least version 14. If you want to manage multiple versions of Node.js in Jenkins, the NodeJS Jenkins plugin may be useful.


### Other cases

If your build set up falls into one of the following categories, then you will need to ensure that the build environment within which the SonarScanner runs has Node.js 14+ installed:

* Manual invocation of the SonarScanner from the command line.
* Custom local build script invoking the SonarScanner.
* Non-standard use of a CI listed above. For example, using GitHub Actions but not using the provided SonarQube GitHub Action and instead using some other custom Action.
* Use of a CI which is not listed above.

In general, if you are running the SonarScanner command line tool as an executable, then Node.js 14+ needs to be installed on the machine where it is run. Alternatively, if you are using the SonarScanner Docker image, then you just have to ensure that you are using at least version 4.6 of the image, as it already bundles the correct version of Node.js.
