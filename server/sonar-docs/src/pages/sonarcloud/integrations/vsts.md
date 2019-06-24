---
title: Get started with Azure DevOps Services
nav: Azure DevOps Services
url: /integrations/vsts/
---

[[info]]
| This page is about Azure DevOps Services, formerly known as VSTS.

You can connect to SonarCloud using your Azure DevOps account. On the [login page](/#sonarcloud#/sessions/new), just click on the "Log in with Azure DevOps" button.

[[warning]]
| ![Warning](/images/exclamation.svg) Only work and school Azure DevOps accounts are authorized to login on SonarCloud.

## Install the SonarCloud Azure DevOps extension

The SonarCloud Azure DevOps extension brings everything you need to have your projects analyzed on SonarCloud 
very quickly:
* Integration with the Build definitions to easily trigger the analysis
* Pull request decoration to get quick feedback on the code changes
* Widget to have the overview quality of your projects inside Azure DevOps dashboards

Install [SonarCloud extension for Azure DevOps](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarcloud)by clicking on the "Get it free" button.

Then follow the comprehensive [Microsoft lab on how to integrate Azure DevOps with SonarCloud](https://aka.ms/sonarcloudlab).

## Quality Gate Status widget 

You can monitor the Quality Gate status of your projects directly in your Azure DevOps dashboard. Follow these simple steps to configure your widget:

1. Once the Azure DevOps extension is installed and your project has been successfully analyzed, go to one of your Azure DevOps dashboards (or create one). Click on the pen icon in the bottom right corner of the screen, and then on the "+" icon to add a widget. 

2. In the list of widgets, select the "Code Quality" one and then click on the "Add" button. An empty widget is added to your dashboard. 

3. You can then click on the widget's cogwheel icon to configure it.

    * **For public projects:** you can simply select your project from the dropdown. A searchbar inside the dropdown will help you find it easily. Just select it and click on the "Save" button.

    * **For private projects:** you'll have to log in using the links provided under the dropdown. Once logged in, your private projects will appear in the dropdown. Select the one you are interested in, and click on "Save".

## FAQ

1. Which kind of analysis scenario are supported for .Net projects ?

    * Using Sonar Scanner for MSBuild, you can build multiple .Net projects / solutions between the "Prepare Analysis on SonarCloud" and "Run Analysis" tasks. You will have full support of Issues and Code Coverage on both branches and PR Analysis. Other kind of scenarios are not yet supported.