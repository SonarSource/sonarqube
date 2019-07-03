---
title: SonarScanner
url: /analysis/scan/sonarscanner/
---

[[info]]
| **Download SonarScanner 4.0** - Compatible with SonarQube 6.7+ (LTS)
| By [SonarSource](https://www.sonarsource.com/) – GNU LGPL 3 – [Issue Tracker](https://jira.sonarsource.com/browse/SQSCANNER) – [Source](https://github.com/Sonarsource/sonar-scanner-cli)   
|
| [Linux 64-bit](https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744-linux.zip) |
| [Windowx 64-bit](https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744-windows.zip) |
| [Mac OS X 64-bit](https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744-macosx.zip) |
| [Any*](https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744.zip)
| *Requires a pre-installed JVM - with the same requirements as the SonarQube server.

The SonarScanner is the scanner to use when there is no specific scanner for your build system.

## Installation
* Expand the downloaded file into the directory of your choice. We'll refer to it as `$install_directory` in the next steps.
* Update the global settings to point to your SonarQube server by editing `$install_directory/conf/sonar-scanner.properties`:
```
#----- Default SonarQube server
#sonar.host.url=http://localhost:9000
```
* Add the `$install_directory/bin` directory to your path.
* Verify your installation by opening a new shell and executing the command `sonar-scanner -h` (`sonar-scanner.bat -h` on Windows). You should get output like this:

   ```
   usage: sonar-scanner [options]
  
   Options:
     -D,--define <arg>     Define property
     -h,--help             Display help information
     -v,--version          Display version information
     -X,--debug            Produce execution debug output
   ```

If you need more debug information you can add one of the following to your command line: `-X`, `--verbose`, or `-Dsonar.verbose=true`.

## Use
Create a configuration file in the root directory of the project: `sonar-project.properties`
```
sonar-project.properties
# must be unique in a given SonarQube instance
sonar.projectKey=my:project

# --- optional properties ---

# defaults to project key
#sonar.projectName=My project
# defaults to 'not provided'
#sonar.projectVersion=1.0
 
# Path is relative to the sonar-project.properties file. Defaults to .
#sonar.sources=.
 
# Encoding of the source code. Default is default system encoding
#sonar.sourceEncoding=UTF-8
```
Run the following command from the project base directory to launch the analysis:  
`sonar-scanner`

## Sample Projects
To help you get started, simple project samples are available for most languages on github. They can be [browsed](https://github.com/SonarSource/sonar-scanning-examples) or [downloaded](https://github.com/SonarSource/sonar-scanning-examples/archive/master.zip). You'll find them filed under sonarqube-scanner/src.


## Alternatives to sonar-project.properties
If a sonar-project.properties file cannot be created in the root directory of the project, there are several alternatives:

* The properties can be specified directly through the command line. Ex:
```
sonar-scanner -Dsonar.projectKey=myproject -Dsonar.sources=src1
```
* The property project.settings can be used to specify the path to the project configuration file (this option is incompatible with the `sonar.projectBaseDir` property). Ex:
```
sonar-scanner -Dproject.settings=../myproject.properties
```
* The root folder of the project to analyze can be set through the `sonar.projectBaseDir` property since SonarScanner 2.4. This folder must contain a `sonar-project.properties` file if `sonar.projectKey` is not specified on the command line.
Additional analysis parameters can be defined in this project configuration file or through command-line parameters. 

## Alternate Analysis Directory
If the files to be analyzed are not in the directory where the analysis starts from, use the `sonar.projectBaseDir` property to move analysis to a different directory. E.G. analysis begins from `jenkins/jobs/myjob/workspace` but the files to be analyzed are in `ftpdrop/cobol/project1`.
```
sonar-project.properties
sonar.projectBaseDir=/home/ftpdrop/cobol/project1
sonar.sources=src
sonar.cobol.copy.directories=/copy
For more, see the listing of analysis parameters.
```


## Troubleshooting
**Java heap space error or java.lang.OutOfMemoryError**  
Increase the memory via the `SONAR_SCANNER_OPTS` environment variable:
```
export SONAR_SCANNER_OPTS="-Xmx512m"
```
On Windows environments, avoid the double-quotes, since they get misinterpreted and combine the two parameters into a single one.
```
set SONAR_SCANNER_OPTS=-Xmx512m
```

**Unsupported major.minor version**  
Upgrade the version of Java being used for analysis or use one of the native package (that embed its own Java runtime).

**Property missing: `sonar.cs.analyzer.projectOutPaths'. No protobuf files will be loaded for this project.**  
Scanner CLI is not able to analyze .NET projects. Please, use Scanner for MSBuild. If you are running Scanner for MSBuild, ensure that you are not hitting a known limitation.

