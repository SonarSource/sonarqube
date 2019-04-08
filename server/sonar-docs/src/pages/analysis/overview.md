---
title: Overview
url: /analysis/overview/
---

<!-- sonarcloud -->
## Prepare your organization

A project must belong to an [organization](/organizations/overview/). Create one if you intend to collaborate with your team mates, or use your personal organization for test purposes.

[[info]]
| ** Important note for private code:** Newly created organizations and personal organizations are under a free plan by default. This means projects analyzed on these organizations are public by default: the code will be browsable by anyone. If you want private projects, you should [upgrade your organization to a paid plan](/sonarcloud-pricing/).

Find the key of your organization, you will need it at later stages. It can be found on the top right corner of your organization's header.

## Run analysis

SonarCloud currently does not trigger analyses automatically - this feature will come in a near future. Currently, it's up to you to launch them inside your existing CI scripts using the available scanners:
* Gradle - [SonarScanner for Gradle](https://redirect.sonarsource.com/doc/gradle.html)
* MSBuild - [SonarScanner for MSBuild](https://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html)
* Maven - use the [SonarScanner for Maven](https://redirect.sonarsource.com/doc/install-configure-scanner-maven.html)
* Ant - [SonarScanner for Ant](https://redirect.sonarsource.com/doc/install-configure-scanner-ant.html)
* anything else (CLI) - [SonarScanner](https://redirect.sonarsource.com/doc/install-configure-scanner.html)

After creating a project, the tutorial available on its homepage will guide you to find how to trigger an analysis.

![Info](/images/info.svg) Remember that depending on which cloud solution you are using for your developments, you can rely on dedicated integrations to help you:

* [GitHub](/integrations/github/)
* [Bitbucket Cloud](/integrations/bitbucketcloud/)
* [Azure DevOps (formerly VSTS)](/integrations/vsts/)

<!-- /sonarcloud -->

<!-- sonarqube -->
Once the SonarQube platform has been installed, you're ready to install an analyzer and begin creating projects. To do that, you must install and configure the scanner that is most appropriate for your needs. Do you build with:

* Gradle - [SonarScanner for Gradle](https://redirect.sonarsource.com/doc/gradle.html)
* MSBuild - [SonarScanner for MSBuild](https://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html)
* Maven - use the [SonarScanner for Maven](https://redirect.sonarsource.com/doc/install-configure-scanner-maven.html)
* Jenkins - [SonarScanner for Jenkins](https://redirect.sonarsource.com/plugins/jenkins.html)
* Azure DevOps - [SonarQube Extension for Azure DevOps](https://redirect.sonarsource.com/doc/install-configure-scanner-tfs-ts.html)
* Ant - [SonarScanner for Ant](https://redirect.sonarsource.com/doc/install-configure-scanner-ant.html)
* anything else (CLI) - [SonarScanner](https://redirect.sonarsource.com/doc/install-configure-scanner.html)

**Note** that we do not recommend running an antivirus scanner on the machine where a SonarQube analysis runs, it could result in unpredictable behavior.


A project is created in the platform automatically on its first analysis. However, if you need to set some configuration on your project before its first analysis, you have the option of provisioning it via Administration options.
<!-- /sonarqube -->

## What does analysis produce? 
{instance} can perform analysis on 20+ different languages. The outcome of this analysis will be quality measures and issues (instances where coding rules were broken). However, what gets analyzed will vary depending on the language:

* On all languages, "blame" data will automatically be imported from supported SCM providers. [Git and SVN are supported automatically](/analysis/scm-integration/). Other providers require additional plugins.
* On all languages, a static analysis of source code is performed (Java files, COBOL programs, etc.)
* A static analysis of compiled code can be performed for certain languages (.class files in Java, .dll files in C#, etc.)


## Will all files be analyzed?
By default, only files that are recognized by a language analyzer are loaded into the project during analysis. 
<!-- sonarqube -->
For example if your SonarQube instance had only SonarJava SonarJS on board, all .java and .js files would be loaded, but .xml files would be ignored.

## What about branches and pull requests?
_Developer Edition_ adds the ability to analyze your project's release / [long-lived branches](/branches/long-lived-branches/), feature / [short-lived branches](/branches/short-lived-branches/), and [pull requests](/analysis/pull-request/) as well as the ability to automatically decorate pull requests in some SCM interfaces. For more on branches see the [branches overview](/branches/overview/).
<!-- /sonarqube -->

## What happens during analysis?
During analysis, data is requested from the server, the files provided to the analysis are analyzed, and the resulting data is sent back to the server at the end in the form of a report, which is then analyzed asynchronously server-side.

Analysis reports are queued, and processed sequentially, so it is quite possible that for a brief period after your analysis log shows completion, the updated values are not visible in your {instance} project. However, you will be able to tell what's going on because an icon will be added on the project homepage to the right of the project name. Mouse over it for more detail (and links if you're logged in with the proper permissions).

![background task processing in progress.](/images/backgroundTaskProcessingInProgress.jpeg)


The icon goes away once processing is complete, but if analysis report processing fails for some reason, the icon changes:

![background task processing failed.](/images/backgroundTaskProcessingFailedIcon.jpeg)


## FAQ

**Q.** Analysis errors out with `java.lang.OutOfMemoryError: GC overhead limit exceeded`. What do I do?  
**A.** This means your project is too large or too intricate for the scanner to analyze with the default memory allocation. To fix this you'll want to allocate a larger heap (using `-Xmx[numeric value here]`) to the process running the analysis. Some CI engines may give you an input to specify the necessary values, for instance if you're using a Maven Build Step in a Jenkins job to run analysis. Otherwise, use Java Options to set a higher value. Note that details of setting Java Options are omitted here because they vary depending on the environment.
