---
title: C/C++/Objective-C Test Coverage
url: /analysis/test-coverage/c-family-test-coverage/
---

SonarQube supports the reporting of test coverage information as part of the analysis of your C/C++/Objective-C project.

However, SonarQube does not generate the coverage report itself.
Instead, you must set up a third-party tool to produce the report as part of your build process.
You then need to configure your analysis to tell the SonarScanner where the report is located so that it can pick it up and send it to SonarQube, where it will be displayed on your project dashboard along with the other analysis metrics.


## Adjust your setup

To enable coverage, you need to:

* Adjust your build process so that the coverage tool generates the report(s).
  This is done just after your unit tests as part of the clean build required to run analysis.
* Make sure that the coverage tool writes its report file to a defined path in the build environment.
* Configure the scanning step of your build so that the scanner picks up the report file from that defined path.


## Add coverage to your build process

For C/C++/Objective-C projects, SonarQube supports a number of coverage tools.
Each has an associated analysis parameter that must be set to the location of the coverage report that is produced by the tool.
The parameters are:

* `sonar.cfamily.llvm-cov.reportPath`
* `sonar.cfamily.vscoveragexml.reportsPath`
* `sonar.cfamily.gcov.reportsPath`
* `sonar.cfamily.bullseye.reportPath`
* `sonar.coverageReportPaths`

Assuming that you have already set up your project, you will have seen the example projects (_without coverage_) referenced in the in-product tutorials: [sonarsource-cfamily-examples](https://github.com/orgs/sonarsource-cfamily-examples/).

In the same GitHub organization, you will also find example repositories that provide guidance on how to _add coverage_ to an already-configured project.
These examples do not explicitly describe every possible combination of tooling and platform but do cover the most significant variants.
You may need to adapt them slightly:

* [Visual Studio Coverage example on GitHub Actions](https://github.com/sonarsource-cfamily-examples/windows-msbuild-vscoverage-gh-actions-sc)

* [Visual Studio Coverage example on Azure DevOps](https://github.com/sonarsource-cfamily-examples/windows-msbuild-vscoverage-azure-sc)

* [XCode Coverage example](https://github.com/sonarsource-cfamily-examples/macos-xcode-coverage-gh-actions-sc)

* [llvm-cov example](https://github.com/sonarsource-cfamily-examples/linux-cmake-llvm-cov-gh-actions-sc)

* [gcovr example](https://github.com/sonarsource-cfamily-examples/linux-cmake-gcovr-gh-actions-sc)

* [gcov example](https://github.com/sonarsource-cfamily-examples/linux-autotools-gcov-travis-sc)

These examples include the major free-to-use coverage tools for C/C++/Objective-C (VS Coverage, XCode Coverage, LLVM-COV, GCOVR, and GCOV). For information on the popular commercial Bullseye product,  see https://www.bullseye.com/.


## Coverage parameters can be set in multiple places

As with other analysis parameters, the coverage-related parameters for C/C++/Objective-C projects can be set in multiple places:

* On the command line of the scanner invocation using the `-D` or `--define` switch. This is what is done in the examples above, inside the `build.yml` files of each example.

* In the `sonar-project.properties` file.

* In the SonarQube interface under

  **_Your Project_ > Project Settings > General Settings > Languages > C/C++/Objective-C > Coverage**

  for project-level settings, and

  **Administration > Configuration > General Settings > Languages > C/C++/Objective-C > Coverage**

  for global settings (applying to all projects).


## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).
