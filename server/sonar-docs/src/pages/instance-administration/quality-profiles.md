---
title: Quality Profiles
url: /instance-administration/quality-profiles/
---

**Quality Profiles** are a core component of SonarQube where you define sets of [**Rules**](/user-guide/rules/) that, when violated, raise issues on your codebase (example: Methods should not have a Cognitive Complexity higher than 15). Each individual language has its own Quality Profile.

To manage Quality Profiles, navigate to the [**Quality Profiles**](/#sonarqube#/profiles) page. Here, you'll find the Quality Profiles grouped by language.

Ideally, all of your projects will be measured with the same Quality Profile, but that isn't _always_ practical. For example, you might run into the following situations:

- You have different technical requirements from one project to another (for example, different rules might apply to a threaded/non-threaded Java application).
- You want to ensure stronger requirements for some of your projects (for example, internal frameworks).

You can define as many Quality Profiles as needed to fit your specific needs.

## Default Quality Profiles

Each language must have a default Quality Profile (marked with the Default tag). Projects that are not explicitly assigned to specific Quality Profiles are analyzed using the default Quality Profiles. There is also at least one built-in **Sonar way** Quality Profile for each language. SonarSource creates these Quality Profiles with rules that generally apply to most projects. 

The **Sonar way** Quality Profiles are a good starting point as you begin analyzing code, and they start out as the default Quality Profiles for each language. However, we recommend that you **Copy** this profile (see **Copying a profile** below) and begin to fine-tune the rules. Why?

- Default Quality Profiles are not editable, so you won't be able to customize the **Sonar way** to your needs.
- The **Sonar way** becomes a baseline against which you can track your own Quality Profiles.
- The **Sonar way** may be updated over time to adjust which rules are included and adjust rule severities.

## Quality Profile permissions

By default, only users with the **Administer Quality Profiles** permission can edit Quality Profiles. This is set at **Administration > Security > Global Permissions**.

SonarQube also lets you to grant permission to users or groups for specific Quality Profiles, so you can delegate profile management to a group of experts for that language.  These users or groups only have permissions for that specific profile, not all Quality Profiles.

A user with the **Administer Quality Profiles** permission or individual edit rights for a specific Quality Profile can grant permissions on Quality Profile pages (**Quality Profiles > [ Profile Name ]**) under the **Permissions** heading.

## Copying a Quality Profile
Copying a profile works well when you need to make a few changes to a built-in profile. When you copy a profile, you start off with all of the activated rules from the profile you copied from. From here, you can activate or deactivate rules to fit your needs. After copying a profile, your new profile won't inherit any changes made to the original profile.

Follow these steps to copy a profile: 

1. Go to the Quality Profile page (**Quality Profiles > [ Profile Name ]**) of the profile you want to copy.
1. Select **Copy** from the ![Settings drop-down](/images/gear.png) drop-down menu  in the upper-right corner of the page. 
1. Give your new Quality Profile a name and click **Copy**

## Extending a Quality Profile
Extending a profile works well when many or all of your projects follow a set of common rules, but some of your projects also need to follow additional rules. When you extend a profile, you create a child profile based on a parent profile. This child profile inherits all of the rule settings from the parent profile. If rules are activated or deactivated in the parent profile, they're activated or deactivated in the child profile. 

While you can activate rules in your child profile that are deactivated in the parent profile, you cannot deactivate rules in the child profile that are active in the parent profile.

Follow these steps to extend a profile:

1. Create a base profile with your core set of rules by clicking the **Create** button on the **Quality Profiles** page, or use an existing profile as a base profile.
1. From the **Quality Profiles** page (**Quality Profiles > [ Profile Name ]**), find your base profile in the list of Quality Profiles and select **Extend** from the ![Settings drop-down](/images/gear.png) drop-down menu.
1. After giving your new profile a name, SonarQube opens your new profiles page. 
1. Below the **Rules** table, click **Activate More** to add rules to your extended profile. 
1. From the **Inheritance** table, you can see the hierarchy of inheritance for your profile, and you can change the parent profile by clicking the **Change Parent** button. 

Your new profile has all of the activated rules from the profile you copied, but you can activate or deactivate any rules from the **Rules** table by clicking the numbers in the **Active** and **Inactive** columns. 

When you copy a profile, your new profile does not inherit any future rule updates made to the original profile. 

## Comparing two Quality Profiles
You can compare the activated rules between two Quality Profiles. This is especially useful when you're using a Quality Profile copied from another profile as you won't automatically inherit new rules added to the original Quality Profile. Comparing your custom Quality Profile to the original Quality Profile shows any additional activated rules that aren't in your Quality Profile.

To compare two profiles:

1. From the **Quality Profiles** page, click the name of the first Quality Profile you'd like to compare. 
1. Select **Compare** from the ![Settings drop-down](/images/gear.png) drop-down menu. 
1. Select the second Quality Profile you'd like to compare from the **Compare with** drop-down menu.

From here you can activate rules between the two profiles using the ![Activate rule right](/images/activate_rule_compare1.png) buttons.

## Knowing what's changed in a Quality Profile
When SonarQube notices that an analysis was performed with a Quality Profile that is different in some way from the previous analysis, a Quality Profile event is added to the project's event log. To see the changes in a profile, navigate to the profile (**Quality Profiles > [ Profile Name ]**) and choose **Changelog**. This may help you understand how profile changes impact the issues raised in an analysis.

Additionally, users with Quality Profile administration privileges are notified by email each time a built-in profile is updated. These updates can be caused by updating SonarQube or updating third-party analyzers.

## Using a Quality Profile on another SonarQube instance
To use a profile from one SonarQube instance on another SonarQube instance, take the following steps:

1. From the source SonarQube instance, open the Quality Profile you want to use. 
1. Select **Back up** from the ![Settings drop-down](/images/gear.png) drop-down menu. This exports the profile as an XML file.
1. From the target SonarQube instance, click the **Restore** button on the **Quality Profiles** main page.
1. Choose the XML file that you exported previously, and click **Restore**.

## Using a non-default profile on a project
One profile for each language is marked as the default profile. Barring any other intervention, all projects that use that language will be analyzed with that profile. To have a project analyzed by a non-default profile instead, start from **Quality Profiles**, and click through on your target profile, then use the Projects part of the interface to manage which projects are explicitly assigned to the profile.

## Ensuring your Quality Profile has all relevant new rules

Each time a new SonarQube version is released, new rules are added. New rules won't appear automatically in your profile, however, unless you're using a built-in profile or a profile extended from a built-in profile (see the **Extending a profile** section above).

If you're not using a built-in profile, you can compare your profile to the built-in profile to see what new on-by-default rules you're missing (see the **Comparing two Quality Profiles** section above).

Another option is to go to the **Rules** page in SonarQube, and use the **Available Since** search facet to see what rules have been added to the platform since the day you upgraded.

And finally, the **Quality Profiles** main page shows recently added rules in the **Recently Added Rules** section on the right side of the page.

## Avoiding deprecated rules

The **Deprecated Rules** section of the **Rules** page is your first warning that a profile contains deprecated rules. This section with a pink background gives the total number of instances of deprecated rules that are currently active in profiles and a breakdown of deprecated rule count per profile. Clicking through here takes you to the **Rules** page to edit the profile in question.

Alternately, you can perform a **Rules** search for the rules in a profile (either manually or by clicking through from the **Quality Profiles** page) and use the **Status** rule search facet to narrow the list to the ones that need attention.

## Security

The Quality Profiles service can be accessed by any user (even anonymous users). All users can view every aspect of a Quality Profile. That means anyone can see which rules are included in a profile, which rules have been left out, how a profile has changed over time, and compare the rules in any two profiles.

To make rule profile changes (create, edit, or delete) users must be granted the **Administer Quality Profiles and Gates** permission.

A **project administrator** can choose which profiles their project is associated with. See Project Settings for more.
