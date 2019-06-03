---
title: VB.NET
url: /analysis/languages/vb/
---

<!-- static -->
[[info]]
| <iframe src="http://update.sonarsource.org/plugins/vbnet-confluence-include.html" height="125px">Your browser does not support iframes.</iframe>
<!-- /static -->


## Language-Specific Properties

Discover and update the VB.NET-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud --> **[Administration > General Settings > VB.NET](/#sonarqube-admin#/admin/settings?category=vb.net)**

## Known Limitations
Currently an error will be thrown when an issue is raised on a line of code containing the following pattern `\s+error\s*:` (i.e. one or more spaces, the string 'error', zero or more spaces and a ':' ) . This is a well known problem on the Microsoft side (see [issue](https://github.com/dotnet/roslyn/issues/5724/)). In order to work around this problem, our analyzer will skip issues reported on any line where the pattern is detected.


## Related Pages
* [Importing External Issues](/analysis/external-issues/) (VSTest, NUnit, MSTest, xUnit)
* [Test Coverage & Execution](/analysis/coverage/) (Visual Studio Code Coverage, dotCover, OpenCover, NCover 3)
