---
title: JavaScript
url: /analysis/languages/javascript/
---

<!-- static -->
<!-- update_center:javascript -->
<!-- /static -->


## Prerequisites

In order to analyze JavaScript code, you need to have Node.js >= 8 installed on the machine running the scan. Set property `sonar.nodejs.executable` to an absolute path to Node.js executable, if standard `node` is not available.
 
## Language-Specific Properties

Discover and update the JavaScript-specific properties in: **<!-- sonarcloud -->Project <!-- /sonarcloud -->[Administration > General Settings > JavaScript](/#sonarqube-admin#/admin/settings?category=javascript)**

## Supported Frameworks and Versions
* ECMAScript 5 / ECMAScript 2015 (ECMAScript 6) / ECMAScript 2016 / ECMAScript 2017
* React JSX
* Vue.js
* Flow

## Rule Profiles

There are 2 built-in rule profiles for JavaScript: `Sonar way` (default) and `Sonar way Recommended`.
* `Sonar way` profile is activated by default. It defines a trimmed list of high-value/low-noise rules useful in almost any JS development context.
* `Sonar way Recommended` contains all rules from `Sonar way`, plus more rules that mandate high code readability and long-term project evolution.

<!-- sonarqube -->
## Custom rules
[[warning]]
| ![](/images/exclamation.svg) This feature is deprecated
### Overview

The JavaScript Analyzer parses the source code, creates an Abstract Syntax Tree (AST) and then walks through the entire tree. A coding rule is a visitor that is able to visit nodes from this AST.

As soon as the coding rule visits a node, it can navigate the tree around the node and log issues if necessary.

### Create SonarQube Plugin
Custom rules for JavaScript can be added by writing a SonarQube Plugin and using JavaScript analyzer APIs.

To get started a sample plugin can be found here: [javascript-custom-rules](https://github.com/SonarSource/sonar-custom-rules-examples/tree/master/javascript-custom-rules).
Here are the step to follow:

* Create a standard SonarQube plugin project
* Attach this plugin to the SonarQube JavaScript analyzer through the `pom.xml`:
  * Add the dependency to the JavaScript analyzer.
  * Add the following line in the sonar-packaging-maven-plugin configuration.
  ```
  <basePlugin>javascript</basePlugin>
  ```
* Implement the following extension points:
  * [Plugin](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/Plugin.html)
  * [RulesDefinition](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/server/rule/RulesDefinition.html) 
  * `CustomRuleRepository`, this interface registers rule classes with JavaScript plugin, so they are invoked during analysis of JavaScript files.
* Declare `RulesDefinition` as an extension in the `Plugin` extension point.

You can implement both `RulesDefinition` and `CustomRulesRepository` in a single class.

### Implement a Rule

* Create a class that will hold the implementation of the rule. It should:
  * Extend `DoubleDispatchVisitorCheck` or `SubscriptionVisitorCheck`
  * Define the rule name, key, tags, etc. with Java annotations.
* Declare this class in the `RulesDefinition`.

###  Implementation Details

#### Using DoubleDispatchVisitorCheck
`DoubleDispatchVisitorCheck` extends `DoubleDispatchVisitor` which provide a set of methods to visit specific tree nodes (these methods' names start with `visit`). To explore a part of the AST, override the required method(s). For example, if you want to explore `if` statement nodes, override the `DoubleDispatchVisitor#visitIfStatement` method that will be called each time an `IfStatementTree` node is encountered in the AST.

![](/images/exclamation.svg) When overriding a visit method, you must call the `super` method in order to allow the visitor to visit the rest of the tree.

#### Using SubscriptionVisitorCheck
`SubscriptionVisitorCheck` extends `SubscriptionVisitor`. To explore a part of the AST, override `SubscribtionVisitor#nodesToVisit()` by returning the list of the `Tree#Kind` of node you want to visit. For example, if you want to explore `if` statement nodes the method will return a list containing the element `Tree#Kind#IF_STATEMENT`.

#### Create issues
Use these methods to log an issue:

* `JavaScriptCheck#addIssue(tree, message)` creates and returns an instance of `PreciseIssue`. In the SonarQube UI this issue will highlight all code corresponding to the tree passed as the first parameter. To add cost (effort to fix) or secondary locations provide these values to your just-created instance of `PreciseIssue`.
* `JavaScriptCheck#addIssue(issue)` creates and returns the instance of `Issue`. Use this method to create non-standard issues (e.g. for a file-level issue instantiate `FileIssue`).

#### Check context
Check context is provided by `DoubleDispatchVisitorCheck` or `SubscriptionVisitorCheck` by calling the `JavaScriptCheck#getContext` method. Check context provides you access to the root tree of the file, the file itself and the symbol model (information about variables).

#### Test rule
To test the rule you can use `JavaScriptCheckVerifier#verify()` or `JavaScriptCheckVerifier#issues()`. To be able to use these methods add a dependency to your project:
```
<dependency>
  <groupId>org.sonarsource.javascript</groupId>
  <artifactId>javascript-checks-testkit</artifactId>
  <version>XXX</version>
  <scope>test</scope>
</dependency>
```

### API Changes
#### SonarJS 4.2.1
* `CustomJavaScriptRulesDefinition` is deprecated. Implement extension `RulesDefinition` and `CustomRuleRepository` instead.

#### SonarJS 4.0
* Method `TreeVisitorContext#getFile()` is removed.

<!-- /sonarqube -->

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/) (LCOV format)
* [Importing External Issues](/analysis/external-issues/) (ESLint)
* [SonarJS Plugin for ESLint](https://github.com/SonarSource/eslint-plugin-sonarjs)
<!-- sonarqube -->
* [Adding Coding Rules](/extend/adding-coding-rules/)
<!-- /sonarqube -->
