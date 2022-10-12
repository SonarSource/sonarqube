---
title: Overview
url: /instance-administration/authentication/overview/
---

SonarQube comes with an onboard user database, as well as the ability to delegate authentication via [HTTP Headers](/instance-administration/authentication/http-header/), [GitHub Authentication](/instance-administration/authentication/github/), [GitLab Authentication](/instance-administration/authentication/gitlab/), [Bitbucket Cloud Authentication](/instance-administration/authentication/bitbucket-cloud/), [SAML](/instance-administration/authentication/saml/overview/), or [LDAP](/instance-administration/authentication/ldap/). Each method offers user identity management, group synchronization/mapping, and authentication.

## Group Mapping
When using group mapping, the following caveats apply regardless of which delegated authentication method is used:
* membership in synchronized groups will override any membership locally configured in SonarQube _at each login_
* membership in a group is synched only if a group with the same name exists in SonarQube
* membership in the default group `sonar-users` remains (this is a built-in group) even if the group does not exist in the identity provider

[[warning]]
|When group mapping is configured, the delegated authentication source becomes the only place to manage group membership, and the user's groups are re-fetched with each log in.

## Revoking tokens for deactivated users
When SonarQube authentication is delegated to an external identity provider, deactivating a user on the identity provider side does not remove any tokens associated with the user on the SonarQube side. We recommend deactivating the user in SonarQube at **Administration > Security > Users** by selecting **Deactivate** from the ![Settings drop-down](/images/gear.png) drop-down menu to ensure tokens associated with that user can no longer be used.

## Delete users' personal information
SonarQube offers the possibility to anonymize the data of deactivated users. This comes in handy when you want to ensure that the personal data of deactivated users is not retained, for example, for legal compliance.

You can delete a user's personal information by following the steps listed above to revoke tokens for any deactivated users and select the checkbox titled **Delete user’s personal information**.

You can also delete personal information using the API. First, the user needs to be deactivated, then an admin can use the webservice `/api/users/anonymize` and pass to it the login of a deactivated user to replace all personal data of the user with anonymized data. Note that the admin is able to retrieve the logins of deactivated users by using `/api/users/search` endpoint with the appropriate parameter.


This feature has the following limitations:
- Deleting the personal information of a user will change its login, making it impossible to reactivate the user by recreating a user with the old login.
- The user’s login may still be stored in issue changelogs and the user’s login, name and email address may still be stored in audit entries. Audit entries are purged by default after 30 days.
- Deleted users may still appear in the list of authors and other locations due to SCM data.
- Some columns in the database may contain parts of the user's login if the user was created before the instance was upgraded to SonarQube 8.3.