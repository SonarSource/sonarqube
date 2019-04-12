---
title: Project Move
url: /instance-administration/project-move/
---

_Project Move is available as part of [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

Project Move allows you to export a project from one SonarQube instance and import it into another, identically configured SonarQube instance. To use Project Move, you must have the Administer permission on the project in the source instance, and access to the file systems of both instances.

## When to Use "Project Move"
In the following cases:

* you want to create a central SonarQube instance at enterprise level and you want to keep the history created on N instances used previously at the team level
* your company is acquiring another company that already has a central SonarQube instance
* an application is transferred from one team to another in a large company and that company has several SonarQube instances

## Prerequisites
In order to be able to export and then load your Project's data, the two SonarQube instances must have:

* the exact same version
* the same plugins with the same versions
* the same custom metrics
* the same custom rules

## How To Export
On the source instance:
* reanalyze the project one last time to make sure it is populated with data corresponding to your current SonarQube installation
* navigate to the project and at the project level, choose **Administration > Import / Export**
* click on the **Export** button to generate a zip file containing the settings and history of your Project (but not the source code). Note that if you need to change the Project's key, you must to do it before performing the export.

A zip file containing all project data ex is generated in _$SONAR_SOURCE_HOME/data/governance/project_dumps/export/_ named _<project_key>.zip_

## How To Import
On the target instance:

* With a user having the "Administer System" and "Create Projects" permissions, go to [**Administration > Projects > Management**](/#sonarqube-admin#/admin/projects_management/) and [provision the project](/project-administration/project-existence/) using the same key the project had in the source instance.
* Configure the Project's permissions, and the Quality Profiles and Quality Gate associated to the Project
* Put the generated zip file into the directory *$SONAR\_TARGET\_HOME/data/governance/project_dumps/import*
* Go to the Project's Home Page and choose **Administration > Import / Export**
* Click on the Import button to start importing your data
* Source code is not included in the zip file. Once the import is finished, trigger an analysis to import source files into the new instance.

Notes:

* If the import is successful, the zip file will automatically be deleted.
* It is not possible to import a Project that has been already analyzed on the target instance.
* Security reports in an imported project will be empty until analysis has run.
