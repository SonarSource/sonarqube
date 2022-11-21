---
title: Azure DevOps Integration
url: /analysis/azuredevops-integration/
---
SonarQube's integration with Azure DevOps allows you to maintain code quality and security in your Azure DevOps repositories. It is compatible with both Azure DevOps Server and Azure DevOps Services.

With this integration, you'll be able to:

- **Import your Azure DevOps repositories** - Import your Azure DevOps repositories into SonarQube to easily set up SonarQube projects. 
- **Analyze projects with Azure Pipelines** - Integrate analysis into your build pipeline. Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), the SonarQube Extension running in Azure Pipelines jobs can automatically detect branches or pull requests being built, so you don't need to specifically pass them as parameters to the scanner.
- **Report your Quality Gate status to your pull requests** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) See your Quality Gate and code metric results right in Azure DevOps so you know if it's safe to merge your changes.

## Prerequisites
Integration with Azure DevOps Server requires Azure DevOps Server 2020, Azure DevOps Server 2019, TFS 2018, or TFS 2017 Update 2 (including _Express_ editions).

### Branch Analysis
Community Edition doesn't support the analysis of multiple branches, so you can only analyze your main branch. Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and pull requests.

## Importing your Azure DevOps repositories into SonarQube
Setting up the import of Azure DevOps repositories into SonarQube allows you to easily create SonarQube projects from your Azure DevOps repositories. If you're using [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above, this is also the first step in adding pull request decoration. 

To set up the import of Azure DevOps repositories:  

1. Set your global DevOps Platform settings
2. Add a personal access token for importing repositories  

### Setting your global settings
To import your Azure DevOps repositories into SonarQube, you need to first set your global SonarQube settings. Navigate to **Administration > Configuration > General Settings > DevOps Platform Integrations**, select the **Azure DevOps** tab, and click the **Create configuration** button. Specify the following settings:
 
- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your Azure DevOps configuration at the project level. Use something succinct and easily recognizable.
- **Azure DevOps collection/organization URL** – If you are using Azure DevOps Server, provide your full Azure DevOps collection URL. For example, `https://ado.your-company.com/DefaultCollection`. If you are using Azure DevOps Services, provide your full Azure DevOps organization URL. For example, `https://dev.azure.com/your_organization`.
- **Personal Access Token** – An Azure DevOps user account is used to decorate Pull Requests. We recommend using a dedicated Azure DevOps account with Administrator permissions. You need a [personal access token](https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=tfs-2017&tabs=preview-page) from this account with the scope authorized for **Code > Read & Write** for the repositories that will be analyzed. Administrators can encrypt this token at **Administration > Configuration > Encryption**. See the **Settings Encryption** section of the [Security](/instance-administration/security/) page for more information. 

   This personal access token is used to report your Quality Gate status to your pull requests. You'll be asked for another personal access token for importing projects in the following section.

### Adding a personal access token for importing repositories
After setting your global settings, you can add a project from Azure DevOps by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and selecting  **Azure DevOps**.

Then, you'll be asked to provide a personal access token with `Code (Read & Write)` scope so SonarQube can access and list your Azure DevOps projects. This token will be stored in SonarQube and can be revoked at anytime in Azure DevOps.

After saving your personal access token, you'll see a list of your Azure DevOps projects that you can **set up** to add them to SonarQube. Setting up your projects this way also sets your project settings for pull request decoration. 

For information on analyzing your projects with Azure Pipelines, see the **Analyzing projects with Azure Pipelines** section below.

## Analyzing projects with Azure Pipelines
The SonarQube Extension running in Azure Pipelines jobs can automatically detect branches or pull requests being built, so you don't need to specifically pass them as parameters to the scanner.

[[info]]
| Automatic branch detection is only available when using Git.

### Installing your extension
From Visual Studio Marketplace, install the [SonarQube extension](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarqube) by clicking the **Get it free** button. 

#### **Azure DevOps Server - build agents**

