---
title: Manage a Team
url: /organizations/manage-team/
---

Members can collaborate on the projects in the organizations to which they belong. Depending on their permisssions within the organization, members can:
* Analyse projects
* Manage project settings (permissions, visibility, quality profiles, ...)
* Update issues
* Manage quality gates and quality profiles
* Administer the organization itself

## Adding Members

Adding members is done on the "Members" page of the organization, and this can be done only by an administrator of 
the organization.

Adding a user as a member is possible only if that user has already signed up on SonarCloud. If the user never authenticated to
the system, the administrator will simply not be able to find the user in the search modal window.

## Granting permissions

Once added, a user can be granted permissions to perform various operations in the organization. It is up to the 
administrator who added the user to make sure that she gets the relevant permissions.

Organization admins will prefer to create groups to manage permissions, and add new users to those
groups through the "Members" page. With such an approach, they won't have to manage individal permissions at
project level for instance.

## Future evolutions

Future versions of SonarCloud will make this onboarding process easier thanks to better integrations with GitHub, 
Bitbucket Cloud and Azure DevOps: users won't have to sign up prior to joining an organization, and their permissions will be retrieved at best from the ones existing on the other systems.
