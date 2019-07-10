---
title: AutoScan Beta Feature
nav: AutoScan
url: /autoscan/
---

SonarCloud can autonomously scan your code, by simply reading it from your repository! We call that AutoScan.

[[info]]
| This is currently a Beta feature, with a limited scope and some caveats. Those limitations will be removed along the way.


## Prerequisites

* The first version of this Beta feature works only for GitHub repositories. 
* The automatic analysis can be activated only on projects which are bound to their remote repository. This implies that the the project was set up through the SonarCloud web interface by selecting a repository (i.e. not "manually").

## What to expect

Once activated, SonarCloud will automatically analyze: 
* the default branch of the repository
* the pull requests (PR)

It will take care of doing it whenever you push on your repository.

The following languages are currently supported: 
* ABAP
* Apex
* CSS
* Flex
* Go
* HTML
* JS
* Kotlin
* PHP
* Python
* Ruby
* Scala
* Swift
* TypeScript
* TSQL
* XML

## How to activate the feature?

To enable the automatic analysis, you need to add a `.sonarcloud.properties` file in your repository.

If you're starting from scratch:

1. Do the [setup for your project](/#sonarcloud#/projects/create) (from the `+ > Analyze new project` top right menu)
    * ![](/images/exclamation.svg) Remember that your project must absolutely be created by selecting a GitHub repository - otherwise it won't work.
1. Once the setup is done on SonarCloud, you end up on the project home page which shows a tutorial. Ignore it and simply add a `.sonarcloud.properties` file in the base directory of your default branch or on a PR. 
1. After a while, the analysis results will be visible in SonarCloud (and your PR will be annotated with comments if you pushed the file on a PR)

Here are the supported optional settings for the `.sonarcloud.properties` file:
```
# Path to sources
#sonar.sources=.
#sonar.exclusions=
#sonar.inclusions=

# Path to tests
#sonar.tests=
#sonar.test.exclusions=
#sonar.test.inclusions=

# Source encoding
#sonar.sourceEncoding=UTF-8

# Exclusions for copy-paste detection
#sonar.cpd.exclusions=
```

Note that you can just push an empty `.sonarcloud.properties` file, this will work fine. In this case, every file in the repository will be considered as a source file.

## Current limitations/caveats

* There is no visual feedback (yet) in the UI when SonarCloud runs an analysis.
* A consequence of the previous point is that if - for any reason, SonarCloud fails to successfully run the analysis, nothing will be displayed. In that case, just come on [the forum](https://community.sonarsource.com/tags/c/help/sc/autoscan) and ask a question, we'll monitor that closely.
* Code coverage information is not supported
* Import of external rule engine reports is not supported

## Noteworthy

* This Beta feature works for any project - public or private.
* It can be activated with no extra cost.
* Sources are cloned only during the analysis, and only when the `.sonarcloud.properties` file exists (i.e. when the feature is activated). The cloned repository is fully deleted at the end of the analysis, and SonarCloud does not keep a copy of it.
* Non supported languages (Java, C#, VB.NET, C/C++, ObjectiveC) are not analyzed at all.

## How to give feedback?

Create a new thread on the forum, under ["Get Help > SonarCloud"](https://community.sonarsource.com/tags/c/help/sc/autoscan), with the "autoscan" tag.

We'd love to hear your feedback about this new upcoming feature, may it be about bugs, improvements, or anything you want to share with us!
