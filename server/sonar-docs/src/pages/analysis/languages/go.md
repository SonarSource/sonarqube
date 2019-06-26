---
title: Go
url: /analysis/languages/go/
---

<!-- static -->
<!-- update_center:go -->
<!-- /static -->



## Prerequisites

* SonarQube Scanner should run on a x86-64 Windows, macOS or Linux 64bits machine
* You need the [Go](https://golang.org/) installation on the scan machine only if you want to import coverage data

## Language-Specific Properties

You can discover and update the Go-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Go](/#sonarqube-admin#/admin/settings?category=go)**

## "sonar-project.properties" Sample

Here is a good first version of a `sonar-project.properties`, correctly excluding "vendor" directories and categorizing files as "main" or "test":

```
  sonar.projectKey=com.company.projectkey1
  sonar.projectName=My Project Name

  sonar.sources=.
  sonar.exclusions=**/*_test.go,**/vendor/**

  sonar.tests=.
  sonar.test.inclusions=**/*_test.go
  sonar.test.exclusions=**/vendor/**
```

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/)
* [Importing External Issues](/analysis/external-issues/) (GoVet, GoLint, GoMetaLinter)
