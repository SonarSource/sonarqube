---
title: Overview
url: /analysis/overview/
---

Once the SonarQube platform has been installed, you're ready to install a scanner and begin creating projects. To do that, you must install and configure the scanner that is most appropriate for your needs. Do you build with:

* Gradle - [SonarScanner for Gradle](/analysis/scan/sonarscanner-for-gradle/)
* MSBuild - [SonarScanner for MSBuild](/analysis/scan/sonarscanner-for-msbuild/)
* Maven - use the [SonarScanner for Maven](/analysis/scan/sonarscanner-for-maven/)
* Jenkins - [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/)
* Azure DevOps - [SonarQube Extension for Azure DevOps](/analysis/scan/sonarscanner-for-azure-devops/)
* Ant - [SonarScanner for Ant](/analysis/scan/sonarscanner-for-ant/)
* anything else (CLI) - [SonarScanner](/analysis/scan/sonarscanner/)

[[info]]
| SonarQube integrations are supported for popular ALMs: GitHub Enterprise and GitHub.com, BitBucket Server, and Azure Devops Server. 

[[warning]]
| We do not recommend running an antivirus scanner on the machine where a SonarQube analysis runs, it could result in unpredictable behavior.


A project is created in SonarQube automatically on its first analysis. However, if you need to set some configuration on your project before its first analysis, you have the option of provisioning it via Administration options or the **+** menu item, which is visible to users with project creation rights.


## What does analysis produce? 
SonarQube can analyze up to 27 different languages depending on your edition. The outcome of this analysis will be quality measures and issues (instances where coding rules were broken). However, what gets analyzed will vary depending on the language:

* On all languages, "blame" data will automatically be imported from supported SCM providers. [Git and SVN are supported automatically](/analysis/scm-integration/). Other providers require additional plugins.
* On all languages, a static analysis of source code is performed (Java files, COBOL programs, etc.)
* A static analysis of compiled code can be performed for certain languages (.class files in Java, .dll files in C#, etc.)


## Will all files be analyzed?
By default, only files that are recognized by your edition of SonarQube are loaded into the project during analysis. 
For example if you're using SonarQube Community Edition, which includes analysis of Java and JavaScript, but not C++, all `.java` and `.js` files would be loaded, but `.cpp` files would be ignored.

## What about branches and pull requests?
_Developer Edition_ adds the ability to analyze your project's [branches](/branches/overview/) and [pull requests](/analysis/pull-request/) as well as the ability to automatically decorate pull requests in some ALM interfaces. 

## What happens during analysis?
During analysis, data is requested from the server, the files provided to the analysis are analyzed, and the resulting data is sent back to the server at the end in the form of a report, which is then analyzed asynchronously server-side.

Analysis reports are queued, and processed sequentially, so it is quite possible that for a brief period after your analysis log shows completion, the updated values are not visible in your {instance} project. However, you will be able to tell what's going on because an icon will be added on the project homepage to the right of the project name. Mouse over it for more detail (and links if you're logged in with the proper permissions).

![background task processing in progress.](/images/backgroundTaskProcessingInProgress.jpeg)


The icon goes away once processing is complete, but if analysis report processing fails for some reason, the icon changes:

![background task processing failed.](/images/backgroundTaskProcessingFailedIcon.jpeg)


## FAQ

**Q.** Analysis errors out with `java.lang.OutOfMemoryError: GC overhead limit exceeded`. What do I do?  
**A.** This means your project is too large or too intricate for the scanner to analyze with the default memory allocation. To fix this you'll want to allocate a larger heap (using `-Xmx[numeric value here]`) to the process running the analysis. Some CI engines may give you an input to specify the necessary values, for instance if you're using a Maven Build Step in a Jenkins job to run analysis. Otherwise, use Java Options to set a higher value. Note that details of setting Java Options are omitted here because they vary depending on the environment.
