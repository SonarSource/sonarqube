---
title: SonarScanner for MSBuild
url: /analysis/scan/sonarscanner-for-msbuild/
---

[[info]]
| **Download SonarScanner for MSBuild 4.7.0.2295** - Compatible with SonarQube 6.7+ (LTS)  
| By [SonarSource](https://www.sonarsource.com/) – GNU LGPL 3 – [Issue Tracker](https://github.com/SonarSource/sonar-scanner-msbuild/issues) – [Source](https://github.com/SonarSource/sonar-scanner-msbuild)
|
| [.NET Framework 4.6+](https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/4.7.0.2295/sonar-scanner-msbuild-4.7.0.2295-net46.zip) |
| [.NET Core 2.0+](https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/4.7.0.2295/sonar-scanner-msbuild-4.7.0.2295-netcoreapp2.0.zip) |
| [.NET Core Global Tool](https://www.nuget.org/packages/dotnet-sonarscanner)


The SonarScanner for MSBuild is the recommended way to launch an analysis for projects/solutions using MSBuild or dotnet command as a build tool. It is the result of a [collaboration between SonarSource and Microsoft](http://www.sonarqube.org/announcing-sonarqube-integration-with-msbuild-and-team-build/). 

SonarScanner for MSBuild is distributed as a standalone command line executable, as a extension for <!-- sonarcloud -->[Azure DevOps](/analysis/scan/sonarscanner-for-azure-devops/)<!-- /sonarcloud --><!-- sonarqube -->[Azure DevOps Server](/analysis/scan/sonarscanner-for-azure-devops/)<!-- /sonarqube -->, and as a plugin for [Jenkins](/analysis/scan/sonarscanner-for-jenkins/).

It supports .Net Core multi-platform projects and it can be used on non-Windows platforms.

## Prerequisites
* At least the minimal version of Java supported by your SonarQube server
* The SDK corresponding to your build system:
   *  [.NET Framework v4.6+](https://www.microsoft.com/fr-fr/download/details.aspx?id=53344) - either [Build Tools for Visual Studio 2015 Update 3](https://go.microsoft.com/fwlink/?LinkId=615458) or the [Build Tools for Visual Studio 2017](https://www.visualstudio.com/downloads/)
   * .NET Core 2.0 - [.NET Core SDK 2.0](https://www.microsoft.com/net/learn/get-started/) (for .NET Core version of the scanner)
   * .NET Core 2.1 - [NET Core SDK 2.1.3](https://www.microsoft.com/net/learn/get-started/) (for .NET Core version of the scanner or if you plan to use [.NET Core Global Tool](https://www.nuget.org/packages/dotnet-sonarscanner)


## Installation
### SonarScanner for MSBuild for .NET Framework or .NET Core
* Expand the downloaded file into the directory of your choice. We'll refer to it as `$install_directory` in the next steps.
   * On Windows, you might need to unblock the ZIP file first (Right click on file > Properties > Unblock).
   * On Linux/OSX you may need to set execute permissions on the files in `$install_directory/sonar-scanner-(version)/bin`.
   
* Uncomment, and update the global settings to point to your SonarQube server by editing `$install_directory/SonarQube.Analysis.xml`. Values set in this file will be applied to all analyses of all projects unless overwritten locally.  
Consider setting file system permissions to restrict access to this file.:
```
<SonarQubeAnalysisProperties  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://www.sonarsource.com/msbuild/integration/2015/1">
  <Property Name="sonar.host.url">http://localhost:9000</Property>
  <Property Name="sonar.login">[my-user-token]</Property>
</SonarQubeAnalysisProperties>
```

* Add `$install_directory` to your PATH environment variable.

### Installation of the SonarScanner for MSBuild .NET Core Global Tool
```
dotnet tool install --global dotnet-sonarscanner --version 4.7.0
```
The _--version_ argument is optional. If it is omitted the latest version will be installed.

### On Linux/OSX, if your SonarQube server is secured:

1. Copy the server's CA certs to `/usr/local/share/ca-certificates`
2. Run `sudo update-ca-certificates`

## Use
There are two versions of the SonarScanner for MSBuild.

[[info]]
| Since version 4.7.0 of the Scanner, you can invoke it using arguments with both dash (-) or forward-slash (/) separators.
| Example : SonarScanner.MSBuild.exe begin /k:"project-key" or SonarScanner.MSBuild.exe begin -k:"project-key" will work.

The first version is based on the “classic” .NET Framework. To use it, execute the following commands from the root folder of your project:
```
SonarScanner.MSBuild.exe begin /k:"project-key" <!-- sonarcloud -->/d:sonar.organization="<organization>" /d:sonar.login="<token>" <!-- /sonarcloud -->
MSBuild.exe <path to solution.sln> /t:Rebuild
SonarScanner.MSBuild.exe end <!-- sonarcloud -->/d:sonar.login="<token>" <!-- /sonarcloud -->
```
Note: On Mac OS or Linux, you can also use `mono <path to SonarScanner.MSBuild.exe>`.


The second version is based on .NET Core which has a very similar usage:
```
dotnet <path to SonarScanner.MSBuild.dll> begin /k:"project-key" <!-- sonarcloud -->/d:sonar.organization="<organization>" /d:sonar.login="<token>" <!-- /sonarcloud -->
dotnet build <path to solution.sln>
dotnet <path to SonarScanner.MSBuild.dll> end <!-- sonarcloud -->/d:sonar.login="<token>" <!-- /sonarcloud -->
```
The .NET Core version can also be used as a .NET Core Global Tool.
After installing the Scanner as a global tool as described above it can be invoked as follows:
```
dotnet sonarscanner begin /k:"project-key" <!-- sonarcloud -->/d:sonar.organization="<organization>" /d:sonar.login="<token>" <!-- /sonarcloud -->
dotnet build <path to solution.sln>
dotnet sonarscanner end <!-- sonarcloud -->/d:sonar.login="<token>" <!-- /sonarcloud -->
```

Same as above, if you are targetting a SonarCloud project, will have to add both the organization and a login for authentication.

Notes:

* The .NET Core version of the scanner does not support TFS XAML builds. Apart from that, the two versions of scanner have the same capabilities and command line arguments.
* Single .NET Core project files (csproj or vbproj) could be built and successfully analyzed only if a `<ProjectGuid>unique guid</ProjectGuid>` element is added in the csproj or vbproj XML. The `<ProjectGuid>` element is not required if you build a solution (sln) containing that project.

## Analysis steps
### Begin
The begin step is executed when you add the `begin` command line argument. It hooks into the MSBuild pipeline, downloads SonarQube quality profiles and settings and prepares your project for the analysis.

Command Line Parameters:

Parameter|Description
---|---
`/k:<project-key>`|[required] Specifies the key of the analyzed project in SonarQube
`/n:<project name>`|[optional] Specifies the name of the analyzed project in SonarQube. Adding this argument will overwrite the project name in SonarQube if it already exists.
`/v:<version>`|[recommended] Specifies the version of your project.
<!-- sonarcloud --> `/d:sonar.organization=<organization>`|[required] Specifies the name of the target organization in SonarCloud <!-- /sonarcloud -->
`/d:sonar.login=<username> or <token>`| [optional] Specifies the username or access token to authenticate with to SonarQube. If this argument is added to the begin step, it must also be added on the end step.
`/d:sonar.password=<password>`|[optional] Specifies the password for the SonarQube username in the `sonar.login` argument. This argument is not needed if you use authentication token. If this argument is added to the begin step, it must also be added on the end step.
`/d:sonar.verbose=true`|[optional] Sets the logging verbosity to detailed. Add this argument before sending logs for troubleshooting.
`/d:<analysis-parameter>=<value>`|[optional] Specifies an additional SonarQube [analysis parameter](/analysis/analysis-parameters/), you can add this argument multiple times. 

For detailed information about all available parameters, see [Analysis Parameters](/analysis/analysis-parameters/).

[[warning]]
| ![](/images/exclamation.svg) The "begin" step will modify your build like this:
| * the active `CodeAnalysisRuleSet` will be updated to match the SonarQube quality profile
| * `WarningsAsErrors` will be turned off
|
| If your build process cannot tolerate these changes we recommend creating a second build job for SonarQube analysis.

### Build
Between the `begin` and `end` steps, you need to build your project, execute tests and generate code coverage data. This part is specific to your needs and it is not detailed here.

### End
The end step is executed when you add the "end" command line argument. It cleans the MSBuild hooks, collects the analysis data generated by the build, the test results, the code coverage and then uploads everything to SonarQube.

There are only two additional arguments that are allowed for the end step:

Parameter|Description
---|---
`/d:sonar.login=<username> or <token>`|[optional] This argument is required if it was added to the begin step.
`/d:sonar.password=<password>`|[optional] This argument is required if it was added to the begin step and you are not using an authentication token.

### Known Limitations
* MSBuild versions older than 14 are not supported.
* Web Application projects are supported. Legacy Web Site projects are not. 
* Projects targeting multiple frameworks and using preprocessor directives could have slightly inaccurate metrics (lines of code, complexity, etc.) because the metrics are calculated only from the first of the built targets.


## Excluding projects from analysis
Some project types, such as [Microsoft Fakes](https://msdn.microsoft.com/en-us/library/hh549175.aspx), are automatically excluded from analysis. To manually exclude a different type of project from the analysis, place the following in its .xxproj file.
```
<!-- in .csproj -->
<PropertyGroup>
  <!-- Exclude the project from analysis -->
  <SonarQubeExclude>true</SonarQubeExclude>
</PropertyGroup>
```

## Advanced topics
**Analyzing MSBuild 12 projects with MSBuild 14**  
The Sonar Scanner for MSBuild requires your project to be built with MSBuild 14.0. We recommend installing Visual Studio 2015 update 3 or later on the analysis machine in order to benefit from the integration and features provided with the Visual Studio ecosystem (VSTest, MSTest unit tests, etc.).

Projects targeting older versions of the .NET Framework can be built using MSBuild 14.0 by setting the "TargetFrameworkVersion" MSBuild property as documented by Microsoft:

* [How to: Target a Version of the .NET Framework](https://msdn.microsoft.com/en-us/library/bb398202.aspx)
* [MSBuild Target Framework and Target Platform](https://msdn.microsoft.com/en-us/library/hh264221.aspx)

If you do not want to switch your production build to MSBuild 14.0, you can set up a separate build dedicated to the SonarQube analysis.

**Detection of Test Projects**  
SonarQube analyzes test projects differently from non-test projects, so it is important to correctly classify test projects.

By default, the SonarQube Scanner for MSBuild will detect as test project:

. MSTest unit test projects, thanks to the presence of a well-known project type GUID in .csproj file of such projects.
1. Projects with names ending in "Test" or "Tests". This behavior can be changed by providing the parameter `sonar.msbuild.testProjectPattern` to the begin step (regex follows .NET Regular Expression in a case-sensitive way with the default value `.*Tests?\.(cs|vb)proj$`). This regex is applied against the fullname of the `.csproj` or `.vbproj` which is why it's recommended to keep at the end of your custom regex `\.(cs|vb)proj$`.
To manually classify a project as a test project, mark it with `<SonarQubeTestProject>true</SonarQubeTestProject>`:
```
<!-- in .csproj -->
<PropertyGroup>
  <!-- Mark the project as being a test project -->
  <SonarQubeTestProject>true</SonarQubeTestProject>
</PropertyGroup>
```

**Per-project analysis parameters**
Some analysis parameters can be set for a single MSBuild project by adding them to its .csproj file.
```
<!-- in .csproj -->
<ItemGroup>
  <SonarQubeSetting Include="sonar.stylecop.projectFilePath">
    <Value>$(MSBuildProjectFullPath)</Value>
  </SonarQubeSetting>
</ItemGroup>
```

**Concurrent Analyses on the Same Build Machine**  
Concurrent analyses (i.e. parallel analysis of two solutions on the same build machine using a unique service account) are not supported by default by the Scanner for MSBuild. You can enable it as follows:

1. Locate the folder containing the Scanner for MSBuild
1. Go in the `Targets` folder and copy the folder `SonarQube.Integration.ImportBefore.targets`
1. Paste it under your build tool global `ImportBefore` folder (if the folder doesn't exist, create it).
   * For MSBuild, the path is `<MSBUILD_INSTALL_DIR>\<Version>\Microsoft.Common.targets\ImportBefore` where <MSBUILD_INSTALL_DIR> is:
      *For v14, default path is: `C:\Program Files (x86)\MSBuild\14.0\Microsoft.Common.Targets\ImportBefore`
      * For v15, default path is: `C:\Program Files (x86)\Microsoft Visual Studio\2017\Community\MSBuild\15.0\Microsoft.Common.targets\ImportBefore` (for VS Community Edition)
      * For v16, default path is: `C:\Program Files (x86)\Microsoft Visual Studio\2019\Enterprise\MSBuild\Current\Microsoft.Common.targets` (for VS Community Edition)
   * For dotnet, the path is `<DOTNET_SDK_INSTALL_DIR>\15.0\Microsoft.Common.targets\ImportBefore` where `<DOTNET_SDK_INSTALL_DIR>` can be found using the `dotnet --info` and looking for the Base Path property.

The performance impact of this global installation for projects that aren't analyzed is negligible as this target is only a bootstrapper and will bail out nearly instantaneously when the `.sonarqube` folder is not found under the folder being built.

**Using SonarScanner for MSBuild with a Proxy**  
On build machines that connect to the Internet through a proxy server you might experience difficulties connecting to {instance}. To instruct the Java VM to use the system proxy settings, you need to set the following environment variable before running the SonarScanner for MSBuild:
```
SONAR_SCANNER_OPTS = "-Djava.net.useSystemProxies=true"
```
To instruct the Java VM to use specific proxy settings or when there is no system-wide configuration use the following value:
```
SONAR_SCANNER_OPTS = "-Dhttp.proxyHost=yourProxyHost -Dhttp.proxyPort=yourProxyPort"
```
Where _yourProxyHost_ and _yourProxyPort_ are the hostname and the port of your proxy server. There are additional proxy settings for https, authentication and exclusions that could be passed to the Java VM. For more information see the following article: https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

