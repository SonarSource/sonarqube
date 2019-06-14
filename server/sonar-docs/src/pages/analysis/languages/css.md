---
title: CSS
url: /analysis/languages/css/
---

<!-- static -->
[[info]]
| <iframe src="http://update.sonarsource.org/plugins/cssfamily-confluence-include.html" height="125px">Your browser does not support iframes.</iframe>
<!-- /static -->


## Prerequisites
In order to analyze CSS code, you need to have Node.js >= 6 installed on the machine running the scan. Set property `sonar.nodejs.executable` to an absolute path to Node.js executable, if standard `node` is not available.

If you have a community plugin that handles CSS installed on your SonarQube instance it will conflict with SonarCSS, so it should be removed.

## Language-Specific Properties

Discover and update the CSS-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > CSS](/#sonarqube-admin#/admin/settings?category=css)**

## Related Pages
* [Importing External Issues](/analysis/external-issues/) (StyleLint.io)
