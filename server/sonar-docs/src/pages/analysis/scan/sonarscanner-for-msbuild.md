---
url: /analysis/scan/sonarscanner-for-msbuild/
title: SonarScanner for .NET
---

<!-- sonarqube -->
<!-- static -->
<update-center updatecenterkey="scannermsbuild"></update-center>
<!-- /static -->
<!-- embedded -->
[[info]]
| See the [online documentation](https://redirect.sonarsource.com/doc/download-scanner-msbuild.html) to get more details on the latest version of the scanner and how to download it.
<!-- /embedded -->
<!-- /sonarqube -->

<!-- sonarcloud -->
[[info]]
| **Download SonarScanner for .NET** - [Issue Tracker](https://github.com/SonarSource/sonar-scanner-msbuild/issues) - [Source](https://github.com/SonarSource/sonar-scanner-msbuild)
|
| [Standalone executables](https://github.com/SonarSource/sonar-scanner-msbuild/releases) |
| [.NET Core Global Tool](https://www.nuget.org/packages/dotnet-sonarscanner)
<!-- /sonarcloud -->

[[info]]
| Since version 5.0, the SonarScanner for MSBuild is now the SonarScanner for .NET. 
| documentation is updated with that new name, artifacts and links will remain with the old name for now.

The SonarScanner for .NET is the recommended way to launch an analysis for projects/solutions using MSBuild or dotnet command as a build tool. It is the result of a [collaboration between SonarSource and Microsoft](https://www.sonarqube.org/announcing-sonarqube-integration-with-msbuild-and-team-build/).

SonarScanner for .NET is distributed as a standalone command line executable, as an extension for <!-- sonarcloud -->[Azure DevOps](/analysis/scan/sonarscanner-for-azure-devops/)<!-- /sonarcloud --><!-- sonarqube -->[Azure DevOps Server](/analysis/scan/sonarscanner-for-azure-devops/)<!-- /sonarqube -->, and as a plugin for [Jenkins](/analysis/scan/sonarscanner-for-jenkins/).

It supports .Net Core on every platform (Windows, macOS, Linux).

## Prerequisites
<!-- sonarqube -->
* At least the minimal version of Java supported by your SonarQube server
<!-- /sonarqube -->
<!-- sonarcloud -->
* Java 11 or greater
<!-- /sonarcloud -->
* The SDK corresponding to your build system:
<!-- sonarqube -->
   * If you are using the .NET Framework version of the scanner you will need [.NET Framework v4.6 or above](https://dotnet.microsoft.com/en-us/download/dotnet-framework). For commercial versions of SonarQube to benefit from security analysis you will need [.NET Framework v4.7.2 or above](https://dotnet.microsoft.com/en-us/download/dotnet-framework)
<!-- /sonarqube -->
<!-- sonarcloud -->
   * If you are using the .NET Framework version of the scanner you will need [.NET Framework v4.6 or above](https://dotnet.microsoft.com/en-us/download/dotnet-framework), however to benefit from security analysis you will need [.NET Framework v4.7.2 or above](https://dotnet.microsoft.com/en-us/download/dotnet-framework) 
<!-- /sonarcloud -->
   * If you are using the .NET version of the scanner or the [.NET Core Global Tool](https://www.nuget.org/packages/dotnet-sonarscanner) you will need [.NET Core SDK 2.0 or above](https://dotnet.microsoft.com/en-us/download/dotnet)

[[info]]
| The flavor used to compile the Scanner for .NET (either .NET Framework, .NET Core or .NET) is independent of the .NET version the 
| project you want to analyze has been built with. Concretely, you can analyze .NET Core code with the .NET Framework version of 
| the Scanner. It's only relevant depending on your OS, and on the versions of .NET SDKs that are installed on your build machine.

## Installation

### Standalone executable

* Expand the downloaded file into the directory of your choice. We'll refer to it as `$install_directory` in the next steps.
  * On Windows, you might need to unblock the ZIP file first (right-click **file > Properties > Unblock**).
  * On Linux/OSX you may need to set execute permissions on the files in `$install_directory/sonar-scanner-(version)/bin`.

* Uncomment, and update the global settings to point to <!-- sonarqube -->your SonarQube server<!-- /sonarqube --><!-- sonarcloud -->SonarCloud<!-- /sonarcloud --> by editing `$install_directory/SonarQube.Analysis.xml`. Values set in this file will be applied to all analyses of all projects unless overwritten locally.  
Consider setting file system permissions to restrict access to this file.:

```xml
<SonarQubeAnalysisProperties  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://www.sonarsource.com/msbuild/integration/2015/1">
  <Property Name="sonar.host.url"><!-- sonarqube -->http://localhost:9000<!-- /sonarqube --><!-- sonarcloud -->https://sonarcloud.io<!-- /sonarcloud --></Property>
  <Property Name="sonar.login">[my-user-token]</Property>
</SonarQubeAnalysisProperties>
```

* Add `$install_directory` to your PATH environment variable.

### .NET Core Global Tool

```bash
dotnet tool install --global dotnet-sonarscanner --version x.x.x
```

The _--version_ argument is optional. If it is omitted the latest version will be installed. Full list of releases is available on the [NuGet page](https://www.nuget.org/packages/dotnet-sonarscanner)

.NET Core Global Tool is available from .NET Core 2.1+

<!-- sonarqube -->
### On Linux/OSX, if your SonarQube server is secured

1. Copy the server's CA certs to `/usr/local/share/ca-certificates`
2. Run `sudo update-ca-certificates`
<!-- /sonarqube -->

## Use

[[info]]
| You can invoke the Scanner using arguments with both dash (-) or forward-slash (/) separators.
| Example : SonarScanner.MSBuild.exe begin /k:"project-key" or SonarScanner.MSBuild.exe begin -k:"project-key" will work.

There are two versions of the SonarScanner for .NET. In the following commands, you need to pass an [authentication token](/user-guide/user-token/) using the `sonar.login` property.

### "Classic" .NET Framework Invocation

The first version is based on the "classic" .NET Framework. To use it, execute the following commands from the root folder of your project:

```
SonarScanner.MSBuild.exe begin /k:"project-key" <!-- sonarcloud -->/o:"<organization>" <!-- /sonarcloud -->/d:sonar.login="<token>"
MSBuild.exe <path to solution.sln> /t:Rebuild
SonarScanner.MSBuild.exe end /d:sonar.login="<token>"
```

Note: On macOS or Linux, you can also use `mono <path to SonarScanner.MSBuild.exe>`.

### .NET Core and .NET Core Global Tool Invocation

The second version is based on .NET Core which has a very similar usage:

```bash
dotnet <path to SonarScanner.MSBuild.dll> begin /k:"project-key" <!-- sonarcloud -->/o:"<organization>" <!-- /sonarcloud -->/d:sonar.login="<token>"
dotnet build <path to solution.sln>
dotnet <path to SonarScanner.MSBuild.dll> end /d:sonar.login="<token>" 
```

The .NET Core version can also be used as a .NET Core Global Tool.
After installing the Scanner as a global tool as described above it can be invoked as follows:

```bash
dotnet tool install --global dotnet-sonarscanner
dotnet sonarscanner begin /k:"project-key" <!-- sonarcloud -->/o:"<organization>" <!-- /sonarcloud -->/d:sonar.login="<token>"
dotnet build <path to solution.sln>
dotnet sonarscanner end /d:sonar.login="<token>"
```

In summary, the invocation of the SonarScanner for .NET will depend on the scanner flavor:

 Scanner Flavor | Invocation
 --- | ---
 .NET 5 | `dotnet <path to SonarScanner.MSBuild.dll>` etc.
 .NET Core Global Tool | `dotnet sonarscanner begin` etc.
 .NET Core 2.0+ | `dotnet <path to SonarScanner.MSBuild.dll>` etc.
 .NET Framework 4.6+|`SonarScanner.MSBuild.exe begin` etc.

Notes:

* The .NET Core version of the scanner does not support TFS XAML builds and automatic finding/conversion of Code Coverage files. Apart from that, all versions of the Scanner have the same capabilities and command line arguments.

## Analysis steps
### Begin
The begin step is executed when you add the `begin` command line argument. It hooks into the build pipeline, downloads {instance} quality profiles and settings and prepares your project for the analysis.

Command Line Parameters:

<!-- sonarcloud -->

Parameter|Description
---|---
`/k:<project-key>`|[required] Specifies the key of the analyzed project in {instance}
`/n:<project name>`|[optional] Specifies the name of the analyzed project in {instance}. Adding this argument will overwrite the project name in {instance} if it already exists.
`/v:<version>`|[recommended] Specifies the version of your project.
`/o:<organization>`|[required] Specifies the name of the target organization in SonarCloud.
`/d:sonar.login=<token> or <username>`| [recommended] Specifies the [authentication token](/user-guide/user-token/) or username used to authenticate with to {instance}. If this argument is added to the begin step, it must also be added to the end step.
`/d:sonar.password=<password>`|[optional] Specifies the password for the {instance} username in the `sonar.login` argument. This argument is not needed if you use authentication token. If this argument is added to the begin step, it must also be added on the end step.
`/d:sonar.verbose=true`|[optional] Sets the logging verbosity to detailed. Add this argument before sending logs for troubleshooting.
`/d:sonar.dotnet.excludeTestProjects=true`|[optional] Excludes Test Projects from analysis. Add this argument to improve build performance when issues should not be detected in Test Projects.
`/d:<analysis-parameter>=<value>`|[optional] Specifies an additional {instance} [analysis parameter](/analysis/analysis-parameters/), you can add this argument multiple times.
`/s:<custom.analysis.xml>`|[optional] Overrides the `$install_directory/SonarQube.Analysis.xml`. You need to give the absolute path to the file.
<!-- /sonarcloud -->

<!-- sonarqube -->

Parameter|Description
---|---
`/k:<project-key>`|[required] Specifies the key of the analyzed project in {instance}
`/n:<project name>`|[optional] Specifies the name of the analyzed project in {instance}. Adding this argument will overwrite the project name in {instance} if it already exists.
`/v:<version>`|[recommended] Specifies the version of your project.
`/d:sonar.login=<token> or <username>`| [recommended] Specifies the [authentication token](/user-guide/user-token/) or username used to authenticate with to {instance}. If this argument is added to the begin step, it must also be added to the end step.
`/d:sonar.password=<password>`|[optional] Specifies the password for the {instance} username in the `sonar.login` argument. This argument is not needed if you use authentication token. If this argument is added to the begin step, it must also be added on the end step.
`/d:sonar.clientcert.path=<ClientCertificatePath>`|[optional] Specifies the path to a client certificate used to access {instance}. The certificate must be password protected.
`/d:sonar.clientcert.password=<ClientCertificatePassword>`|[optional] Specifies the password for the client certificate used to access {instance}. Required if a client certificate is used.
`/d:sonar.verbose=true`|[optional] Sets the logging verbosity to detailed. Add this argument before sending logs for troubleshooting.
`/d:sonar.dotnet.excludeTestProjects=true`|[optional] Excludes Test Projects from analysis. Add this argument to improve build performance when issues should not be detected in Test Projects.
`/d:<analysis-parameter>=<value>`|[optional] Specifies an additional {instance} [analysis parameter](/analysis/analysis-parameters/), you can add this argument multiple times.
`/s:<custom.analysis.xml>`|[optional] Overrides the `$install_directory/SonarQube.Analysis.xml`. You need to give the absolute path to the file.
<!-- /sonarqube -->

For detailed information about all available parameters, see [Analysis Parameters](/analysis/analysis-parameters/).

[[warning]]
| ![](/images/exclamation.svg) The "begin" step will modify your build like this:
| * the active `CodeAnalysisRuleSet` will be updated to match the {instance} quality profile
| * `WarningsAsErrors` will be turned off
|
| If your build process cannot tolerate these changes we recommend creating a second build job for {instance} analysis.

### Build
Between the `begin` and `end` steps, you need to build your project, execute tests and generate code coverage data. This part is specific to your needs and it is not detailed here.

### End
The end step is executed when you add the "end" command line argument. It cleans the MSBuild/dotnet build hooks, collects the analysis data generated by the build, the test results, the code coverage and then uploads everything to {instance}

There are only two additional arguments that are allowed for the end step:

<!-- sonarcloud -->
Parameter|Description
---|---
`/d:sonar.login=<token> or <username>`| This argument is required if it was added to the begin step.
`/d:sonar.password=<password>`| This argument is required if it was added to the begin step and you are not using an authentication token.
<!-- /sonarcloud -->

<!-- sonarqube -->
Parameter|Description
---|---
`/d:sonar.login=<token> or <username>`| This argument is required if it was added to the begin step.
`/d:sonar.password=<password>`| This argument is required if it was added to the begin step and you are not using an authentication token.
`/d:sonar.clientcert.password=<ClientCertificatePassword>`|This argument is required if it was added to the begin step. Specifies the password for the client certificate used to access {instance}.
<!-- /sonarqube -->

### Known Limitations

* MSBuild versions older than 14 are not supported.
* Web Application projects are supported. Legacy Web Site projects are not.
* Projects targeting multiple frameworks and using preprocessor directives could have slightly inaccurate metrics (lines of code, complexity, etc.) because the metrics are calculated only from the first of the built targets.

## Code Coverage

In a Azure DevOps / TFS environment, test files are automatically retrieved following this search
* Search for .trx files in any TestResults folder located under the $Build.SourcesDirectory path
* If not found, then a fallback search is made against $Agent.TempDirectory

Once trx files have been found, their `.coverage` counterpart are searched as well and the scanner tries to convert them to `.coveragexml` files that will be uploaded to {instance}.
CodeCoverage.exe tool is used for that, and the scanner also needs to find a path to that tool, following this search path
* Search for the presence of `VsTestToolsInstallerInstalledToolLocation` environment variable, set by the VsTestToolsPlatformInstaller task or by the user
* If not found, search for either the presence of that tool in well-known installation path, or via the registry.

As stated above, this will work only with the .NET 4.6 flavor of the Scanner.

## Excluding projects from analysis

Some project types, such as [Microsoft Fakes](https://msdn.microsoft.com/en-us/library/hh549175.aspx), are automatically excluded from analysis. To manually exclude a different type of project from the analysis, place the following in its .xxproj file.

```xml
<!-- in .csproj -->
<PropertyGroup>
  <!-- Exclude the project from analysis -->
  <SonarQubeExclude>true</SonarQubeExclude>
</PropertyGroup>
```

## Advanced topics

**Analyzing MSBuild 12 projects with MSBuild 14**  
The Sonar Scanner for .NET requires your project to be built with MSBuild 14.0. We recommend installing Visual Studio 2015 update 3 or later on the analysis machine in order to benefit from the integration and features provided with the Visual Studio ecosystem (VSTest, MSTest unit tests, etc.).

Projects targeting older versions of the .NET Framework can be built using MSBuild 14.0 by setting the "TargetFrameworkVersion" MSBuild property as documented by Microsoft:

* [How to: Target a Version of the .NET Framework](https://msdn.microsoft.com/en-us/library/bb398202.aspx)
* [MSBuild Target Framework and Target Platform](https://msdn.microsoft.com/en-us/library/hh264221.aspx)

For example, if you want to build a .NET 3.5 project, but you are using a newer MSBuild version:

```
MSBuild.exe /t:Rebuild /p:TargetFramework=net35
```

If you do not want to switch your production build to MSBuild 14.0, you can set up a separate build dedicated to the {instance} analysis.

**Detection of test projects**

You can read a full description on that subject on our wiki [here](https://github.com/SonarSource/sonar-scanner-msbuild/wiki/Analysis-of-product-projects-vs.-test-projects).

**Per-project analysis parameters**
Some analysis parameters can be set for a single MSBuild project by adding them to its .csproj file.

```xml
<!-- in .csproj -->
<ItemGroup>
  <SonarQubeSetting Include="sonar.stylecop.projectFilePath">
    <Value>$(MSBuildProjectFullPath)</Value>
  </SonarQubeSetting>
</ItemGroup>
```

**Analyzing languages other than C# and VB**

For newer SDK-style projects (used by .NET Core, .NET 5, and later), the SonarScanner for .NET will analyze all file types that are supported by the available language plugins unless explicitly excluded. 

For older-style projects, the scanner will only analyze files that are listed in the `.csproj` or `.vbproj` project file. Normally this means that only C# and VB files will be analyzed. To enable the analysis of other types of files, include them in the project file.

More specifically, any files included by an element of one of the `ItemTypes` in
[this list](https://github.com/SonarSource/sonar-scanner-msbuild/blob/master/src/SonarScanner.MSBuild.Tasks/Targets/SonarQube.Integration.targets#L112)
will be analyzed automatically. For example, the following line in your `.csproj` or `.vbproj` file

```
<Content Include="foo\bar\*.js" />
```

will enable the analysis of all JS files in the directory `foo\bar` because `Content` is one of the `ItemTypes` whose includes are automatically analyzed.

You can also add `ItemTypes` to the default list by following the directions [here](https://github.com/SonarSource/sonar-scanner-msbuild/blob/master/src/SonarScanner.MSBuild.Tasks/Targets/SonarQube.Integration.targets#L75).

You can check which files the scanner will analyze by looking in the file .sonarqube\out\sonar-project.properties after MSBuild has finished.

**Using SonarScanner for .NET with a Proxy**  
On build machines that connect to the Internet through a proxy server you might experience difficulties connecting to {instance}. To instruct the Java VM to use the system proxy settings, you need to set the following environment variable before running the SonarScanner for .NET:

```bash
SONAR_SCANNER_OPTS = "-Djava.net.useSystemProxies=true"
```

To instruct the Java VM to use specific proxy settings or when there is no system-wide configuration use the following value:

```bash
SONAR_SCANNER_OPTS = "-Dhttp.proxyHost=yourProxyHost -Dhttp.proxyPort=yourProxyPort"
```
Where _yourProxyHost_ and _yourProxyPort_ are the hostname and the port of your proxy server. There are additional proxy settings for HTTPS, authentication and exclusions that could be passed to the Java VM. For more information see the following article: https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html.

You also need to set the appropriate proxy environment variables used by .NET. `HTTP_PROXY`, `HTTPS_PROXY`, `ALL_PROXY`, and `NO_PROXY` are all supported. You can find more details [here](https://docs.microsoft.com/en-us/dotnet/api/system.net.http.httpclient.defaultproxy?view=net-5.0).

## Known issues

**I have multiple builds in the same pipeline, each of them getting analyzed even if the Run Code Analysis has already been executed**

We don't uninstall the global `ImportBefore` targets to support concurrent analyses on the same machine. Main effect is that if you build a solution where a .sonarqube folder is located nearby, then the sonar-dotnet analyzer will be executed along your build task.

To avoid that, you can disable the targets file by adding a build parameter:
```
msbuild /p:SonarQubeTargetsImported=true
dotnet build -p:SonarQubeTargetsImported=true
```
