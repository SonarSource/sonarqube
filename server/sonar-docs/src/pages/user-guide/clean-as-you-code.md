---
title: Clean as You Code
url: /user-guide/clean-as-you-code/
---

## What is Clean as You Code?

Clean as You Code is an approach to Code Quality that eliminates a lot of the challenges that come with traditional approaches. As a developer, you focus on maintaining high standards and taking responsibility specifically in the New Code you're working on. SonarQube gives you the tools that let you set high standards and take pride in knowing that your code meets those standards.

## Focus on New Code

With Clean as You Code, your focus is always on New Code (code that has been added or changed in your New Code Period) and making sure the code you write today is clean and safe. 

The New Code Period can be set at different levels (global, project, and with [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/) at the branch level). Depending on the level at which your New Code Period is set, you can change the starting point of your New Code Period to fit your situation.

For more information on the New Code Period and setting it, check out [Setting Your New Code Period](/project-administration/new-code-period/).

## Personal Responsibility

With Clean as You Code, you aren't responsible for anyone else's code. You own the quality and security of the New Code you are working on today. If you add new issues, SonarQube automatically assigns them to you so you can maintain quality in your code.

For more information on issues and how they are assigned, check out the [Issues](/user-guide/issues/) documentation.

## Quality Gate

Your Quality Gate is a set of conditions that tells you whether or not your project is ready for release. With the Clean as You Code approach, your Quality Gate should:

* **Focus on New Code metrics** – When your Quality Gate is set to focus on New Code metrics (like the built-in Sonar way Quality Gate), new features will be delivered cleanly. As long as your Quality gate is green, your releases will continue to improve.
* **Set and enforce high standards** – When standards are set and enforced on New Code, you aren't worried about having to meet those standards in old code and having to clean up someone else's code. You can take pride in meeting high standards on _your_ code. If a project doesn't meet these high standards, it won't pass the Quality Gate, and it's obviously not ready to be released.

For more information on Quality Gates and making sure your Quality Gate is enforcing your standards, check out the [Quality Gates](/user-guide/quality-gates/) documentation.

## Pull Request Analysis

You can use Pull Request analysis and decoration to make sure your code is meeting your standards before you merge. Pull Request analysis lets you see your Pull Request's Quality Gate in the SonarQube UI. You can then decorate your Pull Requests with SonarQube issues directly in your ALM's interface. 

For more information on setting up Pull Request analysis and decoration, see the [Pull Request](/analysis/pull-request/) documentation.