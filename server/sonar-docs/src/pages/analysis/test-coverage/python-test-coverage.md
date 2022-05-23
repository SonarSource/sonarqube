---
title: Python Test Coverage
url: /analysis/test-coverage/python-test-coverage/
---

SonarQube supports the reporting of test coverage information as part of the analysis of your Python project.

However, SonarQube does not generate the coverage report itself.
Instead, you must set up a third-party tool to produce the report as part of your build process.
You then need to configure your analysis to tell the SonarScanner where the report is located so that it can pick it up and send it to SonarQube, where it will be displayed on your project dashboard along with the other analysis metrics.


## Adjust your setup

To enable coverage, you need to:

* Adjust your build process so that the coverage tool runs _before_ the scanner report generation step runs.
* Make sure that the coverage tool writes its report file to a defined path in the build environment.
* Configure the scanning step of your build so that the scanner picks up the report file from that defined path.


## Add coverage to your build process

The details of setting up coverage within your build process depend on which tools you are using.
In our example we use:

* Tox, to configure the tests
* Pytest, to execute the tests 
* Coverage, (the Coverage.py tool,) to measure code coverage, and 
* GitHub Actions, to perform the build.

In this example, we invoke `pytest` and use the `pytest-cov` plugin which, in turn, uses Coverage.py.
Simply add the text below to the `tox.ini` file at the root of your project:

```
[tox]
envlist = py39
skipsdist = True

[testenv]
deps =
  pytest
  coverage
commands = pytest --cov=my_project --cov-report=xml --cov-config=tox.ini --cov-branch

[coverage:run]
```

Alternatively, we could start the test by invoking the Coverage.py tool (the command `coverage`) with the `pytest` invocation as an argument, like this:

```
[tox]
envlist = py39
skipsdist = True

[testenv]
deps =
    pytest
    coverage
commands =
    coverage run -m pytest
    coverage xml

[coverage:run]
relative_files = True
source = my_project/
branch = True
```

Note that we specify `relative_files = True` in the `tox.ini` file to ensure that your coverage results are correctly parsed.

The following shows how to configure the GitHub Actions build file for your Python project so that it works in conjunction with the `tox.ini` configuration file described above to generate code coverage.
Your  `build.yml` file should look something like this:

```
name: Build
on:
  push:
    branches:
      - main
  pull_request:
    types: [opened, synchronize, reopened]
jobs:
  sonarqube:
    name: SonarQube
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0
      - name: Setup Python
        uses: actions/setup-python@v2
        with:
          python-version: ${{ matrix.python }}
      - name: Install tox and any other packages
        run: pip install tox
      - name: Run tox
        run: tox -e py
      - name: SonarQube Scan
        uses: SonarSource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

First of all, install all of your project dependencies and then invoke `tox` to run your tests and generate a coverage report file.

If, as here, you do not specify an output file, the scanner will look for report paths located under the default
`.coverage-reports/*coverage-*.xml.`

If you are using a different package manager or a different testing tool these details will be different.

The essential requirements are that the tool produces its report in the Cobertura XML format and writes it to a place from which the scanner can then pick it up.


## Add the coverage analysis parameter

The next step is to add `sonar.python.coverage.reportPaths` to your analysis parameters.
This parameter must be set to the path of the report file produced by your coverage tool.
In this example, that path is set to the default produced by Coverage.py.
It is set in the `sonar-project.properties` file, located in the project root:

```
sonar.projectKey=<sonar-project-key>
...
sonar.python.coverage.reportPaths=coverage.xml
```


## Coverage parameters can be set in multiple places

As with other analysis parameters, `sonar.pyhton.coverage.reportPaths` can be set in multiple places:

* In the `sonar-project.properties` file, as mentioned above.

* On the command line of the scanner invocation using the `-D` or `--define`
  switch, for example:

  `sonar-scanner -Dsonar.python.coverage.reportPaths=coverage.xml`

* In the SonarQube interface under

  **_Your Project_ > Project Settings > General Settings > Languages > Python > Tests and Coverage**

  for project-level settings, and

  **Administration > Configuration > General Settings > Languages > Python > Tests and Coverage**

  for global settings (applying to all projects).


## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).
