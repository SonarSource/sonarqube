---
title: Moving Analysis to Java 11
url: /analysis/analysis-with-java-11/
---

Java 11 is required for all scanners. If you're using a previous version of the Java, see the section below that aligns with your build for information on moving your analysis to Java 11.

## Maven or Gradle

We suggest basing your whole build on Java 11. If that's not compatible, you can have a dedicated script for the analysis that overrides the `JAVA_HOME` environment variable just before running it. See the following examples:

### Maven

```
mvn verify ...
export JAVA_HOME=/path/to/java11
mvn sonar:sonar ...
```

### Gradle 

```
gradle build ...
export JAVA_HOME=/path/to/java11
gradle sonarqube ...
```

## Azure DevOps

If you are running your build with a Microsoft-hosted agent, this is already automatically done, and you're all set. 

If you're using a self-hosted agent, you can either modify your build pipeline to ensure that it runs with Java 11 by default or override the `JAVA_HOME` environment variable just before running the analysis.

### Xamarin
In the specific case of Xamarin only allowing Java 8, you will need to specify a Java 8 path while invoking MSBuild, allowing the JAVA_HOME environment variable for the scanner only.

```
$env:XAMARIN_JAVA_HOME=/path/to/java8
msbuild.exe  /p:JavaSdkDirectory=$env:XAMARIN_JAVA_HOME
```

## Dockerfile

You can use several base images to run your build with Java 11. Here are some examples:

- openjdk:11-jre-slim
- debian:buster and above
- gradle:jre11-slim

If your build is not compatible with Java 11, you can override `JAVA_HOME` environment variable before running scanners.

## Jenkins

You can easily define a new JDK version by navigating to **Manage Jenkins > Global Tool Configuration** if you have the [JDK Tool Plugin](https://plugins.jenkins.io/jdk-tool/) installed.

### Declarative Pipelines

If you are using a declarative pipeline with different stages you can add a 'tools' section to the stage in which the code scan occurs. This makes the scanner use the specificed JDK version.

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

If you are analyzing a Java 8 project you probably want to continue using Java 8 to build your project. The following example allows you to continue building in Java 8 but will use Java 11 to scan the code:

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

The previous example is for Maven, but you can easily modify it for Gradle.

### Classical Pipelines

#### **Set Job JDK version**  
You can set the JDK version that a job should use in the **General** section of your configuration. This option is only visible if you have configured multiple JDK versions under **Manage Jenkins > Global Tool Configuration**.

#### **Set Execute SonarQube Scanner JDK version**  
If you're using the **Execute SonarQube Scanner** step in your configuration, you can set the JDK for this step in the configuration dialog. This allows you to use JDK 11 for the code scanning performed by SonarQube and the globally configured JDK for all other steps in the job.

#### **Java 8 projects**  
Jenkins doesn't let you switch JDKs when using a 'Freestyle project' or 'Maven project' configuration, so when you want to build your project using Java 8 you have to manually set the `JAVA_HOME` variable to Java 11 when executing the scanner.

You can do this with the [Tool Environment Plugin](https://plugins.jenkins.io/toolenv/). When this plugin is installed, you can expose the location of the JDK you added under **Manage Jenkins > Global Tool Configuration**.

The location of the JDK can then be used to set the `JAVA_HOME` environment variable. The build and post steps sections can be configured as follows:
![Configuration](/images/jenkins-maven-setup.png)

