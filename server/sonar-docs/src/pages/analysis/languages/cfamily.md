---
title: C/C++/Objective-C
url: /analysis/languages/cfamily/
---

<!-- sonarqube -->
_C/C++/Objective-C analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://redirect.sonarsource.com/editions/editions.html)._
<!-- /sonarqube -->

<!-- static -->
<!-- update_center:cpp -->
<!-- /static -->


C/C++/Objective-C analysis is officially registered as [CWE Compatible](https://cwe.mitre.org/compatible/).

## Supported Compilers, Language Standards and Operating Systems
* Any version of Clang, GCC and Microsoft C/C++ compilers
* Any version of Intel compiler for Linux and macOS
* ARM5 and ARM6 compilers
* IAR compiler for ARM, Renesas RL78, Renesas RX, Renesas V850, Texas Instruments MSP430 and for 8051
* Compilers based wholly on GCC including for instance Linaro GCC and WindRiver GCC are also supported
* C89, C99, C11, C++03, C++11, C++14 and C++17 standards
* GNU extensions
* Microsoft Windows, Linux and macOS for runtime environment

## Language-Specific Properties

Discover and update the C/C++/Objective-C specific properties in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > C / C++ / Objective-C](/#sonarqube-admin#/admin/settings?category=c+%2F+c%2B%2B+%2F+objective-c)**

## Prerequisites
### Build Wrapper
Analysis of C/C++/Objective-C projects requires the **SonarQube Build Wrapper**. It gathers all the configuration required for correct analysis of C/C++/Objective-C projects (such as macro definitions, include directories, …) directly from your project's build process. The Build Wrapper does not impact your build; it merely eavesdrops on it and writes what it learns into files a directory you specify. 

<!-- sonarqube -->
You can download the *Build Wrapper* directly from your SonarQube server, so that its version perfectly matches your version of the plugin. 
* Download *Build Wrapper* for Linux from [{SonarQube URL}/static/cpp/build-wrapper-linux-x86.zip](/#sonarqube#/static/cpp/build-wrapper-linux-x86.zip)
* Download *Build Wrapper* for macOS from [{SonarQube URL}/static/cpp/build-wrapper-macosx-x86.zip](/#sonarqube#/static/cpp/build-wrapper-macosx-x86.zip)
* Download *Build Wrapper* for Windows from [{SonarQube URL}/static/cpp/build-wrapper-win-x86.zip](/#sonarqube#/static/cpp/build-wrapper-win-x86.zip)
<!-- /sonarqube -->
<!-- sonarcloud -->
You can download the *Build Wrapper* directly from SonarCloud:
* [Download *Build Wrapper* for Linux](https://sonarcloud.io/static/cpp/build-wrapper-linux-x86.zip)
* [Download *Build Wrapper* for macOS](https://sonarcloud.io/static/cpp/build-wrapper-macosx-x86.zip)
* [Download *Build Wrapper* for Windows](https://sonarcloud.io/static/cpp/build-wrapper-win-x86.zip)
<!-- /sonarcloud -->


Unzip the downloaded *Build Wrapper* and configure it in your `PATH` because doing so is just more convenient.

### SonarQube Scanner
Analysis of C/C++/Objective-C projects requires the [*SonarScanner*](https://redirect.sonarsource.com/doc/install-configure-scanner.html) CLI.

## Analysis Steps
* If you use macOS or Linux operating systems make sure your source tree is in a directory called `src`
* Add execution of the *Build Wrapper* as a prefix to your usual build command (the examples below use `make`, `xcodebuild` and `MSBuild`, but any build tool that performs a full build can be used)
   ```
   // example for linux
   build-wrapper-linux-x86-64 --out-dir build_wrapper_output_directory make clean all 
   // example for macOS
   build-wrapper-macosx-x86 --out-dir build_wrapper_output_directory xcodebuild clean build
   // example for Windows
   build-wrapper-win-x86-64.exe --out-dir  build_wrapper_output_directory MSBuild.exe /t:Rebuild
   ```
* In the *sonar-project.properties* file at the root of your project add the property `sonar.cfamily.build-wrapper-output` with the path to the *Build Wrapper* output directory relative to the project directory (`build_wrapper_output_directory` in these examples). 

   Sample *sonar-project.properties*:
   ```
   sonar.projectKey=myFirstProject
   sonar.projectName=My First C++ Project
   sonar.projectVersion=1.0
   sonar.sources=src
   sonar.cfamily.build-wrapper-output=build_wrapper_output_directory
   sonar.sourceEncoding=UTF-8
   ```
* Execute the SonarScanner (`sonar-scanner`) from the root directory of the project
   ```
   sonar-scanner
   ```
* Follow the link provided at the end of the analysis to browse your project's quality metrics in the UI

## Multithreaded Code Scan 

It is possible to use all the cores available on the machine running the code scan. This can be activated by configuring the property `sonar.cfamily.threads` at the scanner level. Its default value is 1.

* This feature must not be activated on a machine with only 1 core.

* The analyzer will not guess which value is most suitable for your project. It's up to you to test and find the best value.

* If a build machine with 2 cores is already configured to potentially run two code scans at the same time, there is no guarantee that configuring `sonar.cfamily.threads=2` will bring the expected performance benefits. It can even be worse than running with the default value.

* The multithreaded execution requires more memory than single-threaded execution.

* A machine with 64 cores configured with `sonar.cfamily.threads=64` is not certain to bring a large performance gain compared to a machine with 32 cores. The performance tradeoff will vary depending on the machine, project and setup, so some testing will be required to decide if the performance gain justifies moving to a larger machine.

## Solution with a Mix of C# and C++

When you have a Solution made of C++ and C#, to both use the SonarQube *Build Wrapper* and have an accurate analysis of the C# code, you must to use the [SonarScanner for MSBuild](https://github.com/SonarSource/sonar-scanner-msbuild).
Note that in this scenario source code stored in shared folders, not considered as a "Project" by Visual Studio, won't be scanned.

* Download and install both the [SonarScanner for MSBuild](https://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html) and the SonarQube *Build Wrapper* (see *Prerequisites* section).
* Execute the SonarQube Scanner for MSBuild `begin` step
* Add execution of *Build Wrapper* to your normal MSBuild build command
* Execute the SonarQube Scanner for MSBuild `end` step to complete the analysis

For example:
```
SonarScanner.MSBuild.exe begin /k:"cs-and-cpp-project-key" /n:"My C# and C++ project" /v:"1.0" /d:sonar.cfamily.build-wrapper-output="bw_output"
build-wrapper-win-x86-64.exe --out-dir bw_output MSBuild.exe /t:Rebuild
SonarScanner.MSBuild.exe end
```

## Measures for Header Files
Each time we analyze a header file as part of a compilation unit, we compute for this header the measures: statements, functions, classes, cyclomatic complexity and cognitive complexity. That means that each measure may be computed more than once for a given header. In that case, we store the largest value for each measure.

## Building with Bazel

[Bazel](https://www.bazel.build/) recommends that you use the [`--batch`](https://docs.bazel.build/versions/master/bazel-user-manual.html#flag--batch) option when running in a Continuous Build context. When using the *BuildWrapper*, you are in such context. Also, you need to deactivate the ["sandbox"](https://docs.bazel.build/versions/master/bazel-user-manual.html#sandboxing) mechanism of *Bazel* so that the compiled file paths could be retrieved after the compilation phase.
Here is an example of the *BuildWrapper* command with Bazel parameters on macOS:
```
build-wrapper-macosx-x86 --out-dir bw bazel
  --batch
  --spawn_strategy=standalone
  --genrule_strategy=standalone
  --bazelrc=/dev/null build
  //main:hello-world
```

## Related Pages
* [Test Coverage & Execution](/analysis/coverage/) (CPPUnit, GCOV, llvm-cov, Visual Studio, Bullseye)
* [Sample project](https://github.com/SonarSource/sonar-scanning-examples/tree/master/sonarqube-scanner-build-wrapper-linux) for C/C++ (Linux)
* [Sample project](https://github.com/SonarSource/sonar-scanning-examples/tree/master/objc-llvm-coverage) for Objective-C
* [SonarScanner for Azure Devops](https://redirect.sonarsource.com/doc/install-configure-scanner-tfs-ts.html) (analyzing Visual C++ project)
