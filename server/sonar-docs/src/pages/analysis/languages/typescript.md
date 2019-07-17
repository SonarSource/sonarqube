---
title: TypeScript
url: /analysis/languages/typescript/
---

<!-- static -->
<!-- update_center:typescript -->
<!-- /static -->


## Prerequisites

In order to analyze TypeScript code, you need to have Node.js >= 8 installed on the machine running the scan. Set property `sonar.typescript.node` to an absolute path to Node.js executable, if standard `node` is not available.

Also make sure to have [TypeScript](https://www.npmjs.com/package/typescript) as a project dependency or dev dependency. If it's not the case, add it:
```
cd <your-project-folder>
npm install -D typescript
```
If you can't have TypeScript as a project dependency you can set your `NODE_PATH` variable to point to your globally installed TypeScript (but this is generally discouraged by the Node.js documentation).

## Language-Specific Properties

Discover and update the TypeScript-specific properties in: **<!-- sonarcloud -->Project <!-- /sonarcloud -->[Administration > General Settings > TypeScript](/#sonarqube-admin#/admin/settings?category=typescript)**

## Supported Frameworks and Versions
* TypeScript >= 2.2

## Rule Profiles

There are 2 built-in rule profiles for TypeScript: `Sonar way` (default) and `Sonar way Recommended`.
* `Sonar way` profile is activated by default. It defines a trimmed list of high-value/low-noise rules useful in almost any TS development context.
* `Sonar way Recommended` contains all rules from `Sonar way`, plus more rules that mandate high code readability and long-term project evolution.

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/) (LCOV format)
* [Importing External Issues](/analysis/external-issues/) (TSLint)
* [SonarTS Plugin for TSLint](https://www.npmjs.com/package/tslint-sonarts)
* [Sample TypeScript Project](https://github.com/SonarSource/SonarTS-example/)
