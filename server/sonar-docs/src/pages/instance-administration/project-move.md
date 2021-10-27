---
title: Project Move
url: /instance-administration/project-move/
---

Project Move allows you to export a project from one SonarQube instance and import it into another SonarQube instance. To use Project Move, you must have the Administer permission on the project in the source instance, and access to the file systems of both instances.

## When to use Project Move
Project Move can help you with the following situations:

* You want to create a central SonarQube instance at enterprise level and you want to keep the history created on instances used previously at the team level.
* You want to consolidate your editions and move projects from a Community Edition instance to an [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) instance or [above](https://www.sonarsource.com/plans-and-pricing/).
* Your company is acquiring another company that already has a central SonarQube instance.
* You are at a large company with several SonarQube instances and an application is transferred from one team to another.

## Prerequisites
To export your project's data from the source instance and then load it on the target instance, make sure the following are true.

The _target_ instance must:

* Be [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) or [above](https://www.sonarsource.com/plans-and-pricing/).
* Contain all of the plugins with the same versions as the source instance.  

[[info]]
|The target instance can have additional plugins and languages that aren't in the source instance, but not the other way around. If your source instance has plugins that aren't in your target instance, either remove them and reanalyze your project or add them to your target instance.

Both instances must have:

* The exact same SonarQube version
* The same custom metrics
* The same custom rules

## How to export
_Your source instance can be Community Edition or above, but cannot have plugins or languages that are not in the target instance._

On the source instance:
* reanalyze the project one last time to make sure it is populated with data corresponding to your current SonarQube installation
* navigate to the project and at the project level, choose **Project Settings > Import / Export**
* click on the **Export** button to generate a zip file containing the settings and history of your Project (but not the source code). Note that if you need to change the Project's key, you must to do it before performing the export.

A zip file containing all project data is generated in _$SONAR_SOURCE_HOME/data/governance/project_dumps/export/_ named _<project_key>.zip_

## How to import
_Your target instance must be [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) or [above](https://www.sonarsource.com/plans-and-pricing/)._

On the target instance:

* With a user having the "Administer System" and "Create Projects" permissions, go to [**Administration > Projects > Management**](/#sonarqube-admin#/admin/projects_management/) and [provision the project](/project-administration/project-existence/) using the same key the project had in the source instance.
* Configure the Project's permissions, and the Quality Profiles and Quality Gate associated to the Project
* Put the generated zip file into the directory *$SONAR\_TARGET\_HOME/data/governance/project_dumps/import*
* Go to the Project's Home Page and choose **Project Settings > Import / Export**
* Click on the Import button to start importing your data
* Source code is not included in the zip file. Once the import is finished, trigger an analysis to import source files into the new instance.

Notes:

* If the import is successful, the zip file will automatically be deleted.
* It is not possible to import a project that has been already analyzed on the target instance.
* Security reports in an imported project will be empty until an analysis has run.
