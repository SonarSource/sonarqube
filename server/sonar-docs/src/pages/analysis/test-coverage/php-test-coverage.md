---
title: PHP Test Coverage
url: /analysis/test-coverage/php-test-coverage/
---

SonarQube supports the reporting of test coverage information as part of the analysis of your PHP project. 

However, SonarQube does not generate the coverage report itself.
Instead, you must set up a third-party tool to produce the report as part of your build process.
You then need to configure your analysis to tell the SonarScanner where the report is located so that it can pick it up and send it to SonarQube, where it will be displayed on your project dashboard along with the other analysis metrics.

For PHP projects, we recommend PHPUnit for testing and coverage reporting.


## Adjust your setup

To enable coverage, you need to:

* Adjust your build process so that the coverage tool runs _before_ the scanner report generation step runs.
* Make sure that the coverage tool writes its report file to a defined path in the build environment.
* Configure the scanning step of your build so that the scanner picks up the report file from that defined path.


## Add coverage to your build process

The details of setting up coverage within your build process depend on which tools you are using.
In our example below we use:

* Composer, as a package manager
* PHPUnit with Xdebug, to execute the tests 
* Clover to do the coverage reporting, and 
* GitHub Actions to perform the build.

Simply add the following to your `.github/workflows/build.yml` file: 

```
- name: Setup PHP with Xdebug
    uses: shivammathur/setup-php@v2
    with:
      php-version: '8.1'
      coverage: xdebug

- name: Install dependencies with composer
    run: composer update --no-ansi --no-interaction --no-progress

- name: Run tests with phpunit/phpunit
    run: vendor/bin/phpunit --coverage-clover=coverage.xml

- name: Fix code coverage paths
          run: sed -i 's@'$GITHUB_WORKSPACE'@/github/workspace/@g' coverage.xml
```

The resulting file should look something like this:

```
name: build
on:
  - pull_request
  - push
jobs:
  tests:
      name: Tests
      runs-on: ubuntu-latest
      steps:
        - name: Checkout
          uses: actions/checkout@v2
          with:
            fetch-depth: 0
        - name: Setup PHP with Xdebug
          uses: shivammathur/setup-php@v2
          with:
            php-version: '8.1'
            coverage: xdebug
        - name: Install dependencies with composer
          run: composer update --no-ansi --no-interaction --no-progress
        - name: Run tests with phpunit/phpunit
          run: vendor/bin/phpunit --coverage-clover=coverage.xml
        - name: Fix code coverage paths
          run: sed -i 's@'$GITHUB_WORKSPACE'@/github/workspace/@g' coverage.xml
        - name: SonarQube Scan
          uses: SonarSource/sonarqube-scan-action@master
          env:
            SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
            SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

First you install all your project dependencies using Composer as a package manager and then invoke _PHPUnit with XDebug_ to run your tests and generate a coverage report file.

The essential requirements are that the tool produces its report in the `clover.xml` format and writes it to a place from which the scanner can then pick it up.


## Add the coverage analysis parameter

The next step is to add `sonar.php.coverage.reportPaths` to your analysis parameters.
This parameter must be set to the path of the report file produced by your coverage tool.
In this example, that path is set to the default.
It is set in the `sonar-project.properties` file, located in the project root:

```
sonar.projectKey=<sonar-project-key>
...
sonar.php.coverage.reportPaths=coverage.xml
```


## Coverage parameters can be set in multiple places

As with other analysis parameters, `sonar.php.coverage.reportPaths` can be set in multiple places:

* In the `sonar-project.properties` file, as mentioned above.

* On the command line of the scanner invocation using the `-D` or `--define`
  switch, for example:

  `sonar-scanner -Dsonar.php.coverage.reportPaths=coverage.xml`

* In the SonarQube interface under

  **_Your Project_ > Project Settings > General Settings > Languages > PHP > PHPUnit**

  for project-level settings, and

  **Administration > Configuration > General Settings > Languages > PHP > PHPUnit**

  for global settings (applying to all projects).

## See Also

[Test Coverage Parameters](/analysis/test-coverage/test-coverage-parameters/).
