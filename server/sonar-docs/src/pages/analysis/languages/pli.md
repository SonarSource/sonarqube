---
title: PLI
url: /analysis/languages/pli/
---

_PL/I is available as part of [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://redirect.sonarsource.com/editions/editions.html)._

<!-- static -->
<!-- update_center:pli -->
<!-- /static -->

## Language-Specific Properties

Discover and update the PL/I-specific properties in: **[Administration > General Settings > PL/I](/#sonarqube-admin#/admin/settings?category=pl%2Fi)**

## Source Code Extraction

In order to analyze your source code with SonarQube you need to first extract it onto a filesystem. You can use your own tool or an open source tool; SonarSource does not provide any connectors or source code extraction tools.

## Dealing with Includes

There are two possible ways to tell SonarQube where to retrieve the source code referenced by an %INCLUDE statement.

The following syntaxes are supported:

```
%INCLUDE 'C:/temp/myLib.pli'
%INCLUDE ddname(member);
%INCLUDE member; /* With member not enclosed within single or double quotes, i.e. a SYSLIB member */
```

Example:

If you want to interpret:

```
%INCLUDE O (XX02511) as %INCLUDE 'C:/temp/o/XX02511.99IPO';
%INCLUDE lib1 as %INCLUDE 'C:/temp/syslib/lib1.pli';
```

the Ddnames are defined as:

```
sonar.pli.includeDdnames=O,SYSLIB

sonar.pli.includeDdname.O.path=c:/temp/o
sonar.pli.includeDdname.O.suffix=.99IPO

sonar.pli.includeDdname.SYSLIB.path=c:/temp/syslib
sonar.pli.includeDdname.SYSLIB.suffix=.pli
```

Note that the following constructs, involving at least two members, are currently not supported:

```
%INCLUDE member1, member2;
%INCLUDE ddname1(member1), member2;
%INCLUDE member1, ddname1(member2);
%INCLUDE ddname1(member1), ddname2(member2);
```

## Related Pages
<!-- sonarqube -->
* [Adding Coding Rules](/extend/adding-coding-rules/)
<!-- /sonarqube -->
