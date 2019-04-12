---
title: Project Page
url: /user-guide/project-page/
---


The Project Hompepage is the entry point of any project showing:
* the releasability status of the project
* the current state of its quality
* the quality of what has been produced since the beginning of its [New Code Period](/user-guide/fixing-the-water-leak/).
 

The Project Page answers two questions:
* can I release my project today?
* if not, what should I improve to make the project pass the Quality Gate? 

## Can I release today?

Since the [Quality Gate](/user-guide/quality-gates/) is your most powerful tool to enforce your quality policy, the page starts with the project's current Quality Gate status. If the project passes, a simple, green all-clear is shown.

If not, details and drill-downs are immediately available to quickly identify what went wrong, with a section for each error condition showing what the current project value is and what it should be. As usual, you'll be able to click through on current values to get to drilldowns.

## What should I fix first?
Because the best way to improve a project's quality is to catch and fix new problems before they become entrenched, the first view of a project is centered around the New Code Period, which is highlighted in yellow on the right of the project homepage. The project space page shows a high-level summary of critical metrics, both current values and their New Code Period values.

Just below the Quality Gate information, you have the numbers of old and new Issues in the Reliability and Security domains and then the Maintainability domain. Clicking on any figure on the page will take you to a detailed view, either in the Measures Page or the Issues Page.

The most important thing a developer must do is to ensure the new Issues in the yellow part of the screen are acknowledged, reviewed and fixed and to make sure that new code is covered by tests to help prevent future regressions. Regardless of how many Issues were introduced in the past or how little test coverage there is overall, a focus on the newly added Issues will ensure that the situation won't degrade versus the version you previously released in production.

So, which issues should you go after first: Bugs, Vulnerabilities or Code Smells? It depends, because the answer is dependent on the nature of your Issues. Let's say you have issues for a block of code that is duplicated 5 times, and inside this duplicated block of code, you have 3 Bugs and 5 Security Issues. The best approach is probably to fix the duplication first and then resolve the Bugs and Vulnerabilities in the newly centralized location, rather than fixing them 5 times.

That's why you need to review your new Issues before jumping into resolving them. 

## How can I ...
### How can I see project measures at a lower level?
The project-level **Measures** menu item takes you to a dedicated sub-space where you see all project measures. Choose a measure for more details. Both list and tree views are available for each measure, and treemaps are available for percentages and ratings.

### How can I see all the issues in a project?
The project-level **Issues** menu item takes you to a project-specific Issues page, where you can perform all the same actions you can at the higher level.
On this page, you can easily narrow the list to the New Issues introduced during the New Code Period, by selecting `New Code` in **Creation Date** facet.

### How can I see the project structure and code?
The project-level **Code** menu item takes you to an outline of your project structure. Drill down to see files in a directory, and choose a file to see its code.

If your project is too large for easy exploration via drilling, the search feature on this page will help. While the global search in the main menu returns results from throughout the {instance} instance, the localized search on the code page is restricted to files and directories in the current project.

### How can I see the project activity / history?
The project-level **Activity** menu item takes you to the full list of code scans performed on your project since it was created in {instance}. By going there you can follow the evolution of the Quality Gate, see the changes of Quality Profiles and know when a given version of your code has been scanned.

### How can I easily spot the risks in a project?
Visualizations allow you to compare project components and quickly spot the ones that represent the greatest risks. The **Activity** page offers several pre-defined visualizations, and you can also create Custom visualizations with the metrics of your choice.

### How can I promote the health of my project to peers ?
If your project is publicly visible, then you can further promote its status in external tools and websites using native Project Badges. The **Get project badges** button on the homepage of a public project lets you choose/fine-tune your badge and gives you the URL for it.
