---
title: Overview
url: /analysis/pull-request/
---

_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

You can see your Pull Requests in {instance} from the branches and pull requests dropdown menu of your project.

Pull Request analysis allows you to see your Pull Request's Quality Gate and analysis in the {instance} interface:

![Pull Request Analysis.](/images/pranalysis.png)

## Pull Request Decoration
You can also add Pull Request decoration that shows the Pull Request analysis and Quality Gate directly in your ALM's interface. See [Decorating Pull Requests](/analysis/pr-decoration/) for more information on setting it up.

## Pull Request Quality Gate

A [Quality Gate](/user-guide/quality-gates/) lets you ensure you are meeting your organization's quality policy and that you can merge your Pull Request. The Pull Request Quality Gate:
* **Focuses on new code** – The PR quality gate only uses your project's quality gate conditions that apply to "on New Code" metrics.
* **Assigns a status** – Each PR shows a quality gate status reflecting whether it Passed or Failed.

PR analyses on {instance} are deleted automatically after 30 days with no analysis. This can be updated in **Administration > Configuration > General Settings > Housekeeping > Number of days before purging inactive branches**. 

## Analysis Parameters

These parameters enable PR analysis:

| Parameter Name        | Description |
| --------------------- | ---------------------------------- |
| `sonar.pullrequest.key` | Unique identifier of your PR. Must correspond to the key of the PR in GitHub or Azure DevOps.<br/> e.g.: `sonar.pullrequest.key=5` |
| `sonar.pullrequest.branch` | The name of the branch that contains the changes to be merged.<br/> e.g.: `sonar.pullrequest.branch=feature/my-new-feature` |
| `sonar.pullrequest.base` | The branch into which the PR will be merged. <br/> Default: master <br/> e.g.: `sonar.pullrequest.base=master` |
