---
title: Overview
url: /analysis/test-coverage/overview/
---

Test coverage reports and test execution reports are important metrics in assessing the quality of your code.
Test coverage reports tell you what percentage of your code is covered by your test cases.
Test execution reports tell you which tests have been run and their results.

SonarQube itself does not calculate coverage.
To include coverage results in your analysis, you must set up a third-party coverage tool and configure SonarQube to import the results produced by that tool.

Below, you'll find guidelines and resources, as well as language- and tool-specific analysis parameters. 


## General guidelines

Before importing test coverage, you need to have the appropriate SonarScanner configured to perform code analysis as part of your build pipeline.

To enable coverage reporting, you must then do the following:

1. Set up your coverage tool to run as part of your build pipeline.
   Your coverage tool should be set up to run _before_ the SonarScanner analysis.
2. Configure the coverage tool so that the location and format of the output report files match what the SonarScanner expects.
3. Configure the analysis parameters of the SonarScanner so that it can import the report files.

Now, on each build of your project, your coverage tool should perform its analysis and output its results to one or more files (usually one for test coverage and one for test execution).
Then, the SonarScanner, as part of its analysis process, will import those files and send the results to SonarQube.


## Coverage support

SonarQube directly supports the import of coverage data in formats native to a variety of tools for a variety of languages. It also supports the import of a [generic format](/analysis/generic-test/) that can be used as a target for custom conversion of reports from tools that are not directly supported.


### Detailed guides

Detailed guides for the following languages are provided in this section:

* [Java Test Coverage](/analysis/test-coverage/java-test-coverage/)
* [JavaScript/TypeScript Test Coverage](/analysis/test-coverage/javascript-typescript-test-coverage/)
* [.NET Test Coverage](/analysis/test-coverage/dotnet-test-coverage/)
* [Python Test Coverage](/analysis/test-coverage/python-test-coverage/)
* [PHP Test Coverage](/analysis/test-coverage/php-test-coverage/)
* [C/C++/Objective-C Test Coverage](/analysis/test-coverage/c-family-test-coverage/)


### Generic format

See [Generic Test Data](/analysis/generic-test/) for information on how the generic format works.


### Test coverage parameters

See [Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/) for a reference on all coverage-related analysis parameters.


## Test execution reports

This section is about _test coverage reports_, that is, reports that describe the percentage of your code that is tested by your test suite during a build.

Test execution reports are a separate feature.
These describe which tests within your tests suite are executed during a build.
For details, see [Test Execution Parameters](/analysis/test-coverage/test-execution-parameters/).
