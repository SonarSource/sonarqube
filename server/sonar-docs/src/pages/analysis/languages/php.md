---
title: PHP
url: /analysis/languages/php/
---

<!-- static -->
<!-- update_center:php -->
<!-- /static -->


## Language-Specific Properties

Discover and update the PHP-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > PHP](/#sonarqube-admin#/admin/settings?category=php)**

## Analyze php.ini Files

The PHP analyzer can analyze `php.ini` files with some specific rules (if these rules are activated in your quality profile). `php.ini` files must be part of the project you are analyzing, meaning the `php.ini` files have to be inside the directories listed in `sonar.sources`. 
Rules targeting `php.ini` files can be quickly identified through the ["php-ini"](https://rules.sonarsource.com/php/tag/php-ini) tag set on them.

<!-- sonarqube -->

## Custom Rules

### Overview

The PHP analyzer parses the source code, creates an Abstract Syntax Tree (AST) and then walks through the entire tree. A coding rule is a visitor that is able to visit nodes from this AST.

As soon as the coding rule visits a node, it can navigate its children and log issues if necessary.

### Example Plugin

To get started a sample plugin can be found here: [php-custom-rules](https://github.com/SonarSource/sonar-custom-rules-examples/tree/master/php-custom-rules).

### Writing a Plugin

Custom rules for PHP can be added by writing a SonarQube Plugin and using PHP analyzer APIs.
Here are the step to follow:

#### Create SonarQube Plugin

* create a standard SonarQube plugin project
* attach this plugin to the SonarQube PHP analyzer through the `pom.xml`:
  * add the dependency to the PHP analyzer.
  * add the following line in the sonar-packaging-maven-plugin configuration.
  ```
  <basePlugin>php</basePlugin>
  ```
* implement the following extension points:
  * [Plugin](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/Plugin.html)
  * [RulesDefinition](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/server/rule/RulesDefinition.html) and [PHPCustomRuleRepository](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/visitors/PHPCustomRuleRepository.java), which can be implemented by a single class, to declare your custom rules
* declare the RulesDefinition as an extension in the Plugin extension point.

#### Implement a Rule

* create a class that will hold the implementation of the rule, it should:
  * extend `PHPVisitorCheck` or `PHPSubscriptionCheck`
  * define the rule name, key, tags, etc. with Java annotations.
* declare this class in the `RulesDefinition`.

####  Implementation Details

**Using `PHPVisitorCheck`**

To explore a part of the AST, override a method from the PHPVisitorCheck. For example, if you want to explore "if statement" nodes, override [PHPVisitorCheck#visitIfStatement](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/visitors/PHPVisitorCheck.java#L265) method that will be called each time an [ifStatementTree](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/tree/statement/IfStatementTree.java) node is encountered in the AST.

![](/images/exclamation.svg) When overriding a visit method, you must call the super method in order to allow the visitor to visit the children the node.

**Using `PHPSubscriptionCheck`**

To explore a part of the AST, override [`PHPSubscriptionCheck#nodesToVisit`](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/visitors/PHPSubscriptionCheck.java#L33) by returning the list of the [`Tree#Kind`](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/tree/Tree.java#L124) of node you want to visit. For example, if you want to explore "if statement" nodes the method will return a list containing the element [`Tree#Kind#IF_STATEMENT`](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/tree/Tree.java#L761).

**Create Issues**

From the check, issue can be created by calling [`CheckContext#newIssue`](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/visitors/CheckContext.java#L90) method.

**Testing Checks**

To test custom checks you can use method [`PHPCheckVerifier#verify`](https://github.com/SonarSource/sonar-php/blob/master/php-frontend/src/main/java/org/sonar/plugins/php/api/tests/PHPCheckVerifier.java#L55). You should end each line with an issue with a comment in the following form:

```
// Noncompliant {{Message}}
```

Comment syntax is described [here](https://github.com/SonarSource/sonar-analyzer-commons/blob/master/test-commons/README.md).

<!-- /sonarqube -->

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/)
<!-- sonarqube -->
* [Adding Coding Rules](/extend/adding-coding-rules/)
<!-- /sonarqube -->
