---
title: Supporting SCM Providers
url: /extend/adding-scm/
---
SonarQube Scanner uses information from the project's SCM, if available, to:

* Assign a new issue to the person who introduced it. The last committer on the related line of code is considered to be the author of the issue. 
* Estimate the coverage on New Code, including added and changed code since in your New Code.
* Display the most recent commit on each line the code viewer.
![Commit info is available from the margin of the code viewer](/images/commit-info-in-code-viewer.png)

The only required SCM command is "blame", which gets the last committer of each line for a given file. This command is executed by a SonarQube plugin through the extension point `org.sonar.api.batch.scm.ScmProvider`. See the embedded SCM integrations, for instance [Git](/analysis/scm-integration/), for more details.
