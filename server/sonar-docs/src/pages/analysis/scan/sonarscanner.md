---
title: SonarScanner
url: /analysis/scan/sonarscanner/
---

<!-- static -->
<update-center updatecenterkey="scannercli"></update-center>
<!-- /static -->
<!-- embedded -->
[[info]]
| See the [online documentation](https://redirect.sonarsource.com/doc/download-scanner.html) to get more details on the latest version of the scanner and how to download it.
<!-- /embedded -->

The SonarScanner is the scanner to use when there is no specific scanner for your build system.

## Configuring your project
Create a configuration file in your project's root directory called `sonar-project.properties`

```
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

## Running SonarScanner from the zip file
To run SonarScanner from the zip file, follow these steps:

1. Expand the downloaded file into the directory of your choice. We'll refer to it as `$install_directory` in the next steps.
1. Update the global settings to point to your SonarQube server by editing `$install_directory/conf/sonar-scanner.properties`:
```
#----- Default SonarQube server
#sonar.host.url=http://localhost:9000
```
1. Add the `$install_directory/bin` directory to your path.
1. Verify your installation by opening a new shell and executing the command `sonar-scanner -h` (`sonar-scanner.bat -h` on Windows). You should get output like this:

   ```
   usage: sonar-scanner [options]
  
   Options:
     -D,--define <arg>     Define property
     -h,--help             Display help information
     -v,--version          Display version information
     -X,--debug            Produce execution debug output
   ```
If you need more debug information, you can add one of the following to your command line: `-X`, `--verbose`, or `-Dsonar.verbose=true`.

1. Run the following command from the project base directory to launch analysis and pass your [authentication token](/user-guide/user-token/):  
`sonar-scanner -Dsonar.login=myAuthenticationToken`

## Running SonarScanner from the Docker image
To scan using the SonarScanner Docker image, use the following command:

```
docker run \
    --rm \
    -e SONAR_HOST_URL="http://${SONARQUBE_URL}" \
    -e SONAR_LOGIN="myAuthenticationToken" \
    -v "${YOUR_REPO}:/usr/src" \
    sonarsource/sonar-scanner-cli
```

## Scanning C, C++, or ObjectiveC Projects
Scanning projects that contain C, C++, or ObjectiveC code requires some additional analysis steps. You can find full details on the [C/C++/Objective-C](/analysis/languages/cfamily/) language page.

## Sample Projects
To help you get started, simple project samples are available for most languages on GitHub. They can be [browsed](https://github.com/SonarSource/sonar-scanning-examples) or [downloaded](https://github.com/SonarSource/sonar-scanning-examples/archive/master.zip). You'll find them filed under sonarqube-scanner/src.

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
This is configured in `sonar-project.properties` as follows:
```
sonar.projectBaseDir=/home/ftpdrop/cobol/project1
sonar.sources=src
sonar.cobol.copy.directories=/copy
```

[[info]]
| You can configure more parameters. See [Analysis Parameters](/analysis/analysis-parameters/) for details.

## Advanced Docker Configuration

The following sections offer advanced configuration options when running the SonarScanner with Docker. Click the headings to expand the instructions.

[[collapse]]
| ## Running as a non-root user
| You can run the Docker image as a non-root user using the `--user` option. For example, to run as the current user:
| ```
| docker run \
|     --rm \
|     --user="$(id -u):$(id -g)" \
|     -e SONAR_HOST_URL="http://${SONARQUBE_URL}"  \
|     -v "${YOUR_REPO}:/usr/src" \
|     sonarsource/sonar-scanner-cli
| ```
| [[warning]]
| |When running the container as a non-root user you have to make sure the user has read and write access to the directories you are mounting (like your source code or scanner cache directory), otherwise you may encounter permission-related problems.  

[[collapse]]
| ## Caching scanner files
| To prevent SonarScanner from re-downloading language analyzers each time you run a scan, you can mount a directory where the scanner stores the downloads so that the downloads are reused between scanner runs. On some CI systems, you also need to add this directory to your CI cache configuration. 
|
| The following command will store and use cache between runs:
|
| ```
| docker run \
|     --rm \
|     -v ${YOUR_CACHE_DIR}:/opt/sonar-scanner/.sonar/cache \
|     -v ${YOUR_REPO}:/usr/src \
|     -e SONAR_HOST_URL="http://${SONARQUBE_URL}" \
|     sonarsource/sonar-scanner-cli
| ```
|
| You can also change the location of where the scanner puts the downloads with the `SONAR_USER_HOME` environment variable.

[[collapse]]
| ## Using self-signed certificates
| If you need to configure a self-signed certificate for the scanner to communicate with your SonarQube instance, you can use a volume under `/tmp/cacerts` to add it to the containers java trust store: 
|
| ```bash
| docker pull sonarsource/sonar-scanner-cli
| docker run \
|     --rm \
|     -v ${YOUR_CERTS_DIR}/cacerts:/tmp/cacerts \
|     -v ${YOUR_CACHE_DIR}:/opt/sonar-scanner/.sonar/cache \
|     -v ${YOUR_REPO}:/usr/src \
|     -e SONAR_HOST_URL="http://${SONARQUBE_URL}" \
|     sonarsource/sonar-scanner-cli
| ```
|
| Alternatively, you can create your own container that includes the modified `cacerts` file. Create a `Dockerfile` with the following contents:
|
| ```
| FROM sonarsource/sonar-scanner-cli
| COPY cacerts /usr/lib/jvm/default-jvm/jre/lib/security/cacerts
| ```
|
| Then, assuming both the `cacerts` and `Dockerfile` are in the current directory, create the new image with a command such as:
| ```
| docker build --tag our-custom/sonar-scanner-cli .
| ```
|

## Troubleshooting
**Java heap space error or java.lang.OutOfMemoryError**  
Increase the memory via the `SONAR_SCANNER_OPTS` environment variable when running the scanner from a zip file:
```
export SONAR_SCANNER_OPTS="-Xmx512m"
```
In Windows environments, avoid the double-quotes, since they get misinterpreted and combine the two parameters into a single one.
```
set SONAR_SCANNER_OPTS=-Xmx512m
```

**Unsupported major.minor version**  
Upgrade the version of Java being used for analysis or use one of the native package (that embed its own Java runtime).

**Property missing: `sonar.cs.analyzer.projectOutPaths'. No protobuf files will be loaded for this project.**  
Scanner CLI is not able to analyze .NET projects. Please, use the SonarScanner for .NET. If you are running the SonarScanner for .NET, ensure that you are not hitting a known limitation.
