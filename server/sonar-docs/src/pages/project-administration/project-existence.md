---
title: Project Existence
url: /project-administration/project-existence/
---

Typically, projects are created during their first analysis and never deleted (because old software never dies). For atypical situations, there is the page at **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management/)**, which allows you to manage project existence.

## How do I provision a project before its first analysis?
Provisioning a project allows you to declare and configure it (define permissions, set quality profiles, etc.) before running the first analysis. To be able to provision projects, you have to be logged in and be granted the Provision Projects permission.

To provision a new project either use the '+' menu in the top menu or if you have global administration privileges, go to **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management/)** and click on **Create Project**. The only required information is the key and the name of your project.

Once the project is provisioned, you can configure it (define permissions, set quality profiles, etc.), and when you're finished with the configuration, you can simply run the project's first analysis.

You can also provision and configure projects using the Web API.

## How do I find provisioned projects (that haven't been analyzed yet)?
The **[Projects Management](/#sonarqube-admin#/admin/projects_management/)** search interface includes a toggle to allow you to narrow your results on this page to only projects that have never been analyzed. From there you can deal with them on this page as a set, or click through to the individual project homepages for individual attention and administration.

## How do I lock down permissions on a project? (Private vs Public)
By default, any newly created project will be considered "Public". It means every SonarQube user, authenticated or not, will be able to:

* **Browse**: Access a project, browse its measures, issues and perform some issue edits (confirm/resolve/reopen, assignment, comment).
* **See Source Code**: View the project's source code.

If you want to be sure only a limited list of Groups and Users can see the project, you need to mark it Private. Once a project is private you will be able to define which Groups and Users can **Browse** the project or **See Source Code**.

If you want all newly created projects to be considered "Private", you can change the default visibility in **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management/)**.

## How do I delete projects?
A project may be deleted individually from the Administration page of the project. See Project Settings for more details. To delete projects in bulk, use **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management/)**. Here you can select the projects to delete. A deleted project is gone for good, there is no way to undo this action.

## How do I find projects that are no longer analyzed?
The **[Projects Management](/#sonarqube-admin#/admin/projects_management/)** search interface includes a date picker to help you find all projects last analyzed before your specified date. From there you can deal with them on this page as a set, or click through to the individual project homepages for individual attention and administration.
