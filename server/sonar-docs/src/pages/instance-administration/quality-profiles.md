---
title: Quality Profiles
url: /instance-administration/quality-profiles/
---

## Overview

**Quality Profiles** are a core component of {instance}, since they are where you define sets of [**Rules**](/user-guide/rules/) that when violated should raise issues on your codebase (example: Methods should not have a Cognitive Complexity higher than 15). Quality Profiles are defined for individual languages.

To manage Quality Profiles, browse to the the [**Quality Profiles**](/#sonarqube#/profiles) page <!-- sonarcloud --> of your organization<!-- /sonarcloud --> where you'll find Quality Profiles grouped by language.

Ideally all of your projects will be measured with the same Quality Profiles, but that is not _always_ practical. In some cases, you may find that:

- You have different technical requirements from one project to another (different rules might apply to a threaded/non-threaded Java application)
- You want to ensure stronger requirements for some of your projects (internal frameworks, for example)

While it's recommended to have as few Quality Profiles as possible to ensure consistency across projects, you can define as many Quality Profiles as are necessary to fit your specific needs.

Each language must have a default Quality Profile (marked with the Default tag). Projects that are not explicitly assigned to specific Quality Profiles will be analyzed using the default Quality Profiles. There is also at least one built-in Quality Profile (the **Sonar way**) per language. These Quality Profiles are designed by SonarSource with rules that are generally applicable for most projects. 

The Sonar way Quality Profiles are a good starting-point as you begin analyzing code, and they start out as the default Quality Profiles for each language. That being said, we recommend that you **Copy** this profile and begin to fine-tune the contents. Why?

- Default Quality Profiles are not editable, so you won't be able to customize the Sonar way to your needs
- The Sonar way becomes a baseline against which you can track your own Quality Profiles
- The Sonar way may be updated over time to adjust which rules are included and adjust rule severities.

## How do I...

### Delegate the management of Quality Profiles to someone else?

By default, only users with the "Administer Quality Profiles" permission can edit Quality Profiles. But in large organizations, it may not be desirable to grant permissions to change all the Quality Profiles without distinction. That's why you can also grant users/groups the permission to edit an individual Quality Profile so that, for instance, the management of the Swift profile can be delegated to a group of Swift experts, and the same for COBOL, ...

This delegation of permission can only be performed by someone who already has the "Administer Quality Profiles" permission or individual edit rights on the profile to which additional permissions should be granted. The interface to grant individual permissions is available on the profile detail page.

### Copy the rules from one profile to another?

Many times people want to work from a profile that's based on a built-in profile without actually using the built-in profile. The easiest thing to do in this case is to go to the original profile, we'll call it _Source_, in **Quality Profiles**. From there, click through on the total number of rules in _Source_ to land on the **Rules** page at a pre-narrowed search of _Source_'s rules. Use **Bulk Activate** to turn Source's rules on in your target profile.

### Know what's changed in a profile?

When {instance} notices that an analysis was performed with a profile that is different in some way from the previous analysis, a Quality Profile event is added to the project's event log. To see the changes in a profile, navigate to the profile (**Quality Profiles > [ Profile Name ]**), and choose **Changelog**. This may help you understand how profile changes impact the issues raised in an analysis.

Additionally, users with Quality Profile administration privileges are notified by email each time a built-in profile (one that is provided directly by an analyzer) is updated. These updates can only be caused by analyzer updates.

### Copy a profile from one SonarQube instance to another?

Use the **Back up** feature on the source instance to export the profile to an XML file. Use the **Restore Profile** feature on the target instance to import the file. Note that some [limitations](https://jira.sonarsource.com/browse/SONAR-5366) on this feature exist.

### Apply a core set of rules plus additional rules to a project?

Let's say your company has a minimum set of coding rules that all teams must follow, but you want to add rules that are specific to the in use technology in your project. Those rules are good for your team, but irrelevant or even misleading for others. This situation calls for inheritance. Set up a base profile, we'll call it _Root_ with your core set of rules. Then create a child profile, we'll call it _Sprout_. Once it's created, you can **Change parent** to inherit from _Root_, then add your missing rules.

Any profile that inherits from another Quality Profile will be updated when the parent Quality Profile is updated.

### Make sure my non-default profile is used on a project?

One profile for each language is marked the default. Barring any other intervention, all projects that use that language will be analyzed with that profile. To have a project analyzed by a non-default profile instead, start from **Quality Profiles**, and click through on your target profile, then use the Projects part of the interface to manage which projects are explicitly assigned to the profile.

### Make sure I've got all the relevant new rules in my profile?

Each time a language plugin update is released, new rules are added, but they won't appear automatically in your profile unless you're using a built-in profile such as _Sonar way_.

If you're not using a built-in profile, you can compare your profile to the built-in profile to see what new on-by-default rules you're missing.

Another option is to go to the **Rules** space, and use the **Available Since** search facet to see what rules have been added to the platform since the day you upgraded the relevant plugin.

And finally, the profile interface itself will help you be aware of rules added in a new plugin version in the **Latest New Rules** section on the right of the interface.

### Compare two profiles?

Starting from the **Quality Profiles** page, click through on one of the profiles you'd like to compare, then use the **Actions > Compare** interface to select the second profile and see the differences.

### Make sure I don't have any deprecated rules in my profile?

The **Deprecated Rules** section of the rules interface itself is your first warning that a profile contains deprecated rules. This pink-background section gives the total number of instances of deprecated rules that are currently active in profiles, and a breakdown of deprecated count per profile. A click-through here takes you to the **Rules** page to edit the profile in question.

Alternately, you can perform a **Rules** search for the rules in a profile (either manually or by clicking-through from **Quality Profiles** page) and use the **Status** rule search facet to narrow the list to the ones that need attention.

## Security

The Quality Profiles service can be accessed by any user (even anonymous users). All users can view every aspect of a profile. That means anyone can see which rules are included in a profile, and which ones have been left out, see how a profile has changed over time, and compare the rules in any two profiles.

To make rule profile changes (create, edit or delete) users must be granted the **Administer Quality Profiles and Gates** permission.

A **project administrator** can choose which profiles his project is associated with. See Project Settings for more.
