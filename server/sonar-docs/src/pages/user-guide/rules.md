---
title: Rules
url: /user-guide/rules/
---
## Overview
In {instance}, analyzers contribute rules which are executed on source code to generate issues. There are four types of rules:
* Code Smell (Maintainability domain)
* Bug (Reliability domain)
* Vulnerability (Security domain)
* Security Hotspot (Security domain)

For Code Smells and Bugs, zero false-positives are expected. At least this is the target so that developers don't have to wonder if a fix is required.

For Vulnerabilities, the target is to have more than 80% of issues be true-positives.

Security Hotspot rules draw attention to code that is security-sensitive. It is expected that more than 80% of the issues will be quickly resolved as "Reviewed" after review by a developer.

The Rules page is the entry point where you can discover all the existing rules or create new ones based on provided templates.

## Rules

By default, when entering the top menu item "Rules", you will see all the available rules brought by the analyzers <!-- sonarqube -->installed on your {instance} instance<!-- /sonarqube --><!-- sonarcloud -->available on SonarCloud<!-- /sonarcloud -->. You have the ability to narrow the selection based on search criteria in the left pane:

* **Language**: the language to which a rule applies.
* **Type**: Bug, Vulnerability, Code Smell or Security Hotspot rules.
* **Tag**: it is possible to add tags to rules in order to classify them and to help discover them more easily.
* **Repository**: the engine/analyzer that contributes rules to {instance}.
* **Default Severity**: the original severity of the rule - as defined by the analyzer that contributes this rule.
* **Status**: rules can have 3 different statuses:
  * **Beta**: The rule has been recently implemented and we haven't gotten enough feedback from users yet, so there may be false positives or false negatives.
  * **Deprecated**: The rule should no longer be used because a similar, but more powerful and accurate rule exists.
  * **Ready**: The rule is ready to be used in production.
* **Available Since**: date when a rule was first added on {instance}. This is useful to list all the new rules since the last upgrade of a plugin for instance.
* **Template**: display rule templates that allow to create custom rules (see later on this page).
* **Quality Profile**: inclusion in or exclusion from a specific profile

If a quality profile is selected, it is also possible to check for its active severity and whether it is inherited or not. See the Quality Profile documentation for more.

## Rule Details

To see the details of a rule, either click on it, or use the right arrow key. Along with basic rule data, you'll also be able to see which, if any, profiles it's active in and how many open issues have been raised with it.

The following actions are available only if you have the right permissions ("Administer Quality Profiles and Gates"):

* **Add/Remove Tags**:
  * It is possible to add existing tags on a rule, or to create new ones (just enter a new name while typing in the text field).
  * Note that some rules have built-in tags that you cannot remove - they are provided by the plugins which contribute the rules.
* **Extend Description**:
  * You can extend rule descriptions to let users know how your organization is using a particular rule or to give more insight on a rule.
  * Note that the extension will be available to non-admin users as a normal part of the rule details.

<!-- sonarqube -->
## Rule Templates and Custom Rules

Rule Templates are provided by plugins as a basis for users to define their own custom rules in {instance}. To find templates, select the **Show Templates Only** facet from the the "Template" dropdown:

![Rule templates.](/images/rule-templates.png)

To create a custom rule from a template click the **Create** button next to the "Custom Rules" heading and fill in the following information:
* Name
* Key (auto-suggested)
* Description (Markdown format is supported)
* Default Severity
* Status
* The parameters specified by the template

You can navigate from a template to the details of custom rules defined from it by clicking the link in the "Custom Rules" section.

![Rule template details.](/images/rule-template-details.png)

### Custom Rules
Custom Rules are considered like any other rule, except that you can edit or delete them:

![Custom rules.](/images/rules-custom.png)

**Note:** When deleting a custom rule, it is not physically removed from the {instance} instance. Instead, its status is set to "REMOVED". This allows current or old issues related to this rule to be displayed properly in {instance} until they are fully removed.

## Extending Coding Rules

Custom coding rules can be added. See [Adding Coding Rules](/extend/adding-coding-rules/) for detailed information and tutorials.
<!-- /sonarqube -->

## Rule Types and Severities

### How are rules categorized?

The {instance} Quality Model divides rules into four categories: Bugs, Vulnerabilities, Security Hotspots, and Code Smells. Rules are assigned to categories based on the answers to these questions:

**Is the rule about code that is demonstrably wrong, or more likely wrong than not?**  
If the answer is "yes", then it's a Bug rule.  
If not...

**Is the rule about code that could be exploited by a hacker?**  
If so, then it's a Vulnerability rule.  
If not...

**Is the rule about code that is security-sensitive?**  
If so, then it's a Security Hotspot rule.  
If not...

**Is the rule neither a Bug nor a Vulnerability?**  
If so, then it's a Code Smell rule.

## How are severities assigned?
To assign severity to a rule, we ask a further series of questions. The first one is basically:

**What's the worst thing that could happen?**

In answering this question, we try to factor in Murphy's Law without predicting Armageddon.

Then we assess whether the impact and likelihood of the Worst Thing (see _How are severity and likelihood decided?_, below) are high or low, and plug the answers into a truth table:

|          | Impact                 | Likelihood             |
| -------- | ---------------------- | ---------------------- |
| Blocker  | ![](/images/check.svg) | ![](/images/check.svg) |
| Critical | ![](/images/check.svg) | ![](/images/cross.svg) |
| Major    | ![](/images/cross.svg) | ![](/images/check.svg) |
| Minor    | ![](/images/cross.svg) | ![](/images/cross.svg) |

## How are severity and likelihood decided?
To assess the severity of a rule, we start from the Worst Thing (see _How are severities assigned?_, above) and ask category-specific questions.

### Bugs
Impact: **Could the Worst Thing cause the application to crash or to corrupt stored data?**

Likelihood: **What's the probability that the Worst Thing will happen?**

### Vulnerabilities
Impact: **Could the exploitation of the Worst Thing result in significant damage to your assets or your users?**

Likelihood: **What is the probability that a hacker will be able to exploit the Worst Thing?**

### Security Hotspots
Security Hotspots are not assigned severities as it is unknown whether there is truly an issue until review by a Security Auditor. When an auditor converts a Security Hotspot into a Vulnerability, severity is assigned based on the identified Vulnerability (see above).
