---
title: Quality Profiles
url: /instance-administration/quality-profiles/
---

Quality profiles are a key part of your SonarQube configuration.
They define the set of [rules](/user-guide/rules/) to be applied during code analysis.

Every project has a quality profile set for each supported language.
When a project is analyzed, SonarQube determines which languages are used and uses the active quality profile for each of those languages in that specific project.

Go to **Quality Profiles** to see all the currently defined profiles grouped by language.


## Built-in and default profiles

SonarQube comes with a built-in quality profile defined for each supported language, called the **Sonar way** profile (it is marked with the **BUILT-IN** tag in the interface).
The **Sonar way** activates a set of rules that should be applicable to most projects.

In a newly set up instance, the **Sonar way** profile is the default for every language (marked with the **DEFAULT** tag in the interface).
The default profile is used for that language if no other profile is explicitly defined at the project level.
The default profile for a given language can be changed.


## Customizing a quality profile

The **Sonar way** profile is designed to be broadly suitable for most projects, but it is intended only as a starting point.
In most cases, you will want to adjust your profile as the project progresses.

If you have multiple projects, you might also need to have different profiles for each.
You might run into the following situations

* You have different technical requirements from one project to another.
* You want to ensure stronger requirements for some of your projects than for others.

New profiles can be created in two ways:

1. Copying an existing profile and adjusting the copy.
1. Extending an existing profile.


### Copying a quality profile

When you copy a profile, you clone all activated rules of the original.
From here, you independently activate or deactivate rules to fit your needs; your new profile won't inherit changes made to the original profile.

Follow these steps to copy a profile

1. Go to the page of the profile you want to copy (**Quality Profiles** > *profile name*).
1. Select **Copy** from the ![Settings drop-down](/images/gear.png) menu in the upper-right corner of the page.
1. Give your new profile a name and select **Copy**.
1. Modify the copy as needed.


### Extending a quality profile

When you extend a profile, you create a child profile that inherits all the _activated_ rules in the parent profile.
You can then activate additional rules in the child, beyond those that are inherited.
However, you cannot de-activate rules that are activated in the parent.
In other words, extension works by adding rules to the child profile.

Follow these steps to extend a profile:

1. Create a base profile with your core set of rules by selecting the **Create** button on the Quality Profiles page, or use an existing profile as a base profile.
1. Find your base profile (**Quality Profiles** > *profile name*) and select **Extend** from the  ![Settings drop-down](/images/gear.png) menu.
1. After giving your new profile a name, SonarQube opens your new profile page.
1. Below the **Rules** table, select **Activate More** to add rules to your extended profile.
1. From the **Inheritance** table, you can see the hierarchy of inheritance for your profile, and you can change the parent profile by selecting **Change Parent**.

Your new profile has all of the activated rules from the profile you copied, but you can activate or deactivate any rules from the **Rules** table by selecting the numbers in the **Active** and **Inactive** columns.


### Differences between copying and extending 

The key differences between an extension of a profile and a copy are:

* With an extension, you can only activate rules that are deactivated in the parent.
  With a copy, you can activate or de-activate any rules you like.
* With an extension, any changes made to the parent will be automatically reflected in the child.
  This includes rules activated in the parent, rules deactivated in the parent, and new rules added to the parent by Sonar.
  With a copy, changes are not propagated because the copy is entirely independent.

Copied profiles are typically used to establish a new common profile that you want full control over and that can serve as the base profile for all your projects.
Extension is typically used to provide customized profiles for projects which all follow a common base set of rules, but where each also requires different additional ones.


## Quality profile permissions

By default, only users with the global **Administer Quality Profiles** permission can edit quality profiles.
User permissions are defined at **Administration** > **Security** > **Global Permissions**.

SonarQube also allows users with the global **Administer Quality Profiles** permission to give an expert or group of experts permission to manage a specific profile.
These experts only have permission for that specific profile.

Permissions can be granted to manage specific quality profiles on that profile's page (**Quality Profiles** > *profile name*) under **Permissions** by selecting **Grant permissions to more users**.


## Comparing two quality profiles

You can compare the activated rules between two quality profiles.
This is especially useful when you're using a quality profile copied from another profile because you won't automatically inherit new rules added to the original quality profile.

To compare two profiles:

1. From the **Quality Profiles** page, select the name of the first profile you'd like to compare.
1. Select **Compare** from the ![Settings drop-down](/images/gear.png) menu.
1. Select the second profile you'd like to compare from the **Compare with** drop-down menu.

From here you can push rules between the two profiles using the ![Activate rule right](/images/activate_rule_compare1.png) buttons.


## Finding out what has changed in a quality profile

When SonarQube notices that an analysis was performed with a quality profile that is different in some way from the previous analysis, a _quality profile event_ is added to the project's event log.
To see the changes in a profile, navigate to the profile (**Quality Profiles** > *profile name*) and choose **Changelog**.
This can help you understand how profile changes impact the issues raised in an analysis.

Additionally, users with the **Administer Quality Profile** privilege are notified by email each time a built-in profile is updated.
These updates can be caused by updating SonarQube or updating third-party analyzers.


## Importing a quality profile from another SonarQube instance

To import a profile from another SonarQube instance, do the following:

1. From the source SonarQube instance, open the quality profile you want to use.
1. Select **Back up** from the ![Settings drop-down](/images/gear.png) menu.
   This exports the profile as an XML file.
1. From the target SonarQube instance, select the **Restore** button on the **Quality Profiles** main page.
1. Choose the XML file that you exported previously, and select **Restore**.


## Applying profiles to projects

One profile for each language is marked as the default.
Barring any other intervention, all projects that use that language will be analyzed with that profile.
To have a project analyzed by a non-default profile instead, start from **Quality Profiles**, and navigate to your target profile, then use the **Projects** part of the interface to manage which projects are explicitly assigned to that profile.


## Ensuring your quality profile has all relevant new rules

Each time a new SonarQube version is released, new rules are added.
New rules won't appear automatically in your profile unless you're using a built-in profile or a profile extended from a built-in profile.

If you're not using a built-in profile, you can compare your profile to the built-in profile to see which rules you're missing.

Another option is to go to the **Rules** page in SonarQube and use the **Available Since** search facet to see what rules have been added to the platform since the day you upgraded.

And finally, the **Quality Profiles** main page shows recently added rules in the **Recently Added Rules** section on the right side of the page.


## Avoiding deprecated rules

The **Deprecated Rules** section of the **Quality Profiles** page has a pink background and is your first warning that a profile contains deprecated rules.
This section gives the total number of instances of deprecated rule(s) that are currently active in each Quality Profile, and provides a breakdown of deprecated rule(s) per profile.
Selecting the **Deprecated Rules** section takes you either to the **Rules** page or to the relevant Quality Profile to investigate further.

Alternatively, you can perform a **Rules** search for the rules in a profile and use the **Status** rule search facet (in the left sidebar) to narrow the list to the ones that need attention.


## Security

The **Quality Profiles** page can be accessed by any user (even anonymous users).
All users can view every aspect of any profile.
That means anyone can see which rules are included in a profile, which rules have been left out, how a profile has changed over time, and compare the rules between any two profiles.

To create, edit, or delete a profile, a user must be granted the **Administer Quality Profiles** permission.

A project administrator can choose which profiles their project is associated with.
