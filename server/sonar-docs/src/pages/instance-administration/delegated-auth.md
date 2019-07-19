---
title: Delegating Authentication
url: /instance-administration/delegated-auth/
---


SonarQube comes with an onboard user database, as well as the ability to delegate authentication via HTTP Headers, GitHub Authentication, SAML, or LDAP. Each method offers user identity management, group synchronization/mapping and authentication.

## Group Mapping
When using group mapping, the following caveats apply regardless of which delegated authentication method is used:
* membership in synchronized groups will override any membership locally configured in SonarQube _at each login_
* membership in a group is synched only if a group with the same name exists in SonarQube
* membership in the default group `sonar-users` remains (this is a built-in group) even if the group does not exist in the identity provider

[[warning]]
|When group mapping is configured, the delegated authentication source becomes the one and only place to manage group membership, and the user's groups are re-fetched with each log in.


## HTTP Header Authentication
You can delegate user authentication to third-party systems (proxies/servers) using HTTP Header Authentication.

When this feature is activated, SonarQube expects that the authentication is handled prior any query reaching the server. 
The tool that handles the authentication should:

* intercept calls to the SonarQube server
* take care of the authentication
* update the HTTP request header with the relevant SonarQube user information
* re-route the request to SonarQube with the appropriate header information

![HTTP Header Authentication flow](/images/http-header-authentication.png)

All the parameters required to activate and configure this feature are available in SonarQube server configuration file (in _$SONARQUBE-HOME/conf/sonar.properties_).

Using Http header authentication is an easy way integrate your SonarQube deployment with an in-house SSO implementation.

## GitHub Authentication
You can delegate authentication to GitHub Enterprise using a dedicated GitHub OAuth application. Alternately, if you're using the pull request decoration provided as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/) you can harness the [GitHub application needed for PR decoration](/instance-administration/github-application/) to also provide authentication.

