---
title: Manage a Team
url: /organizations/manage-team/
---

Members can collaborate on the projects in the organizations to which they belong. Depending on their permisssions within the organization, members can:
* Analyze projects
* Manage project settings (permissions, visibility, quality profiles, ...)
* Update issues
* Manage quality gates and quality profiles
* Administer the organization itself

Members are managed on the "Members" page of the organization. Only organization administrators can manage members. 

## Managing Members Manually
Members are managed manually when synchronization is not available (for Bitbucket Cloud or Azure Devops for example) or when synchronization is deactivated.

### Adding Members
You can add members to an organization using the "Add a member" button. Administrators can search manually for SonarCloud users and add them as members.

## Managing Members in GitHub
For organizations that are bound to GitHub, members can be synchronized with GitHub organizations or managed manually. 

Note that in all cases, members should have a SonarCloud account before being synchronized with GitHub or added manually.

### Synchronizing Members with your GitHub Organization
When [importing](/organizations/overview/) a GitHub organization into SonarCloud, members are automatically synchronized with your GitHub organization.  
This means that each member of your GitHub organization who has a SonarCloud account will be automatically added to the SonarCloud organization, 
and will have direct access in SonarCloud to the organizations they've been added to. 

You can synchronize a bound organization with manually managed members using the "Configure synchronization" button. 
During synchronization, members of the SonarCloud organization who are not part of the GitHub organization are removed from the SonarCloud 
organization and members of the GitHub organization who are not members of the SonarCloud organization are added to the SonarCloud organization.

After creating an organization or activating synchronization, SonarCloud users that are added or removed from the GitHub organization are automatically added or removed from 
the SonarCloud organization. It's not possible to manually add or remove a member when synchronization is activated.

Permissions are not synchronized and must be managed manually (see below).

### Deactivating Member Synchronization
You can deactivate member synchronization using the "Configure synchronization" button. 
When you deactivate member synchronization, no members will be added or removed automatically.
After deactivating synchronization, members will be managed manually.


## Granting permissions
Once users are added or synchronized, organization administrators can grant them permissions to perform specific operations in the organization. It is up to the 
administrators to make sure each member gets the relevant permissions.

To avoid having to manage individual permissions at a project level, organization admins can create groups to manage permissions 
and add new users to those groups on the "Members" page.

## Future evolutions
Future versions of SonarCloud will make this onboarding process easier for BitBucket Cloud, Azure Devops, and others. 
Users' permissions will be retrieved from systems and mapped to SonarCloud permissions on a best-effort basis.