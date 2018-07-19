---
title: Analyzing Source Code
---

Once the SonarQube platform has been installed, you're ready to install an analyzer and begin creating projects. To do that, you must install and configure the scanner that is most appropriate for your needs. Do you build with:

<!-- sonarcloud -->
* TravisCI for SonarCloud - [SonarCloud Travis addon](https://docs.travis-ci.com/user/sonarcloud/)
<!-- /sonarcloud -->
* Gradle - [SonarQube Scanner for Gradle](https://redirect.sonarsource.com/doc/gradle.html)
* MSBuild - [SonarQube Scanner for MSBuild](https://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html)
* Maven - use the [SonarQube Scanner for Maven](https://redirect.sonarsource.com/doc/install-configure-scanner-maven.html)
* Jenkins - [SonarQube Scanner for Jenkins](https://redirect.sonarsource.com/plugins/jenkins.html)
* VSTS / TFS - [SonarQube Extension for VSTS-TFS](https://redirect.sonarsource.com/doc/install-configure-scanner-tfs-ts.html)
* Ant - [SonarQube Scanner for Ant](https://redirect.sonarsource.com/doc/install-configure-scanner-ant.html)
* anything else (CLI) - [SonarQube Scanner](https://redirect.sonarsource.com/doc/install-configure-scanner.html)

**Note** that we do not recommend running an antivirus scanner on the machine where a SonarQube analysis runs, it could result in unpredictable behavior.


A project is created in the platform automatically on its first analysis. However, if you need to set some configuration on your project before its first analysis, you have the option of provisioning it via Administration options.

## What does analysis produce? 
SonarQube can perform analysis on 20+ different languages. The outcome of this analysis will be quality measures and issues (instances where coding rules were broken). However, what gets analyzed will vary depending on the language:

* On all languages, "blame" data will automatically be imported from supported SCM providers. Git and SVN are supported automatically. Other providers require additional plugins.
* On all languages, a static analysis of source code is performed (Java files, COBOL programs, etc.)
* A static analysis of compiled code can be performed for certain languages (.class files in Java, .dll files in C#, etc.)
* A dynamic analysis of code can be performed on certain languages.

## Will _all_ files be analyzed?
By default, only files that are recognized by a language analyzer are loaded into the project during analysis. For example if your SonarQube instance had only SonarJava SonarJS on board, all .java and .js files would be loaded, but .xml files would be ignored. However, it is possible to import all text files in a project by setting [**Settings > Exclusions > Files > Import unknown files**](/#sonarqube-admin#/admin/settings?category=exclusions) to true. 

## What happens during analysis?
During analysis, data is requested from the server, the files provided to the analysis are analyzed, and the resulting data is sent back to the server at the end in the form of a report, which is then analyzed asynchronously server-side.

Analysis reports are queued, and processed sequentially, so it is quite possible that for a brief period after your analysis log shows completion, the updated values are not visible in your SonarQube project. However, you will be able to tell what's going on because an icon will be added on the project homepage to the right of the project name. Mouse over it for more detail (and links if you're logged in with the proper permissions).

![background task processing in progress.](/images/backgroundTaskProcessingInProgress.jpeg)


The icon goes away once processing is complete, but if analysis report processing fails for some reason, the icon changes:

![background task processing failed.](/images/backgroundTaskProcessingFailedIcon.jpeg)


## F.A.Q.

**Q.** Analysis errors out with `java.lang.OutOfMemoryError: GC overhead limit exceeded`. What do I do?  
**A.** This means your project is too large or too intricate for the scanner to analyze with the default memory allocation. To fix this you'll want to allocate a larger heap (using `-Xmx[numeric value here]`) to the process running the analysis. Some CI engines may give you an input to specify the necessary values, for instance if you're using a Maven Build Step in a Jenkins job to run analysis. Otherwise, use Java Options to set a higher value. Note that details of setting Java Options are omitted here because they vary depending on the environment.