### Dedicated GitHub OAuth application
1. You'll need to first create a GitHub OAuth application. Click [here](https://developer.github.com/apps/building-oauth-apps/creating-an-oauth-app/) for general instructions:
   1. "Homepage URL" is the public URL to your SonarQube server, for example "https://sonarqube.mycompany.com". For security reasons HTTP is not supported. HTTPS must be used. The public URL is configured in SonarQube at **[Administration -> General -> Server base URL](/#sonarqube-admin#/admin/settings)**
   1. "Authorization callback URL" is <Homepage URL>/oauth2/callback, for example "https://sonarqube.mycompany.com/oauth2/callback"
1. In SonarQube navigate to **[Administration > Configuration > General Settings > GitHub](/#sonarqube-admin#/admin/settings?category=github)**:
   1. Set **Enabled** to `true`
   1. Set the **Client ID** to the value provided by the GitHub developer application
   1. Set the **Client Secret** to the value provided by the GitHub developer application
  
On the login form, the new "Log in with GitHub" button allows users to connect with their GitHub Enterprise accounts. 

### Re-use GitHub PR decoration application
1. In the GitHub app, in **Permission & events > User permissions**: Add **Read-only** access in **Emails**.
1. In SonarQube settings, update the **Client ID** and **Client Secret** and use values defined in the GitHub app

If you previously used a dedicated GitHub OAuth application for authentication, it can be removed.

## SAML Authentication  
You can delegate authentication to a SAML 2.0 Identity Provider using SAML Authentication.

### Limitations
* SAML requests are not signed. Client signature validation should be disabled in the Identity Provider.
* SAML encrypted responses are not supported. SAML encryption should be disabled in the Identity Provider.

### Example: Using Keycloak as a SAML Identity Provider
The following example may be useful if you're using Keycloak as a SAML Identity Provider. If you're not using Keycloak, your settings are likely to be different.

[[collapse]]
| ## In the Keycloak server, create a new SAML client
| Create a new client
|
| 1. "Client ID" is something like "sonarqube" 
| 1. "Client Protocol" must be set to "saml"
| 1. "Client SAML Endpoint" can be left empty
|
| Configure the new client
|
| 1. in Settings
|    1. Set"Client Signature Required" to OFF
|    1. Set "Valid Redirect URIs" to "<Your SonarQube URL>/oauth2/callback/*, E.G https://sonarqube.mycompany.com/oauth2/callback/saml
| 1. in Client Scopes > Default Client Scopes , remove "role_list" from "Assigned Default Client Scopes" (to prevent the error `com.onelogin.saml2.exception.ValidationError: Found an Attribute element with duplicated Name` during authentication)
| 1. In Mappers create a mapper for each user attribute (Note that values provided below for Name, SAML Attribute Name, Role Attribute Name are only example values): 
|    1. Create a mapper for the login: 
|       * Name: Login
|       * Mapper Type: User Property
|       * Property: Username (Note that the login should not contain any special characters other than `.-_@` to meet SonarQube restrictions.)
|       * SAML Attribute Name: login
|    1. Create a mapper for the name: 
|       * Name: Name
|       * Mapper Type: User Property
|       * User Attribute: Username (It can also be another attribute you would previously have specified for the users)
|       * SAML Attribute Name: name
|    1. (Optional) Create a mapper for the email: 
|       * Name: Email
|       * Mapper Type: User Property
|       * Property: Email
|       * SAML Attribute Name: email
|    1. (Optional) Create a mapper for the groups (If you rely on a list of roles defined in "Roles" of the Realm (not in "Roles" of the client)):
|       * Name: Groups
|       * Mapper Type: Role list
|       * Role Attribute Name: groups
|       * Single Role Attribute: ON
|    1. If you rely on a list of groups defined in "Groups":
|       * Name: Groups
|       * Mapper Type: Group list
|       * Role Attribute Name: groups
|       * Single Role Attribute: ON
|       * Full Group Path: OFF
|
| Download the XML configuration file in Installations > Format Option > SAML Metadata IDPSSODescriptor

[[collapse]]
| ## In SonarQube, Configure SAML authentication
| Go to **[Administration > Configuration > General Settings > SAML > Authentication](/#sonarqube-admin#/admin/settings?category=saml)**
| * **Enabled** should be set to true
| * **Application ID** is the value of the "Client ID" you set in Keycloak (for example "sonarqube")
| * **Provider ID** is the value of the "EntityDescriptor" > "entityID" attribute in the XML configuration file (for example "http://keycloak:8080/auth/realms/sonarqube" where sonarqube is the name of the realm)
| * **SAML login url** is the value of "SingleSignOnService" > "Location" attribute in the XML configuration file (for example "http://keycloak:8080/auth/realms/sonarqube/protocol/saml")
| * **Provider certificate** is the value of "dsig:X509Certificate" node in the XML configuration file
| * **SAML user login attribute** is the value set in the login mapper in "SAML Attribute Name"
| * **SAML user name attribute** is the value set in the name mapper in "SAML Attribute Name"
| * (Optional) **SAML user email attribute** is the value set in the email mapper in "SAML Attribute Name"
| * (Optional) **SAML group attribute** is the value set in the groups mapper in "Role/Group Attribute Name"
|
| In the login form, the new button "Log in with SAML" allows users to connect with their SAML account.

## LDAP Authentication
You can configure SonarQube authentication and authorization to an LDAP server (including LDAP Service of Active Directory) by configuring the correct values in _$SONARQUBE-HOME/conf/sonar.properties_.

The main features are:

* Password checking against the external authentication engine.
* Automatic synchronization of usernames and emails.
* Automatic synchronization of relationships between users and groups (authorization).
* Ability to authenticate against both the external and the internal authentication systems. There is an automatic fallback on SonarQube internal system if the LDAP server is down.
* During the first authentication trial, if the user's password is correct, the SonarQube database is automatically populated with the new user. Each time a user logs into SonarQube, the username, the email and the groups this user belongs to that are refreshed in the SonarQube database. You can choose to have group membership synchronized as well, but this is not the default.


&nbsp;| Apache DS | OpenLDAP | Open DS | Active Directory
----|-----------|----------|---------|-----------------
Anonymous | ![](/images/check.svg) |![](/images/check.svg) |![](/images/check.svg) |  &nbsp;
Simple|![](/images/check.svg)|![](/images/check.svg)|![](/images/check.svg)|![](/images/check.svg)
LDAPS|![](/images/check.svg)|![](/images/check.svg)|  |![](/images/check.svg)
DIGEST-MD5|![](/images/check.svg)|  |![](/images/check.svg)|![](/images/check.svg)
CRAM-MD5|![](/images/check.svg)|  |![](/images/check.svg)|![](/images/check.svg)
GSSAPI|![](/images/check.svg)|  |  |  
![](/images/check.svg) = successfully tested

### Setup
1. Configure the LDAP plugin by editing _$SONARQUBE-HOME/conf/sonar.properties_ (see table below)
2. Restart the SonarQube server and check the log file for:
```
INFO org.sonar.INFO Security realm: LDAP ...
INFO o.s.p.l.LdapContextFactory Test LDAP connection: OK
```
1. Log into SonarQube
1. On logout users will be presented a login page (_/sessions/login_), where they can choose to login as technical user or a domain user by passing appropriate credentials

From SonarScanners, we recommend using [local technical users](/instance-administration/security/) for authentication against SonarQube Server.

**General Configuration**

Property|Description|Default value|Required|Example
---|---|---|---|---
`sonar.security.realm`|Set this to `LDAP` authenticate first against the external sytem. If the external system is not reachable or if the user is not defined in the external system, authentication will be performed against SonarQube's internal database.| none |Yes|`LDAP` (only possible value)
`sonar.authenticator.downcase`|Set to true when connecting to a LDAP server using a case-insensitive setup.|`false`|No
`ldap.url`|URL of the LDAP server. If you are using ldaps, you should install the server certificate into the Java truststore.| none |Yes|`ldap://localhost:10389`
`ldap.bindDn`|The username of an LDAP user to connect (or bind) with. Leave this blank for anonymous access to the LDAP directory.|none|No|`cn=sonar,ou=users,o=mycompany`
`ldap.bindPassword`|The password of the user to connect with. Leave this blank for anonymous access to the LDAP directory.|none|No|`secret`
`ldap.authentication`|Possible values: `simple`, `CRAM-MD5`, `DIGEST-MD5`, `GSSAPI`. See [the tutorial on authentication mechanisms](http://java.sun.com/products/jndi/tutorial/ldap/security/auth.html)|`simple`|No
`ldap.realm`|See [Digest-MD5 Authentication](http://java.sun.com/products/jndi/tutorial/ldap/security/digest.html), [CRAM-MD5 Authentication](http://java.sun.com/products/jndi/tutorial/ldap/security/crammd5.html)| none|No|example.org
`ldap.contextFactoryClass`|Context factory class.|`com.sun.jndi.ldap.LdapCtxFactory`|No
`ldap.StartTLS`|Enable use of `StartTLS`|`false`|No
`ldap.followReferrals`|Follow referrals or not. See [Referrals in the JNDI](http://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html)|`true`

**User Mapping**

Property|Description|Default value|Required|Example for Active Directory
---|---|---|---|---
`ldap.user.baseDn`|Distinguished Name (DN) of the root node in LDAP from which to search for users.|None|Yes|`cn=users,dc=example,dc=org`
`ldap.user.request`|LDAP user request.|`(&(objectClass=inetOrgPerson)(uid={login}))`|No|`(&(objectClass=user)(sAMAccountName={login}))`
`ldap.user.realNameAttribute`|Attribute in LDAP defining the user’s real name.|`cn`|No|  
`ldap.user.emailAttribute`|Attribute in LDAP defining the user’s email.|`mail`|No| 

**Group Mapping**
Only groups (not roles) and static groups (not dynamic groups) are supported. Click [here](http://identitycontrol.blogspot.fr/2007/07/static-vs-dynamic-ldap-groups.html) for more information.

For the delegation of authorization, [groups must be first defined in SonarQube](/instance-administration/security/). Then, the following properties must be defined to allow SonarQube to automatically synchronize the relationships between users and groups.

Property|Description|Default value|Required|Example for Active Directory
---|---|---|---|---
`ldap.group.baseDn`|Distinguished Name (DN) of the root node in LDAP from which to search for groups.|none|No|`cn=groups,dc=example,dc=org`
`ldap.group.request`|LDAP group request.|`(&(objectClass=groupOfUniqueNames)(uniqueMember={dn}))`|No|`(&(objectClass=group)(member={dn}))`
`ldap.group.idAttribute`|Property used to specifiy the attribute to be used for returning the list of user groups in the compatibility mode.|`cn`|No|`sAMAccountName`

### Sample Configuration
```
# LDAP configuration
# General Configuration
sonar.security.realm=LDAP
ldap.url=ldap://myserver.mycompany.com
ldap.bindDn=my_bind_dn
ldap.bindPassword=my_bind_password
  
# User Configuration
ldap.user.baseDn=ou=Users,dc=mycompany,dc=com
ldap.user.request=(&(objectClass=inetOrgPerson)(uid={login}))
ldap.user.realNameAttribute=cn
ldap.user.emailAttribute=mail
 
# Group Configuration
ldap.group.baseDn=ou=Groups,dc=sonarsource,dc=com
ldap.group.request=(&(objectClass=posixGroup)(memberUid={uid}))
```

## Advanced LDAP Topics
### Authentication Methods
* **`Anonymous`** -  Used when only read-only access to non-protected entries and attributes is needed when binding to the LDAP server.
* **`Simple`** Simple authentication is not recommended for production deployments not using the ldaps secure protocol since it sends a cleartext password over the network.
* **`CRAM-MD5`** - The Challenge-Response Authentication Method (CRAM) based on the HMAC-MD5 MAC algorithm ([RFC 2195](http://tools.ietf.org/html/rfc2195)).
* **`DIGEST-MD5`** - This is an improvement on the CRAM-MD5 authentication method ([RFC 2831](http://www.ietf.org/rfc/rfc2831.txt)).
* **`GSSAPI`** - GSS-API is Generic Security Service API ([RFC 2744](http://www.ietf.org/rfc/rfc2744.txt)). One of the most popular security services available for GSS-API is the Kerberos v5, used in Microsoft's Windows 2000 platform.

For a full discussion of LDAP authentication approaches, see [RFC 2829](http://www.ietf.org/rfc/rfc2829.txt) and [RFC 2251](http://www.ietf.org/rfc/rfc2251.txt).

### Multiple Servers
To configure multiple servers:
```
# List the different servers
ldap.servers=server1,server2
  
# Configure server1
ldap.server1.url=ldap://server1:1389
ldap.server1.user.baseDn=dc=dept1,dc=com
...
 
# Configure server2
ldap.server2.url=ldap://server2:1389
ldap.server2.user.baseDn=dc=dept2,dc=com
...
```

Authentication will be tried on each server, in the order they are listed in the configurations, until one succeeds. User/Group mapping will be performed against the first server on which the user is found.

Note that all the LDAP servers must be available while (re)starting the SonarQube server.

### Troubleshooting
* Detailed connection logs (and potential error codes received from LDAP server) are output to SonarQube's _$SONARQUBE_HOME/logs/web.log_, when logging is in `DEBUG` mode.

* Time out when running SonarQube analysis using LDAP
Java parameters are documented here: http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html. Such parameters can be set in `sonar.web.javaAdditionalOpts` in _$SONARQUBE-HOME/conf/sonar.properties_.
