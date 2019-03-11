---
title: Organizations
url: /organizations/overview/
---

## Overview

An organization is a space where a team or a whole company can collaborate across many projects.

An organization consists of:
* Projects, on which users collaborate
* [Members](/organizations/manage-team/), who can have different persmissions on the projects
* [Quality Profiles](/instance-administration/quality-profiles/) and [Quality Gates](/user-guide/quality-gates/), which can be customized and shared accross projects

There are 2 kind of organizations:
* **Personal organizations**. Each account has a personal organization linked to it. This is typically where open-source developers host their personal projects. It is not possible to delete this kind of organization.
* **Standard organization**. This is the kind of organization that users want to create for their companies or for their open-source communities. As soon as you want to collaborate, it is a good idea to create such an organization.

Organizations can be on:
* **Free plan**. This is the default plan. Every project in an organization on the free plan is public.
* **Paid plan**. This plan unlocks the ability to have private projects. Go to the "Billing" page of your organization to upgrade it to the paid plan.

Depending on which plan the organization is in, its [visibility](/organizations/organization-visibility/) will change.

You can create organizations from the top right menu "+ > Create new organization"

## How to bind an existing organization to GitHub or Bitbucket Cloud?

You might notice the following warning message on your pull requests inside SonarCloud:

    The SonarCloud GitHub application is installed on your GitHub organization, but the 
    SonarCloud organization is not bound to it. Please read "How to bind an existing 
    organization?" section in the "Organizations" documentation page to fix your setup.

This means that your SonarCloud organization is not bound to GitHub or Bitbucket Cloud whereas you had already installed the SonarCloud application (probably to annotate pull requests). To fix your setup, here are the steps to follow.

**For GitHub:**

1. Click your profile picture in the top right menu and select the organization.
2. In the organization menu, click "Administration > Organization settings"
3. Click on "Choose an organization on GitHub".
4. On GitHub page, you should see a list of organization you are admin of. The organization you want to bind is marked as already configured. Click on it.
5. Click on "Uninstall" at the bottom of the page.
6. Go back to SonarCloud, to the settings page of your organization, and click on "Choose an organization on GitHub" again. The organization you want to bind should not be marked as configured anymore. Click on it, and then on "Install". After the installation, you will be redirected to SonarCloud.
7. You are all set! You should see a GitHub icon close to the name of your organization at the top of the page.

**For Bitbucket Cloud:**

1. Click your profile picture in the top right menu and select the organization.
2. In the organization menu, click "Administration > Organization settings"
3. Click on "Choose a team on Bitbucket".
4. On Bitbucket Cloud page, select the name of the team you want to bind and click on "Grant access". You will then be redirected to SonarCloud.
   [[warning]]
   | If you get a 405 error page from Bitbucket Cloud at this stage, this means that you did not approve a recent scope change - for which you should have received an email from Bitbucket Cloud. The easiest way to get around this is to uninstall the SonarCloud application in your Bitbucket Cloud "Install apps" settings, and reinstall it.
5. You are all set! You should see a Bitbucket Cloud icon close to the name of your organization at the top of the page.
