---
title: Quality Gates
url: /user-guide/quality-gates/
---

## Overview

Quality Gates enforce a quality policy in your organization by answering one question: is my project ready for release?

To answer this question, you define a set of conditions against which projects are measured. For example:

* No new blocker issues
* Code coverage on new code greater than 80%

See the **Defining Quality Gates** section below for more information on defining conditions.

Ideally, all projects will use the same quality gate, but that's not always practical. For instance, you may find that:

* Technological implementation differs from one application to another (you might not require the same code coverage on new code for Web or Java applications).
* You want to ensure stronger requirements on some of your applications (internal frameworks for example).

Which is why you can define as many quality gates as you need. You can access the **[Quality Gates](/#sonarqube#/quality_gates)** page from the top menu. From here you can define and manage your Quality Gates.

## Use the best Quality Gate configuration

The "Sonar way" Quality Gate is provided by SonarSource, activated by default, and considered as built-in and read-only. This Quality Gate represents the best way to implement the [Clean as You Code](/user-guide/clean-as-you-code/) concept by focusing on new code. With each SonarQube release, we automatically adjust this default quality gate according to SonarQube's capabilities.

With the Quality Gate, you can enforce ratings (reliability, security, security review, and maintainability) based on metrics on overall code and new code. These metrics are part of the default quality gate. Note that, while test code quality impacts your Quality Gate, it's only measured based on the maintainability and reliability metrics. Duplication and security issues aren't measured on test code.

You should adjust your quality gates so they provide clear feedback to developers looking at their project page.

Don't forget that Quality Gate conditions must use differential values. For example, there's no point in checking an absolute value such as: `Number of Lines of Code is greater than 1000`.

### Recommended Quality Gate

We recommend the built-in `Sonar way` quality gate for most projects. It focuses on keeping new code clean, rather than spending a lot of effort remediating old code. Out of the box, it's already set as the default profile.

## Quality Gate status

The current status is displayed prominently at the top of the Project Page:

![Quality Gate Status](/images/quality-gate-status.jpeg)

## Getting notified when a Quality Gate fails

Thanks to the notification mechanism, users can be notified when a quality gate fails. To do so, subscribe to the **New quality gate status** notification either for all projects or a set of projects you're interested in.

## Security

Quality Gates can be accessed by any user (even anonymous users). All users can view every aspect of a quality gate.

To make changes (create, edit or delete) users must be granted the **Administer Quality Profiles and Gates** permission.

A **project administrator** can choose which quality gates his/her project is associated with. See Project Settings for more.

## Defining Quality Gates

Each Quality Gate condition is a combination of:

* a measure
* a comparison operator
* an error value

For instance, a condition might be:

* measure: Blocker issue
* comparison operator: >
* error value: 0

Which can be stated as: No blocker issues.
