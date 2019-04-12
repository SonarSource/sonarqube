---
title: Delegating Authentication
url: /instance-administration/delegated-auth/
---


SonarQube comes with an onboard user database, as well as the ability to delegate authentication via HTTP Headers, or LDAP.

## HTTP Header Authentication
This feature is designed to delegate user authentication to third-party systems (proxies/servers).

When this feature is activated, SonarQube expects that the authentication is handled prior any query reaching the server. 
The tool that handles the authentication should:

* intercept calls to the SonarQube server
* take care of the authentication
* update the HTTP request header with the relevant SonarQube user information
* re-route the request to SonarQube with the appropriate header information

![HTTP Header Authentication flow](/images/http-header-authentication.png)

All the parameters required to activate and configure this feature are available in SonarQube server configuration file (in _$SONARQUBE-HOME/conf/sonar.properties_).

Using Http header authentication is an easy way integrate your SonarQube deployment with an in-house SSO implementation.

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
Only [groups](http://identitycontrol.blogspot.fr/2007/07/static-vs-dynamic-ldap-groups.html) are supported (not [roles](http://identitycontrol.blogspot.fr/2007/07/static-vs-dynamic-ldap-groups.html)). Only [static groups](http://identitycontrol.blogspot.fr/2007/07/static-vs-dynamic-ldap-groups.html) are supported (not [dynamic groups](http://identitycontrol.blogspot.fr/2007/07/static-vs-dynamic-ldap-groups.html)).

[[warning]]
|When group mapping is configured (i.e the below `ldap.group.*` properties are configured), membership in LDAP server will override any membership locally configured in SonarQube. LDAP server becomes the one and only place to manage group membership (and the info is fetched each time the user logs in).

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

* Kerberos Troubleshooting resources
   * [Enabling Kerberos Logging](http://support.microsoft.com/kb/262177/en-us)
   * [Troubleshooting Kerberos Delegation](https://gallery.technet.microsoft.com/Troubleshooting-Kerberos-72837743)
* Troubleshooting NTLM
   * [Enabling NTLM Logging](http://blogs.technet.com/b/askds/archive/2009/10/08/ntlm-blocking-and-you-application-analysis-and-auditing-methodologies-in-windows-7.aspx)

## Authenticating Via Other Systems
Additionally, several plugins are available to allow delegation to other providers: 
* Crowd - [SonarQube Crowd Plugin](https://github.com/SonarCommunity/sonar-crowd)
* GitHub - [GitHub Authentication Plugin](https://redirect.sonarsource.com/plugins/authgithub.html)
* Bitbucket - [Bitbucket Authentication Plugin](https://github.com/SonarQubeCommunity/sonar-auth-bitbucket)
