---
title: Integration with VSTS
scope: sonarcloud
---


## Authentication

You can connect to SonarCloud using your VSTS account. On the [login page](/#sonarcloud#/sessions/new), just click on the "Log in with VSTS" button.

** /!\ Warning:** Only work and school VSTS accounts are authorized to login on SonarCloud.

## Install the SonarCloud VSTS extension

The SonarCloud VSTS extension brings everything you need to have your projects analyzed on SonarCloud 
very quickly:
* Integration with the Build definitions to easily trigger the analysis
* Pull request decoration to get quick feedback on the code changes
* Widget to have the overview quality of your projects inside VSTS dashboards

Install [SonarCloud extension for VSTS](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarcloud)by clicking on the "Get it free" button.

Then follow the comprehensive [Microsoft lab on how to integrate VSTS with SonarCloud](https://aka.ms/sonarcloudlab).

## Quality Gate Status widget 

You can monitor the Quality Gate status of your projects directly in your VSTS dashboard. Follow these simple steps to configure your widget:

1. Once the VSTS extension is installed and your project has been successfully analyzed, go to one of your VSTS dashboards (or create one). Click on the pen icon in the bottom right corner of the screen, and then on the "+" icon to add a widget. 

2. In the list of widgets, select the "Code Quality" one and then click on the "Add" button. An empty widget is added to your dashboard. 

3. You can then click on the widget's cogwheel icon to configure it.

    * **For public projects:** you can simply select your project from the dropdown. A searchbar inside the dropdown will help you find it easily. Just select it and click on the "Save" button.

    * **For private projects:** you'll have to log in using the links provided under the dropdown. Once logged in, your private projects will appear in the dropdown. Select the one you are interested in, and click on "Save".