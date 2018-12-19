---
title: Quality Gates
url: /user-guide/quality-gates/
---

## Overview

A quality gate is the best way to enforce a quality policy in your organization.
It's there to answer ONE question: can I deliver my project to production today or not?

In order to answer this question, you define a set of Boolean conditions based on measure thresholds against which projects are measured. For example:

* No new blocker issues
* Code coverage on new code greater than 80%
* Etc.

Ideally, all projects will be verified against the same quality gate, but that's not always practical. For instance, you may find that:

* Technological implementation differs from one application to another (you might not require the same code coverage on new code for Web or Java applications).
* You want to ensure stronger requirements on some of your applications (internal frameworks for example).
* Etc.

Which is why you can define as many quality gates as you wish. Quality Gates are defined and managed in the **[Quality Gates](/#sonarqube#/quality_gates)** page found in the top menu.

## Use the Best Quality Gate Configuration

The quality gate "Sonar way" is provided by SonarSource, activated by default and considered as built-in and so read-only. It represents our view of the best way to implement the [Fixing the Water Leak](/user-guide/fixing-the-water-leak/) concept. <!-- sonarqube -->At each SonarQube release, we adjust automatically this default quality gate according to SonarQube's capabilities.<!-- /sonarqube -->

Three metrics allow you to enforce a given Rating of Reliability, Security and Maintainability, not just overall but also on new code. These metrics are recommended and come as part of the default quality gate. We strongly advise you to adjust your own quality gates to use them to make feedback more clear to your developers looking at their quality gate on their project page.

Don't forget also that quality gate conditions must use differential values. There is no point for example to check an absolute value such as : Number of Lines of Code is greater than 1000.

### Recommended Quality Gate

The `Sonar way` Built-in quality gate is recommended for most projects. If focuses on keeping new code clean, rather than spending a lot of effort remediating old code. Out of the box, it's already set as the default profile.

## Quality Gate Status

The current status is displayed prominently at the top of the Project Page:

![Quality Gate Status](/images/quality-gate-status.jpeg)

## Getting Notified When a Quality Gate Fails

Thanks to the notification mechanism, users can be notified when a quality gate fails. To do so, subscribe to the **New quality gate status** notification either for all projects or a set of projects you're interested in.

## Security

Quality Gates can be accessed by any user (even anonymous users). All users can view every aspect of a quality gate.

To make changes (create, edit or delete) users must be granted the **Administer Quality Profiles and Gates** permission.

A **project administrator** can choose which quality gates his/her project is associated with. See Project Settings for more.

## Defining Quality Gates

To manage quality gates, go to **[Quality Gates](/#sonarqube#/quality_gates)** (top menu bar).

Each Quality Gate condition is a combination of:

* measure
* comparison operator
* error value

For instance, a condition might be:

* measure: Blocker issue
* comparison operator: >
* error value: 0

Which can be stated as: No blocker issues.
