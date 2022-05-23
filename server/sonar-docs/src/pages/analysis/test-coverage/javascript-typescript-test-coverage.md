---
title: Javascript/Typescript Test Coverage
url: /analysis/test-coverage/javascript-typescript-test-coverage/
---

SonarQube supports the reporting of test coverage information as part of the analysis of your JS/TS project.

However, SonarQube does not generate the coverage report itself.
Instead, you must set up a third-party tool to produce the report as part of your build process.
You then need to configure your analysis to tell the SonarScanner where the report is located so that it can pick it up and send it to SonarQube, where it will be displayed on your project dashboard along with the other analysis metrics.

For JS/TS projects, SonarQube directly supports all coverage tools that produce reports in the LCOV format.
Additionally, a generic coverage format is also supported if you wish to use an unsupported tool (though you will have to convert its output to the generic format yourself).

In this section, we discuss the directly supported JS/TS LCOV coverage feature.
For information on the generic format, see Generic Test Data.


## Adjusting your setup

To enable coverage, you need to:

* Adjust your build process so that the coverage tool runs before the scanner step.
* Make sure that the coverage tool writes its report file to a defined path in the build environment.
* Configure the scanning step of your build so that the scanner picks up the report file from that defined path.


## Adding coverage to your build process

The details of setting up coverage within your build process depend on which tools you are using.

The following illustrates how to do this for a JS/TS project that uses Yarn and Jest in the GitHub Actions CI.
Simply add the following to your build.yml file:

```
- name: Install dependencies
   run: yarn
- name: Test and coverage
   run: yarn jest --coverage
```

The resulting file should look something like this:

```
name: Build
on:
 push:
   branches:
     - master
 pull_request:
   types: [opened, synchronize, reopened]
jobs:
  sonarqube:
    name: sonarqube
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0 
      - name: Install dependencies
        run: yarn
      - name: Test and coverage
        run: yarn jest --coverage
      - name: SonarQube Scan
        uses: SonarSource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

First, you install all your project dependencies and then invoke jest with the `--coverage` option to run your tests and write out the coverage data to a file.

If, as here, you do not specify an output file, the default `./coverage/lcov.info` is used.

If you are using a different package manager or a different testing tool, these details will be different.

The essential requirements are that the tool produces its report in the LCOV format and writes it to a place from which the scanner can then pick it up.


## Adding the coverage analysis parameter

The next step is to add `sonar.javascript.lcov.reportPaths` to your analysis parameters.
This parameter must be set to the path of the report file produced by your coverage tool.
The path can be either absolute or relative to the project root.
In this example, that path is set to the default produced by Jest: `./coverage/lcov.info`.
It is set in the `sonar-project.properties` file, located in the project root:

```
sonar.projectKey=<project-key>
...
sonar.javascript.lcov.reportPaths=./coverage/lcov.info
```


## Coverage parameters can be set in multiple places

As with other analysis parameters, the coverage-related parameters for JavaScript/TypeScript projects (`sonar.javascript.lcov.reportPaths`) can be set in multiple places:

* In the `sonar-project.properties` file, as mentioned above.

* On the command line of the scanner invocation using the `-D` or `--define` switch, for example:

  `sonar-scanner -Dsonar.javascript.lcov.reportPaths=./coverage/lcov.info`

* In the SonarQube interface under

  **_Your Project_ > Project Settings > General Settings > Languages > JavaScript/TypeScript > Tests and Coverage**

  for project-level settings, and

  **Administration > Configuration > General Settings > Languages > JavaScript/TypeScript > Tests and Coverage**

  for global settings (applying to all projects).


## Same parameter for JavaScript and TypeScript

The parameter `sonar.typescript.lcov.reportPaths` was formerly used for typescript coverage.
This parameter has been deprecated.

The parameter `sonar.javascript.lcov.reportPaths` is now used for both JavaScript and TypeScript.

## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).
