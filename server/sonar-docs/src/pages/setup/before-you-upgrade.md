---
title: Before You Upgrade
url: /setup/before-you-upgrade/
---

This page contains some concepts and recommendations that you should familiarize yourself with before upgrading. See the [Upgrade Guide](/setup/upgrading/) for information on the actual upgrade process.

## SonarQube version number format
Version numbers have up to three digits with each digit representing part of the release cycle:

![SonarQube version number format](/images/version.png)

**Major version number**  
The major version number represents a series of releases with high-level objectives for the release cycle. It's incremented with the release following an LTS version (for example, the release following 7.9 LTS was 8.0).

**Minor version number**  
The minor version number corresponds to incremental functional changes within a major release cycle. At the time of an LTS release, the release cycle is closed and the minor version number is frozen.

**Patch release number**  
Only on LTS versions, the patch release number represents patches to an LTS that fixed blocker or critical problems. The patch release number isn't considered in your upgrade migration path, and your migration path is the same no matter which patch number you are on.

## Migration path
Upgrading across multiple non-LTS versions is handled automatically. However, if there are one or multiple LTS versions in your migration path, you must first migrate to each intermediate LTS and then to your target version, as shown in **Example 3** below.

[[info]]
|If you're migrating from an earlier patch version of an LTS, you can upgrade directly to the next LTS. You don't need to install any intermediate patch versions.

**Migration Path Examples**:

**Example 1** – From 8.1 > 9.0, the migration path is 8.1 > 8.9.1 LTS > 9.0  
**Example 2** – From 8.2 > 8.9 LTS, the migration path is 8.2 > the latest 8.9 LTS patch.  
**Example 3** – From 6.7.7 LTS > 8.9 LTS, the migration path is 6.7.7 LTS > 7.9.6 LTS > the latest 8.9 LTS patch.

## Release Upgrade Notes
Usually SonarQube releases come with some specific recommendations for upgrading from the previous version. You should read the [Release Upgrade Notes](/setup/upgrade-notes/) for each version between your current version and the target version.

## Practice your upgrade
We recommend practicing your upgrade to:
- make sure your infrastructure can run the upgrade.
- get an idea of how long the upgrade will take.
- gain a better understanding of the upgrade process and anticipate what you'll need to do when performing the actual upgrade.
- address any issues you encounter during the practice upgrade on the [SonarSource Community](https://community.sonarsource.com/).

To practice your upgrade, create a staging environment using a recent backup of your production database. You want your staging environment to be as similar to your production instance as possible because the resources and time needed to upgrade depends on what's stored in your database. Use this staging environment to test the upgrade, observing how long it takes to back up and restore systems and complete the process.

