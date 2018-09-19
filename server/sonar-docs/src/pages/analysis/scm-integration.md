---
title: SCM Integration
url: /analysis/scm-integration/
---

Collecting SCM data during code analysis can unlock a number of SonarQube features:

* Automatic Issue Assignment
* code annotation (blame data) in the Code Viewer
* SCM-driven detection of new code (to help with [Fixing the Water Leak](/user-guide/fixing-the-water-leak/)). Without SCM data, SonarQube determines new code using analysis dates (to timestamp modification of lines).

### Turning it on/off
SCM integration requires support for your individual SCM provider. Git and SVN are supported by default. <!-- sonarqube -->For other SCM providers, see the Marketplace.<!-- /sonarqube -->

If need be, you can toggle it off at global/project level via administration settings.

