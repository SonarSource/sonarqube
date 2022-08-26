---
title: Overview
url: /instance-administration/authentication/overview/
---

SonarQube comes with an onboard user database, as well as the ability to delegate authentication via HTTP Headers, GitHub Authentication, GitLab Authentication, Bitbucket Cloud Authentication, SAML, or LDAP. Each method offers user identity management, group synchronization/mapping, and authentication.

## Group Mapping
When using group mapping, the following caveats apply regardless of which delegated authentication method is used:
* membership in synchronized groups will override any membership locally configured in SonarQube _at each login_
* membership in a group is synched only if a group with the same name exists in SonarQube
* membership in the default group `sonar-users` remains (this is a built-in group) even if the group does not exist in the identity provider

[[warning]]
|When group mapping is configured, the delegated authentication source becomes the only place to manage group membership, and the user's groups are re-fetched with each log in.

## Revoking tokens for deactivated users
When SonarQube authentication is delegated to an external identity provider, deactivating a user on the identity provider side does not remove any tokens associated with the user on the SonarQube side. We recommend deactivating the user in SonarQube at **Administration > Security > Users** by selecting **Deactivate** from the ![Settings drop-down](/images/gear.png) drop-down menu to ensure tokens associated with that user can no longer be used.
