---
title: Project Settings
url: /project-administration/project-settings/
---

## Tags

Project tags allow you to categorize and group projects for easier selection on the **Projects** page. Project tags can be administered from the project home page. Administrators will see a dropdown menu indicator next to the project's list of current tags (or next to the "No tags" indicator). If the tag you want isn't visible in the dropdown, use the built in "search" input to find what you're looking for or create it on the fly.

## Administration Items

Project administration is accessible through the **Administration** menu of each project.  

Only project administrators can access project's settings (see [Authorization](/instance-administration/security/)).

### Adding a Project

A project is automatically added at its first analysis. Note that you can also [provision projects](/project-administration/project-existence/).

### Analysis Report Processing

A project-level Background Tasks page is available at **Administration > Background Tasks** to allow project administrators to check their projects' processing. It offers analysis report details and logs.

### Deleting a Project

You can delete a project through **Administration > Deletion**:

Note also that projects can also be deleted in bulk.

### Setting the New Code Period  

The new code period can be set on a per-project basis through **Administration > General Settings > New Code > New Code Period**. The default value is `previous_version`, which is most appropriate value for projects that version/release regularly. For other projects, a time period such as 30 days (enter "30", the "days" part is understood) may be more appropriate.

Note that this can be set at the global level as well.

### Updating Project Key  

The project key can be updated (without losing the history on the project) at **Administration > Update Key**.

The new key must contain at least one non-digit character. Allowed characters are: 'a' through 'z', 'A' through 'Z', '-' (dash), '\_' (underscore), '.' (dot), ':' (colon) and digits '0' to '9'.

### Default Issue Assignee

When new issues are created during an analysis, they are assigned to the last committer where the issue was raised. When it is not possible to identify the last committer, issues can be assigned to a default assignee, at **Administration > General Settings > Issues**.

### Setting Quality Gate and Quality Profiles  

Project administrators can select which ...

* Quality profiles (go to **Administration > Quality Profiles**)
* Quality gate (go to **Administration > Quality Gate**)

... to use on their project.

### Setting Exclusions  

See [Narrowing the Focus](/project-administration/narrowing-the-focus/).

### Customizing Links

On top of standard links which may only be set as [Analysis Parameters](/analysis/analysis-parameters/), additional custom links can be added through the web interface (under **Administration > Links**). Those links will then be displayed in the [Project Page](/user-guide/project-page/).
