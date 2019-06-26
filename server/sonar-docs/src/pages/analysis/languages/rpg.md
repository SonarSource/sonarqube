---
title: RPG
url: /analysis/languages/rpg/
---

_RPG is available as part of [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://redirect.sonarsource.com/editions/editions.html)._

<!-- static -->
<!-- update_center:rpg -->
<!-- /static -->

## Language-Specific Properties

Discover and update the RPG-specific [properties](/analysis/analysis-parameters/) in: **[Administration > General Settings > RPG](/#sonarqube-admin#/admin/settings?category=rpg)**

## Source Code Extraction

In order to analyze your source code with SonarQube you need to first extract it onto a filesystem. You can use your own tool or an open source tool; SonarSource does not provide any connectors or source code extraction tools.

## RPG Source Format

Depending on your extraction process, your RPG source files may include an extra margin on the left of the 80 columns used for code. This margin is in addition to the standard margin which takes up characters 1-5 in the 80-character source format. The extra margin is controlled through the `sonar.rpg.leftMarginWidth` property. By default, it is set to 12, which is the size of the margin in an IBM “source physical file”. If your RPG source files do not contain such a margin, you should set `sonar.rpg.leftMarginWidth` to `0`.

You can find an [example file](https://raw.githubusercontent.com/SonarSource/sonar-scanning-examples/master/sonarqube-scanner/src/rpg/MYPROGRAM.rpg) illustrating a 12-character margin in our sample project.

You should also make sure to set `sonar.sourceEncoding` to the appropriate encoding. Please check the [documentation of this property](/analysis/analysis-parameters/).

## Free-Form Support

Free-form is supported for C-specs and SQL statements. Free-form is not yet supported for H, F, D and P specs (which were [added in IBM i 7.2](http://www-01.ibm.com/support/knowledgecenter/ssw_ibm_i_72/rzasd/rpgrelv7r2.htm)).

## Custom Rules for RPG

To get started you can [browse](https://github.com/SonarSource/sonar-custom-rules-examples/tree/master/rpg-custom-rules) or [download](https://github.com/SonarSource/sonar-custom-rules-examples/archive/master.zip) a simple plugin.

### Pre-requisites

- JDK 8
- SonarRPG 2.0+

### Creating a Maven Project

You should first create a Maven project: re-using the [pom.xml from the RPG example](https://github.com/SonarSource/sonar-custom-rules-examples/blob/master/rpg-custom-rules/pom.xml) is a good start.

The following dependencies need to be defined in the pom.xml:

- `sonar-plugin-api` to get access to SonarQube APIs
- `sonar-rpg-plugin` to use the APIs of the RPG plugin

### Writing a Custom Rule

Each rule needs to be defined in a class which:

- Implements [`com.sonarsource.rpg.api.checks.Check`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html). Instead of implementing this interface directly, the class can also extend [`VisitorBasedCheck`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html?com/sonarsource/rpg/api/checks/VisitorBasedCheck.html) which makes it easier to target some specific parts of the analyzed source code.
- Has an `org.sonar.check.Rule` annotation to define the key of the rule.

#### Navigating the Syntax Tree

The analyzed source code is represented as a tree structure. The top-most tree is an instance of [`ModuleTree`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html?com/sonarsource/rpg/api/tree/ModuleTree.html) which has references to other trees. Some of the trees represent a group of RPG calculations (for example, an `IF` group is represented as a tree which references the calculations which are executed when the condition is true), some others represent expressions such as `a + b`.

The instance of [`CheckContext`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html?com/sonarsource/rpg/api/checks/CheckContext.html) which is passed to the checks gives a reference to the `ModuleTree` of the analyzed source code. The whole tree structure can be navigated from that object.

Most often, it's easier to extend `VisitorBasedCheck` and to override one or more methods which name starts with visit, e.g. `visitIfGroup`. That way, it's possible to define what should be executed when visiting some specific kinds of trees.

#### Creating Issues

[`CheckContext`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html?com/sonarsource/rpg/api/checks/CheckContext.html) provides methods to create issues either at file level or at line level.

#### Testing the Rule

It's possible to write unit tests for custom rules using `com.sonarsource.rpg.api.test.RpgCheckVerifier`. This utility class executes a custom rule against a given RPG test file. The RPG test file should contain comments denoting lines where issues should be expected:

- if the line ends with "// Noncompliant", `RpgCheckVerifier` expects an issue on that line.
- if the line ends with "// Noncompliant {{my message}}", `RpgCheckVerifier` expects an issue on that line and checks that the issue message is "my message".

The example project contains an [example test class](https://github.com/SonarSource/sonar-custom-rules-examples/blob/master/rpg-custom-rules/src/test/java/com/sonarsource/rpg/example/checks/DataStructureNamingConventionCheckTest.java) and the [associated RPG file](https://github.com/SonarSource/sonar-custom-rules-examples/blob/master/rpg-custom-rules/src/test/resources/data-structure-name.rpg).

### Rules Definition

One class should extend [`com.sonarsource.rpg.api.CustomRulesDefinition`](http://javadocs.sonarsource.org/rpg/apidocs/2.3/index.html?com/sonarsource/rpg/api/checks/Check.html?com/sonarsource/rpg/api/CustomRulesDefinition.html): it should list the classes of the custom rules and use the SonarQube API to define the metadata of these rules: name, HTML description, default severity...

### Plugin Class

The entry point of the custom plugin is a class which lists SonarQube extensions. This list should contain the class created at the previous step.

### Packaging the Custom Plugin

To package your custom plugin, the pom.xml should use `org.sonarsource.sonar-packaging-maven-plugin`, as described in the [documentation explaining how to build a plugin](/extend/developing-plugin/).

In the configuration for `sonar-packaging-maven-plugin`, basePlugin should be set to "rpg".

Building the Maven project will produce a JAR file which can be deployed to a SonarQube server.
