---
title: Rules
url: /user-guide/rules/
---

SonarQube evaluates your source code against its set of rules to generate issues.
There are four types of rules:

* Code Smell (Maintainability domain)
* Bug (Reliability domain)
* Vulnerability (Security domain)
* Security Hotspot (Security domain)

For Code Smells and Bugs, zero false-positives are expected. 
At least this is the target so that developers don't have to wonder if a fix is required.

For Vulnerabilities, the target is to have more than 80% of issues be true-positives.

Security Hotspot rules draw attention to code that is security-sensitive.
It is expected that more than 80% of the issues will be quickly resolved as "Reviewed" after review by a developer.

The Rules page is the entry point where you can discover all the existing rules or create new ones based on provided templates.


## Rules

By default, when entering the top menu item "Rules", you will see all the available rules installed on your SonarQube instance.
You have the ability to narrow the selection based on search criteria in the left pane:

* **Language**: the language to which a rule applies.
* **Type**: Bug, Vulnerability, Code Smell or Security Hotspot rules.
* **Tag**: it is possible to add tags to rules in order to classify them and to help discover them more easily.
* **Repository**: the engine/analyzer that contributes rules to SonarQube.
* **Default Severity**: the original severity of the rule - as defined by SonarQube.
* **Status**: rules can have 3 different statuses:
  * **Beta**: The rule has been recently implemented and we haven't gotten enough feedback from users yet, so there may be false positives or false negatives.
  * **Deprecated**: The rule should no longer be used because a similar, but more powerful and accurate rule exists.
  * **Ready**: The rule is ready to be used in production.
* **Available Since**: date when a rule was first added on SonarQube. This is useful to list all the new rules since the last upgrade of a plugin for instance.
* **Template**: display rule templates that allow to create custom rules (see later on this page).
* **Quality Profile**: inclusion in or exclusion from a specific profile

If a Quality Profile is selected, it is also possible to check for its active severity and whether it is inherited or not.
See [Quality Profiles](/instance-administration/quality-profiles/) for more information.


## Rule Details

To see the details of a rule, either click on it, or use the right arrow key.
Along with basic rule data, you'll also be able to see which, if any, profiles it's active in and how many open issues have been raised with it.

The following actions are available only if you have the right permissions ("Administer Quality Profiles and Gates"):

* **Add/Remove Tags**:
  * It is possible to add existing tags on a rule, or to create new ones (just enter a new name while typing in the text field).
  * Note that some rules have built-in tags that you cannot remove - they are provided by the plugins which contribute the rules.
* **Extend Description**:
  * You can extend rule descriptions to let users know how your organization is using a particular rule or to give more insight on a rule.
  * Note that the extension will be available to non-admin users as a normal part of the rule details.


## Rule Templates and Custom Rules

Rule Templates are provided by plugins as a basis for users to define their own custom rules in {instance}. To find templates, select the **Show Templates Only** facet from the the "Template" dropdown:

![Rule templates](/images/rule-templates.png)

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


## Rule Types and Severities

### How are rules categorized?

The {instance} Quality Model divides rules into four categories: Bugs, Vulnerabilities, Security Hotspots, and Code Smells. Rules are assigned to categories based on the answers to these questions:

**Is the rule about code that is demonstrably wrong, or more likely wrong than not?**  
If the answer is "yes", then it's a Bug rule.  
If not...

**Is the rule about code that could be exploited by an attacker?**  
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

Likelihood: **What is the probability that an attacker will be able to exploit the Worst Thing?**


### Security Hotspots

Security Hotspots are not assigned severities as it is unknown whether there is truly an underlying vulnerability until they are reviewed.


## What might change after a software update

Sonar developers continually re-evaluate our rules to provide the best results. As a result, the characteristics of some rules may change after an upgrade. This is normal and expected, and is no cause for alarm. 

The following rule charactersitics that may change in an upgrade:

- **Type:** Type (Bug, Vulnerability, Code Smell) updates happen on occasion. When a rule type is updated, its value will update automatically in every profile that uses it. Although the rule will be updated, issues previously raised by the rule will not be updated. For example, if a rule transitioned from Bug to Code Smell, the existing issues will retain their original Bug type, and new issues will get the new type, Code Smell.

- **Severity:** Changes to a rule's default severity will automatically be applied in Quality Profiles where the default severity was used. Although the rule will be updated, existing issues raised by the rule will not be updated. Note that it is possible to override a rule's default severity in a profile, and your custom override should remain intact in your Quality Profile after the upgrade.

- **Tags:** Two types of tags may be attached to a rule: the default tags that come out of the box, and the custom tags added by administrators. When the default tags attached to a rule are updated in SonarQube, those changes will happen automatically. Custom tags associated with a rule will not change.

- **Key:** Can change but this is uncommon. Typically this happens in the rare case that, for whatever reason, a key that was non-normal and needs to be normalized. When the key of a rule is changed, related issues are updated as well, so that they remain related to the re-keyed rule.

- **Status:** Status does not affect the operation of a rule and has no impact on its issues. There are three possible rule statuses: Beta, Ready, and Deprecated. Sometimes, rules are first issued in Beta status and then moved to Ready. Most rules are in Ready status; ready to be used in production. When Sonar developers realize that a rule no longer makes sense, they first deprecate the rule, then eventually drop it.
