---
title: SonarScanner for Jenkins
url: /analysis/scan/sonarscanner-for-jenkins/
---

[[info]]
| By [SonarSource](https://www.sonarsource.com/) – GNU LGPL 3 – [Issue Tracker](https://jira.sonarsource.com/browse/SONARJNKNS) – [Source](https://github.com/SonarSource/sonar-scanner-jenkins)
| Current version: **SonarScanner for Jenkins 2.9**

This plugin lets you centralize the configuration of SonarQube server connection details in Jenkins global configuration.

Then you can trigger SonarQube analysis from Jenkins using standard Jenkins Build Steps or [Jenkins Pipeline DSL](https://jenkins.io/solutions/pipeline/) to trigger analysis with:

* [SonarScanner](/analysis/scan/sonarscanner/)
* [SonarScanner for Maven](/analysis/scan/sonarscanner-for-maven/)
* [SonarScanner for Gradle](/analysis/scan/sonarscanner-for-gradle/)
* [SonarScanner for MSBuild](/analysis/scan/sonarscanner-for-msbuild/)

Once the job is complete, the plugin will detect that a SonarQube analysis was made during the build and display a badge and a widget on the job page with a link to the SonarQube dashboard as well as quality gate status.

## Installation
1. [Install the SonarScanner for Jenkins via the Jenkins Update Center](https://plugins.jenkins.io/sonar).
1. Configure your SonarQube server(s)
   * Log into Jenkins as an administrator and go to Manage Jenkins > Configure System
   * Scroll down to the SonarQube configuration section, click on Add SonarQube, and add the values you're prompted for.
   * The server authentication token should be created as a 'Secret Text' credential

## Analyzing a .NET solution
**Global Configuration**  
This step is mandatory if you want to trigger any of your analyses with the SonarScanner for MSBuild. You can define as many scanner instances as you wish. Then for each Jenkins job, you will be able to choose with which launcher to use to run the SonarQube analysis.
1. Log into Jenkins as an administrator and go to **Manage Jenkins > Global Tool Configuration**
1. Click on **Add SonarScanner for MSBuild**
1. Add an installation of the latest available version. Check **Install automatically** to have the SonarScanner for MSBuild automatically provisioned on your Jenkins executors

If you do not see any available version under Install from GitHub, first go to Manage Jenkins > Manage Plugins > Advanced and click on Check now

**Job Configuration**  
1. Configure the project, and go to the **Build** section.
1. Add the SonarQube for MSBuild - Begin Analysis to your build
1. Configure the SonarQube Project Key, Name and Version in the SonarScanner for MSBuild - Begin Analysis build step
1. Add the MSBuild build step or the Execute Windows batch command to execute the build with MSBuild 14 (see compatibility) to your build.
1. Add the SonarQube for MSBuild - End Analysis build steps to your build

## Analyzing a Java project with Maven or Gradle
** Global Configuration**  
1. Log into Jenkins as an administrator and go to Manage Jenkins > Configure System
1. Scroll to the SonarQube servers section and check Enable injection of SonarQube server configuration as build environment variables

** Job Configuration**  
1. **Configure** the project, and go to the **Build Environment** section.
1. Enable **Prepare SonarScanner environment** to allow the injection of SonarQube server values into this particular job. If multiple SonarQube instances are configured, you will be able to choose which one to use.
Once the environment variables are available, use them in a standard Maven build step (Invoke top-level Maven targets) by setting the Goals to include, or a standard Gradle build step (Invoke Gradle script) by setting the Tasks to execute.

Maven goal:
```
$SONAR_MAVEN_GOAL
```
Gradle task:
```
sonarqube
```

In both cases, launching your analysis may require authentication. In that case, make sure that the Global Configuration defines a valid SonarQube token.

## Analyzing other project types

**Global Configuration**  
This step is mandatory if you want to trigger any of your SonarQube analyses with the SonarScanner. You can define as many scanner instances as you wish. Then for each Jenkins job, you will be able to choose with which launcher to use to run the SonarQube analysis.

1. Log into Jenkins as an administrator and go to **Manage Jenkins > Global Tool Configuration**
1. Scroll down to the SonarScanner configuration section and click on Add SonarScanner. It is based on the typical Jenkins tool auto-installation. You can either choose to point to an already installed version of SonarScanner (uncheck 'Install automatically') or tell Jenkins to grab the installer from a remote location (check 'Install automatically')

If you don't see a drop down list with all available SonarScanner versions but instead see an empty text field then this is because Jenkins still hasn't downloaded the required update center file (default period is 1 day). You may force this refresh by clicking 'Check Now' button in Manage Plugins > Advanced tab.

**Job Configuration**  
1. **Configure** the project, and go to the **Build** section. 
1. Add the SonarScanner build step to your build.
1. Configure the SonarQube analysis properties. You can either point to an existing sonar-project.properties file or set the analysis properties directly in the **Analysis properties** field



## Using a Jenkins pipeline
We provide a `withSonarQubeEnv` block that allows you to select the SonarQube server you want to interact with. Connection details you have configured in Jenkins global configuration will be automatically passed to the scanner.

If needed you can override the `credentialId` if you don't want to use the one defined in global configuration (for example if you define credentials at folder level).

Here are a some examples for every scanner, assuming you run on Unix slaves and you have configured a server named "My SonarQube Server" as well as required tools. If you run on Windows slaves, just replace `sh` with `bat`.

SonarScanner:
```
node {
  stage('SCM') {
    git 'https://github.com/foo/bar.git'
  }
  stage('SonarQube analysis') {
    def scannerHome = tool 'SonarScanner 4.0';
    withSonarQubeEnv('My SonarQube Server') { // If you have configured more than one global server connection, you can specify its name
      sh "${scannerHome}/bin/sonar-scanner"
    }
  }
}
```
SonarScanner for Gradle:
```
node {
  stage('SCM') {
    git 'https://github.com/foo/bar.git'
  }
  stage('SonarQube analysis') {
    withSonarQubeEnv() { // Will pick the global server connection you have configured
      sh './gradlew sonarqube'
    }
  }
}
```
SonarScanner for Maven:
```
node {
  stage('SCM') {
    git 'https://github.com/foo/bar.git'
  }
  stage('SonarQube analysis') {
    withSonarQubeEnv(credentialsId: 'f225455e-ea59-40fa-8af7-08176e86507a', installationName: 'My SonarQube Server') { // You can override the credential to be used
      sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
    }
  }
}
```
SonarScanner for MSBuild:
```
node {
  stage('SCM') {
    git 'https://github.com/foo/bar.git'
  }
  stage('Build + SonarQube analysis') {
    def sqScannerMsBuildHome = tool 'Scanner for MSBuild 4.6'
    withSonarQubeEnv('My SonarQube Server') {
      bat "${sqScannerMsBuildHome}\\SonarQube.Scanner.MSBuild.exe begin /k:myKey"
      bat 'MSBuild.exe /t:Rebuild'
      bat "${sqScannerMsBuildHome}\\SonarQube.Scanner.MSBuild.exe end"
    }
  }
}
```

## Pause pipeline until quality gate is computed
The `waitForQualityGate` step will pause the pipeline until SonarQube analysis is completed and returns quality gate status.

### Pre-requisites:
* Configure a webhook in your SonarQube server pointing to `<your Jenkins instance>/sonarqube-webhook/` 
* Use `withSonarQubeEnv` step in your pipeline (so that SonarQube taskId is correctly attached to the pipeline context).


Scripted pipeline example:
```
node {
  stage('SCM') {
    git 'https://github.com/foo/bar.git'
  }
  stage('SonarQube analysis') {
    withSonarQubeEnv('My SonarQube Server') {
      sh 'mvn clean package sonar:sonar'
    } // submitted SonarQube taskId is automatically attached to the pipeline context
  }
}
  
// No need to occupy a node
stage("Quality Gate"){
  timeout(time: 1, unit: 'HOURS') { // Just in case something goes wrong, pipeline will be killed after a timeout
    def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
    if (qg.status != 'OK') {
      error "Pipeline aborted due to quality gate failure: ${qg.status}"
    }
  }
}
```
Thanks to the webhook, the step is implemented in a very lightweight way: no need to occupy a node doing polling, and it doesn't prevent Jenkins to restart (step will be restored after restart). Note that to prevent race conditions, when the step starts (or is restarted) a direct call is made to the server to check if the task is already completed.

Declarative pipeline example:
```
pipeline {
    agent any
    stages {
        stage('SCM') {
            steps {
                git url: 'https://github.com/foo/bar.git'
            }
        }
        stage('build && SonarQube analysis') {
            steps {
                withSonarQubeEnv('My SonarQube Server') {
                    // Optionally use a Maven environment you've configured already
                    withMaven(maven:'Maven 3.5') {
                        sh 'mvn clean package sonar:sonar'
                    }
                }
            }
        }
        stage("Quality Gate") {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                    // true = set pipeline to UNSTABLE, false = don't
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}
```

If you want to run multiple analysis in the same pipeline and use waitForQualityGate you have to do everything in order:
```
pipeline {
    agent any
    stages {
        stage('SonarQube analysis 1') {
            steps {
                sh 'mvn clean package sonar:sonar'
            }
        }
        stage("Quality Gate 1") {
            steps {
                waitForQualityGate abortPipeline: true
            }
        }
        stage('SonarQube analysis 2') {
            steps {
                sh 'gradle sonarqube'
            }
        }
        stage("Quality Gate 2") {
            steps {
                waitForQualityGate abortPipeline: true
            }
        }
    }
}
```
