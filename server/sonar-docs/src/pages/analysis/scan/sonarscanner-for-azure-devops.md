---
title: SonarScanner for Azure DevOps
url: /analysis/scan/sonarscanner-for-azure-devops/
---

<update-center updatecenterkey="scannerazure"></update-center>

The [SonarScanner for Azure DevOps](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarqube) makes it easy to integrate analysis into your build pipeline. The extension allows the analysis of all languages supported by SonarQube.

## Compatibility
The SonarScanner for Azure DevOps is compatible with:
* TFS 2017 Update 2+
* TFS 2018
* Azure DevOps Server 2019

## Installation
1. Install the extension [from the marketplace](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarqube). If you are using [Microsoft-hosted build agents](https://docs.microsoft.com/en-us/azure/devops/pipelines/agents/hosted?view=azure-devops) then there is nothing else to install. The extension will work with all of the hosted agents (Windows, Linux, and MacOS).

2. If you are self-hosting the build agents, make sure you have at least the minimum SonarQube-supported version of Java installed.

## Configure
The first thing to do is to declare your SonarQube server as a service endpoint in your Azure DevOps project settings. 

1. Open the Connections page in your Azure DevOps Server project: **Project Settings > Pipelines > Service Connections**.
1. Click **New service connection** and choose **SonarQube**.
1. Specify a **Connection name**, the **Server URL** of your SonarQube Server (including the port if required), and the [Authentication Token](/user-guide/user-token/) to use

Each extension provides three tasks you will use in your build definitions to analyze your projects:

* **Prepare Analysis Configuration** task, to configure all the required settings before executing the build. 
   * This task is mandatory. 
   * In case of .NET solutions or Java projects, it helps to integrate seamlessly with MSBuild, Maven and Gradle tasks.
* **Run Code Analysis** task, to actually execute the analysis of the source code. 
   * This task is not required for Maven or Gradle projects, because scanner will be run as part of the Maven/Gradle build.
* **Publish Quality Gate Result** task, to display the Quality Gate status in the build summary and give you a sense of whether the application is ready for production "quality-wise". 
   * This task is optional. 
   * It can significantly increase the overall build time because it will poll SonarQube until the analysis is complete. Omitting this task will not affect the analysis results on SonarQube - it simply means the Azure DevOps Build Summary page will not show the status of the analysis or a link to the project dashboard on SonarQube.
 
When creating a build definition you can filter the list of available tasks by typing "Sonar" to display only the relevant tasks.

[[collapse]]
| ## Analyzing a .NET solution
|1. In your build definition, add:
|    * At least **Prepare Analysis Configuration** task and **Run Code Analysis** task
|    * Optionally **Publish Quality Gate Result** task
|1. Reorder the tasks to respect the following order:
|    * **Prepare Analysis Configuration** task before any **MSBuild** or **Visual Studio Build** tasks.
|    * **Run Code Analysis** task after the **Visual Studio Test task**.
|    * **Publish Quality Gate Result** task after the **Run Code Analysis** task
|1. Click on the **Prepare Analysis Configuration** build step to configure it:
|   * You must specify the service connection (i.e. SonarQube) to use. You can:
|      * select an existing endpoint from the drop down list
|      * add a new endpoint
|      * manage existing endpoints
|   * Keep **Integrate with MSBuild** checked and specify at least the project key
|      * **Project Key** - the unique project key in {instance}
|      * **Project Name** - the name of the project in {instance}
|      * **Project Version** - the version of the project in {instance}
|1. Click the **Visual Studio Test** task and check the **Code Coverage Enabled** checkbox to process the code coverage and have it imported into {instance}. (Optional but recommended)
|
| Once all this is done, you can trigger a build.

[[collapse]]
| ## Analyzing a Java project with Maven or Gradle
|1. In your build definition, add:
|   * At least **Prepare Analysis Configuration** task|
|   * Optionally **Publish Quality Gate Result** task
|1. Reorder the tasks to respect the following order:
|   * **Prepare Analysis Configuration** task before the **Maven** or **Gradle** task.
|   * **Publish Quality Gate Result** task after the **Maven** or **Gradle** task.
|1. Click on the **Prepare Analysis Configuration** task to configure it:
|   * Select the **SonarQube Server**
|   * Select **Integrate with Maven or Gradle**
|1. On the Maven or Gradle task, in **Code Analysis**, check **Run SonarQube or SonarCloud Analysis**
|
|Once all this is done, you can trigger a build.

[[collapse]]
| ## Analyzing a C/C++/Obj-C project
|In your build pipeline, insert the following steps in the order they appear here. These steps can be interleaved with other steps of your build as long as the following order is followed. All steps have to be executed on the same agent.
|1. Make **Build Wrapper** available on the build agent:\
|   Download and unzip **Build Wrapper** on the build agent (see *Prerequisites* section of *C/C++/Objective-C* page). The archive to download and decompress depends on the platform of the host.\
|   Please, note that:
|   * For the Microsoft-hosted build agent you will need to do it every time (as part of build pipeline), e.g. you can add **PowerShell script** task doing that. This can be done by inserting a **Command Line** task.\
|     Example of PowerShell commands on a windows host:
|     ```
|     Invoke-WebRequest -Uri '<sonarqube_url>/static/cpp/build-wrapper-win-x86.zip' -OutFile 'build-wrapper.zip'
|     Expand-Archive -Path 'build-wrapper.zip' -DestinationPath '.'
|     ```
|     Example of bash commands on a linux host:
|     ```
|     curl '<sonarqube_url>/static/cpp/build-wrapper-linux-x86.zip' --output build-wrapper.zip
|     unzip build-wrapper.zip
|     ```
|     Example of bash commands on a macos host:
|     ```
|     curl '<sonarqube_url>/static/cpp/build-wrapper-macosx-x86.zip' --output build-wrapper.zip
|     unzip build-wrapper.zip
|     ```  
|   * For the self-hosted build agent you can either download it everytime (using the same scripts) or only once (as part of manual setup of build agent).
|2. Add a **Prepare analysis Configuration** task and configure it as follow:\
|   Click on the **Prepare analysis on SonarQube** task to configure it:
|   * Select the **SonarQube Server**
|   * In *Choose the way to run the analysis*, select *standalone scanner* (even if you build with *Visual Studio*/*MSBuild*) 
|   * In *Additional Properties* in the *Advanced* section, add the property `sonar.cfamily.build-wrapper-output` with, as its value, the output directory to which the Build Wrapper should write its results: `sonar.cfamily.build-wrapper-output=<output directory>`
|3. Add a **Command Line** task to run your build.\
|   For the analysis to happen, your build has to be run through a command line so that it can be wrapped-up by the build-wrapper.
|   To do so, 
|   * Run **Build Wrapper** executable. Pass in as the arguments (1) the output directory configured in the previous task and (2) the command that runs a clean build of your project (not an incremental build).\
|   Example of PowerShell commands on a windows host with an *MSBuild* build:
|      ```
|     build-wrapper-win-x86/build-wrapper-win-x86-64.exe --out-dir <output directory> MSBuild.exe /t:Rebuild
|      ```
|      Example of bash commands on a linux host with a *make* build:
|      ```
|      build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir <output directory> make clean all
|      ```
|      Example of bash commands on a macos host with a *xcodebuild* build:
|      ```
|      build-wrapper-macosx-x86/build-wrapper-macos-x86 --out-dir <output directory> xcodebuild -project myproject.xcodeproj -configuration Release clean build
|      ```
|4. Add a **Run Code Analysis** task to run the code analysis and make the results available to SonarQube. Consider running this task right after the previous one as the build environment should not be significantly altered before running the analysis.
|5. Optionally, add a **Publish Quality Gate Result** task.
|
|Once all this is done, you can trigger a build.

[[collapse]]
| ## Analysing other project types
|If you are not developing a .NET application or a Java project, here is the standard way to trigger an analysis:
|
|1. In your build definition, add:
|   * At least **Prepare Analysis Configuration** task and **Run Code Analysis** task
|   * Optionaly **Publish Quality Gate Result** task
|1. Reorder the tasks to respect the following order:
|   1. **Prepare Analysis Configuration**
|   2. **Run Code Analysis**
|   3. **Publish Quality Gate Result**
|1. Click on the **Prepare Analysis Configuration** task to configure it:
|   * Select the **SonarQube Server**
|   * Select **Use standalone scanner**
|   * Then:
|      * Either the SonarQube properties are stored in the (standard) `sonar-project.properties` file in your SCM, and you just have to make sure that "Settings File" correctly points at it. This is the recommended way.
|      * Or you don't have such a file in your SCM, and you can click on **Manually provide configuration** to specify it within your build definition. This is not recommended because it's less portable.
|
|Once all this is done, you can trigger a build.

## Branch and Pull Request analysis

_Branch and Pull Request analysis are available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

### Branches
When a build is run on a branch of your project, the extension automatically configures the analysis to be pushed to the relevant project branch in {instance}. The same build definition can apply to all your branches, whatever type of Git repository you are analyzing,

[[info]]
| If the branch to be analyzed is the **default** branch of your repository, it will be analyzed as the **master** or **main** branch of your SonarQube project. You can [rename](/branches/overview/) it in SonarQube after the first analysis.

If you are working with branches on TFVC projects, you still need to manually specify the branch to be used on {instance}: in the **Prepare Analysis Configuration** task, in the **Additional Properties**, you need to set `sonar.branch.name`.

### Pull Requests
SonarQube can analyze the code of the new features and annotate your pull requests in Azure DevOps with comments to highlight issues that were found.

Pull request analysis is supported for any type of Git repositories. To activate it:

1. Follow the instructions on the [Azure DevOps integration](/analysis/azuredevops-integration/) page for pull request decoration.
1. In the **Branch policies** page of your main development branches (e.g. "master"), add a build policy that runs your build definition

Next time some code is pushed in the branch of a pull request, the build definition will execute a scan on the code and publish the results in SonarQube which will decorate the pull request in Azure DevOps.

_Note : The number of comments posted in a PR is limited to 50. If this limit has been reached, a message will be displayed as a comment, with a link to the rest of the issues on SonarQube. Please note also that this comment will not disappear upon resolution of an issue, but only upon a new build, with less than 50 issues remaining._

## FAQ

**How can I break the build based on the Quality Gate status?**  
We believe that breaking a CI build is not the right approach. 

Instead, we are providing pull request decoration (to make sure that issues aren't introduced at merge time) and we'll soon add a way to check the quality gate as part of a Release process.