If you are using [Microsoft-hosted build agents](https://docs.microsoft.com/en-us/azure/devops/pipelines/agents/hosted?view=azure-devops) then there is nothing else to install. The extension will work with all of the hosted agents (Windows, Linux, and macOS).

If you are self-hosting the build agents, make sure you have at least the minimum SonarQube-supported version of Java installed.

### Adding a new SonarQube Service Endpoint
After installing your extension, you need to declare your SonarQube server as a service endpoint in your Azure DevOps project settings:

1. In Azure DevOps, go to **Project Settings > Service connections**. 
2. Click **New service connection** and select **SonarQube** from the service connection list.
3. Enter your SonarQube **Server URL**, an [Authentication Token](/user-guide/user-token/), and a memorable **Service connection name**. Then, click **Save**.

### Configuring branch analysis
After adding your SonarQube service endpoint, you'll need to configure branch analysis. You'll use the following tasks in your build definitions to analyze your projects:

- **Prepare Analysis Configuration** - This task configures the required settings before executing the build.

- **Run Code Analysis** - (Not used in Maven or Gradle projects) This task executes the analysis of source code.

- **Publish Quality Gate Result** - this task displays the Quality Gate status in the build summary letting you know if your code meets quality standards for production. This task may increase your build time as your pipeline has to wait for SonarQube to process the analysis report. It is highly recommended but optional.

Select your build technology below to expand the instructions for configuring branch analysis and to see an example `.yml` file.

[[collapse]]
| ## .NET
| 1. In Azure DevOps, create or edit a **Build Pipeline**, and add a new **Prepare Analysis Configuration** task _before_ your build task:
|    - Select the SonarQube server endpoint you created in the **Adding a new SonarQube Service Endpoint** section.
|    - Under **Choose a way to run the analysis**, select **Integrate with MSBuild**.
|    - In the **project key** field, enter your project key.
| 1. Add a new **Run Code Analysis** task _after_ your build task.
| 1. Add a new **Publish Quality Gate Result** on your build pipeline summary.
| 1. Under the **Triggers** tab of your pipeline, check **Enable continuous integration**, and select all of the branches for which you want SonarQube analysis to run automatically.
| 1. Save your pipeline.
|
| **.yml example**:
| ```
| trigger:
| - master # or the name of your main branch
| - feature/*
|
| steps:
| # Prepare Analysis Configuration task
| - task: SonarQubePrepare@5
|   inputs:
|     SonarQube: 'YourSonarqubeServerEndpoint'
|     scannerMode: 'MSBuild'
|     projectKey: 'YourProjectKey'
|
| # Run Code Analysis task
| - task: SonarQubeAnalyze@5
|
| # Publish Quality Gate Result task
| - task: SonarQubePublish@5
|   inputs:
|     pollingTimeoutSec: '300'
| ```

[[collapse]]
| ## Maven or Gradle
| 1. In Azure DevOps, create or edit a **Build Pipeline**, and add a new **Prepare Analysis Configuration** task _before_ your build task:
|    - Select the SonarQube server endpoint you created in the **Adding a new SonarQube Service Endpoint** section.
|    - Under **Choose a way to run the analysis**, select **Integrate with Maven or Gradle**.
|    - Expand the **Advanced section** and replace the **Additional Properties** with the following snippet:
| ```
|    # Additional properties that will be passed to the scanner,
|    # Put one key=value per line, example:
|    # sonar.exclusions=**/*.bin
|    sonar.projectKey=YourProjectKey
| ```
| 2. Edit or add a new Maven or Gradle task
|    - Under **Code Analysis**, check **Run SonarQube or SonarCloud Analysis**.
| 3. Add a new **Publish Quality Gate Result** on your build pipeline summary.
| 4. Under the **Triggers** tab of your pipeline, check **Enable continuous integration**, and select all of the branches for which you want SonarQube analysis to run automatically.
| 5. Save your pipeline.
|
| **.yml example**:
| ```
| trigger:
| - master # or the name of your main branch
| - feature/*
|
| steps:
| # Prepare Analysis Configuration task
| - task: SonarQubePrepare@5
|   inputs:
|     SonarQube: 'YourSonarqubeServerEndpoint'
|     scannerMode: 'Other'
|     extraProperties: 'sonar.projectKey=YourProjectKey'
|
| # Publish Quality Gate Result task
| - task: SonarQubePublish@5
|   inputs:
|     pollingTimeoutSec: '300'
| ```

[[collapse]]
| ## Other (JavaScript, TypeScript, Go, Python, PHP, etc.)
| 1. In Azure DevOps, create or edit a **Build Pipeline**, and add a new **Prepare Analysis Configuration** task _before_ your build task:
|    - Select the SonarQube server endpoint you created in the **Adding a new SonarQube Service Endpoint** section.
|    - Under **Choose a way to run the analysis**, select **Use standalone scanner**.
|    - Select the **Manually provide configuration** mode.
|    - In the **project key** field, enter your project key.
| 1. Add a new **Run Code Analysis** task _after_ your build task.
| 1. Add a new **Publish Quality Gate Result** on your build pipeline summary.
| 1. Under the **Triggers** tab of your pipeline, check **Enable continuous integration**, and select all of the branches for which you want SonarQube analysis to run automatically.
| 1. Save your pipeline.
|
| **.yml example**:
| ```
| trigger:
| - master # or the name of your main branch
| - feature/*
|
| steps:
| # Prepare Analysis Configuration task
| - task: SonarQubePrepare@5
|   inputs:
|     SonarQube: 'YourSonarqubeServerEndpoint'
|     scannerMode: 'CLI'
|     configMode: 'manual'
|     cliProjectKey: 'YourProjectKey'
|
| # Run Code Analysis task
| - task: SonarQubeAnalyze@5
|
| # Publish Quality Gate Result task
| - task: SonarQubePublish@5
|   inputs:
|     pollingTimeoutSec: '300'
| ```

[[collapse]]
| ## Analyzing a C/C++/Obj-C project
| In your build pipeline, insert the following steps in the order they appear here. These steps can be interweaved with other steps of your build as long as the following order is followed. All steps have to be executed on the same agent.
| 
| 1. Make the **Build Wrapper** available on the build agent: 
|    
|    Download and unzip the **Build Wrapper** on the build agent (see the **Prerequisites** section of the [C/C++/Objective-C](/analysis/languages/cfamily/) page). The archive to download and decompress depends on the platform of the host.
|    Please, note that:
|    - For the Microsoft-hosted build agent, you will need to make the **Build Wrapper** available on the build agent every time (as part of the build pipeline). To accomplish this, you can add a **PowerShell script** task by inserting a **Command Line** task.
|     Example of PowerShell commands on a Windows host:
|     ```
|     Invoke-WebRequest -Uri '<sonarqube_url>/static/cpp/build-wrapper-win-x86.zip' -OutFile 'build-wrapper.zip'
|     Expand-Archive -Path 'build-wrapper.zip' -DestinationPath '.'
|     ```
|     Example of bash commands on a Linux host:
|     ```
|     curl '<sonarqube_url>/static/cpp/build-wrapper-linux-x86.zip' --output build-wrapper.zip
|     unzip build-wrapper.zip
|     ```
|     Example of bash commands on a macos host:
|     ```
|     curl '<sonarqube_url>/static/cpp/build-wrapper-macosx-x86.zip' --output build-wrapper.zip
|     unzip build-wrapper.zip
|     ```  
|    - For the self-hosted build agent you can either download it every time (using the same scripts) or only once (as part of manual setup of build agent).
| 1. Add a **Prepare analysis Configuration** task and configure it as follow:
|     Click on the **Prepare analysis on SonarQube** task to configure it:
|    * Select the **SonarQube Server**
|    * In *Choose the way to run the analysis*, select *standalone scanner* (even if you build with *Visual Studio*/*MSBuild*) 
|    * In *Additional Properties* in the *Advanced* section, add the property `sonar.cfamily.build-wrapper-output` with, as its value, the output directory to which the Build Wrapper should write its results: `sonar.cfamily.build-wrapper-output=<output directory>`
| 1. Add a **Command Line** task to run your build.
|    For the analysis to happen, your build has to be run through a command line so that it can be wrapped-up by the build-wrapper.
|    To do so, 
|    * Run **Build Wrapper** executable. Pass in as the arguments (1) the output directory configured in the previous task and (2) the command that runs a clean build of your project (not an incremental build).
|    Example of PowerShell commands on a Windows host with an *MSBuild* build:
|      ```
|     build-wrapper-win-x86/build-wrapper-win-x86-64.exe --out-dir <output directory> MSBuild.exe /t:Rebuild
|      ```
|      Example of bash commands on a Linux host with a *make* build:
|      ```
|      build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir <output directory> make clean all
|      ```
|      Example of bash commands on a macos host with a *xcodebuild* build:
|      ```
|      build-wrapper-macosx-x86/build-wrapper-macos-x86 --out-dir <output directory> xcodebuild -project myproject.xcodeproj -configuration Release clean build
|      ```
| 1. Add a **Run Code Analysis** task to run the code analysis and make the results available to SonarQube. Consider running this task right after the previous one as the build environment should not be significantly altered before running the analysis.
| 1. Add a **Publish Quality Gate Result** task.
| 
| **.yml example**:
| ```
| trigger:
| - master # or the name of your main branch
| - feature/*
|
| steps:
| # Make Build Wrapper available
| - task: Bash@3
|   displayName: Download Build Wrapper
|   inputs:
|     targetType: inline
|     script: >
|       curl  '<SONARQUBE_HOST>/static/cpp/build-wrapper-linux-x86.zip' --output build-wrapper.zip
|       unzip build-wrapper.zip
|
| # Prepare Analysis Configuration task
| - task: SonarQubePrepare@5
|   inputs:
|     SonarQube: 'YourSonarqubeServerEndpoint'
|     scannerMode: 'CLI'
|     configMode: 'manual'
|     cliProjectKey: 'YourProjectKey'
|     extraProperties: "sonar.cfamily.build-wrapper-output=bw_output"
| # Command Line task to run your build.
| - task: Bash@3
|    displayName: Bash Script
|    inputs:
|      targetType: inline
|      script: >
|        ./build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw_output <Your build command>
|
| # Run Code Analysis task
| - task: SonarQubeAnalyze@5
|
| # Publish Quality Gate Result task
| - task: SonarQubePublish@5
|   inputs:
|     pollingTimeoutSec: '300'
| ```
|  *Note: You need to choose your correct image and adapt the correct wrapper depending on the agent os. See above example to have the correct wrapper.*

### Running your pipeline
Commit and push your code to trigger the pipeline execution and SonarQube analysis. New pushes on your branches (and pull requests if you set up pull request analysis) trigger a new analysis in SonarQube.

### Maintaining pull request code quality and security 
Using pull requests allows you to prevent unsafe or substandard code from being merged with your main branch. The following branch policies can help you maintain your code quality and safety by analyzing code and identifying issues in all of the pull requests on your project. These policies are optional, but they're highly recommended so you can quickly track, identify, and remediate issues in your code.

#### **Ensuring your pull requests are automatically analyzed**
Ensure all of your pull requests get automatically analyzed by adding a [build validation branch policy](https://docs.microsoft.com/en-us/azure/devops/pipelines/repos/azure-repos-git#pr-triggers) on the target branch.

#### **Preventing pull request merges when the Quality Gate fails**
Prevent the merge of pull requests with a failed Quality Gate by adding a `SonarQube/quality gate` [status check branch policy](https://docs.microsoft.com/en-us/azure/devops/repos/git/pr-status-policy) on the target branch.
 
[[info]]
| Projects configured as part of a mono repository cannot use this status check branch policy to prevent pull request merges.
 
Check out this [![YouTube link](/images/youtube.png) video](https://www.youtube.com/watch?v=be5aw9_7bBU) for a quick overview on preventing pull requests from being merged when they are failing the Quality Gate.

## Reporting your Quality Gate status in Azure DevOps
After you've set up SonarQube to import your Azure DevOps repositories as shown in the **Importing your Azure DevOps repositories into SonarQube** above, SonarQube can report your Quality Gate status and analysis metrics directly to your Azure DevOps pull requests.

To do this, add a project from Azure DevOps by clicking the **Add project** button in the upper-right corner of the **Projects** homepage and select **Azure DevOps** from the drop-down menu.

Then, follow the steps in SonarQube to analyze your project. SonarQube automatically sets the project settings required to show your Quality Gate in your pull requests.

[[info]]
| To report your Quality Gate status in your pull requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for pull request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

If you're creating your projects manually or adding Quality Gate reporting to an existing project, see the following section.

### Reporting your Quality Gate status in manually created or existing projects
SonarQube can also report your Quality Gate status to Azure DevOps pull requests for existing and manually-created projects. After setting your global settings as shown in the **Importing your Azure DevOps repositories into SonarQube** section above, set the following project settings at **Project Settings > General Settings > DevOps Platform Integration**:

- **Project name**
- **Repository name**

### Advanced configuration

[[collapse]]
| ## Reporting your Quality Gate status on pull requests in a mono repository
|
| _Reporting Quality Gate statuses to pull requests in a mono repository setup is supported starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html)._
|
| In a mono repository setup, multiple SonarQube projects, each corresponding to a separate project within the mono repository, are all bound to the same Azure DevOps repository. You'll need to set up each SonarQube project that's part of a mono repository to report your Quality Gate status.
|
| You need to set up projects that are part of a mono repository manually as shown in the **Displaying your Quality Gate status in manually created or existing project** section above. You also need to set the **Enable mono repository support** setting to true at **Project Settings > General Settings > DevOps Platform Integration**.
|
| After setting your project settings, ensure the correct project is being analyzed by adjusting the analysis scope and pass your project names to the scanner. See the following sections for more information.
|
| ### Ensuring the correct project is analyzed
| You need to adjust the analysis scope to make sure SonarQube doesn't analyze code from other projects in your mono repository. To do this set up a **Source File Inclusion** for your  project at **Project Settings > Analysis Scope** with a pattern that will only include files from the appropriate folder. For example, adding `./MyFolderName/**/*` to your inclusions would only include analysis of code in the `MyFolderName` folder. See [Narrowing the Focus](/project-administration/narrowing-the-focus/) for more information on setting your analysis scope.
|
| ### Passing project names to the scanner
| Because of the nature of a mono repository, SonarQube scanners might read all project names of your mono repository as identical. To avoid having multiple projects with the same name, you need to pass the `sonar.projectName` parameter to the scanner. For example, if you're using the Maven scanner, you would pass `mvn sonar:sonar -Dsonar.projectName=YourProjectName`.

[[collapse]]
| ## Configuring multiple DevOps Platform instances
| SonarQube can report your Quality Gate status to multiple DevOps Platform instances. To do this, you need to create a configuration for each DevOps Platform instance and assign that configuration to the appropriate projects. 
|
| - As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each DevOps Platform. 
|
| - Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can create multiple configurations for each DevOps Platform. If you have multiple configurations of the same DevOps Platform connected to SonarQube, you have to create projects manually.

[[collapse]]
| ## Linking issues
| When adding a Quality Gate status to your pull requests, individual issues will be linked to their SonarQube counterparts automatically. For this to work correctly, you need to set the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) correctly. Otherwise, the links will default to `localhost`.

## FAQ

**Missing Build Agent Capability**	

If you add a Windows Build Agent and install a non-oracle Java version on it, the agent will fail to detect a needed capability for the SonarQube Azure DevOps plugin. If you are sure that the `java` executable is available in the `PATH` environment variable, you can add the missing capability manually by going to **your build agent > capabilities > user capabilities > add capability**. Here, you can add the key, value pair java, and null which should allow the SonarQube plugin to be scheduled on that build agent.	
This Bug has been reported to the Microsoft Team with [azure-pipelines-agent#2046](https://github.com/microsoft/azure-pipelines-agent/issues/2046) but is currently not followed up upon.

### Interaction details between SonarQube and Azure

When you run a Sonar analysis for a pull request, each Sonar issue will be a comment on the Azure DevOps pull request. If the Azure DevOps instance is configured correctly and you set an issue in SonarQube to 'resolved', the Azure DevOps pull request comment will automatically be resolved. Likewise, when you fix an issue in the code and run the analysis build another time, the issue will be resolved in Sonar and deleted in Azure DevOps.
