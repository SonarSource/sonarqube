---
title: Security
url: /instance-administration/security/
---

## Overview

SonarQube comes with a number of global security features:

* on-board authentication and authorization mechanisms
* the ability to force users to authenticate before they can see any part of a SonarQube instance
* the ability to delegate to authentication (for more see [Delegating Authentication](/instance-administration/delegated-auth/))

Additionally, you can configure at a group or user level who can:
* see that a project even exists
* access a project's source code 
* administer a project (set exclusion patterns, tune plugin configuration for that project, etc.)
* administer Quality Profiles, Quality Gates, and the SonarQube instance itself.


Another aspect of security is the encryption of settings such as passwords. SonarQube provides a built-in mechanism to encrypt settings.

## Authentication
The first question that should be answered when setting the security strategy for SonarQube is: Can anonymous users browse the SonarQube instance or is authentication be required? 

To force user authentication, log in as a system administrator, go to **[Administration > Configuration > General Settings > Security](/#sonarqube-admin#/admin/settings?category=security)**, and set the **Force user authentication** property to true. 

[[info]]
| SonarQube does not support sharing email addresses across multiple users.


### Authentication Mechanisms
Authentication can be managed through a number of mechanisms:

* Via the SonarQube built-in users/groups database
* Via external identity providers such as an LDAP server (including LDAP Service of Active Directory), GitHub etc. See the Authentication & Authorization section of the Plugin Library.
* Via HTTP headers

### Technical Users
When you create a user in SonarQube's own database, it is considered local and will only be authenticated against SonarQube's own user/group database rather than against any external tool (LDAP, Active Directory, Crowd, etc.). By default `admin` is a local account.

Similarly, all non-local accounts will be authenticated only against the external tool. 

An Administrator can manage tokens on a user's behalf via **[Administration > Security > Users](/#sonarqube-admin#/admin/users)**. From here, click in the user's **Tokens** column to see the user's existing tokens, and either revoke existing tokens or generate new ones. Once established, a token is the only credential needed to run an analysis. Tokens should be passed as the value of the `sonar.login` property.

### Default Admin Credentials
When installing SonarQube, a default user with Administer System permission is created automatically:

* Login: admin
* Password: admin

## Reinstating Admin Access
If you changed and then lost the `admin` password, you can reset it using the following query:
```
update users set crypted_password = '$2a$12$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi', salt=null, hash_method='BCRYPT' where login = 'admin'
```
If you've deleted `admin` and subsequently locked out the other users with global administrative permissions, you'll need to re-grant `admin` to a user with the following query:
```
INSERT INTO user_roles(user_id, role) VALUES ((select id from users where login='mylogin'), 'admin');
```

## Authorization
The way authorization is implemented in SonarQube is pretty standard. It is possible to create as many users and groups of users as needed. The users can then be attached (or not) to (multiple) groups. Groups and / or users are then given (multiple) permissions. The permissions grant access to projects, services and functionalities.

To administer groups and users, choose **Administration > Security**, and use the sub-menu items.

### User
Multiple integrations that allow the delegation of authentication are available (see the [Plugin Library](https://redirect.sonarsource.com/doc/plugin-library.html) and [Other Plugins](https://docs.sonarqube.org/display/PLUG/Other+Plugins), but you can manually create and edit users at **[Settings > Security > Users](/#sonarqube-admin#/admin/users)**. For manually-created users, login and password can be set at creation. Manually-created users can edit their passwords.

During both user creation and edit, you can set an account's screen name, email address. User login and email address will be implicitly recognized by the Issue Assignment feature as SCM accounts if applicable, but you can set additional SCM accounts explicitly. 

### Group
A group is a set of users.

To administer groups, go to **[Administration > Security > Groups](/#sonarqube-admin#/admin/groups)**.

To edit the membership of a group, click the icon next to the membership total.

Two groups have a special meaning:

* **Anyone** is a group that exists in the system, but that cannot be managed. Every user belongs to this group, including Anonymous user.
* **sonar-users** is the default group to which users are automatically added.

### Global Permissions
To set global permissions, log in as a System administrator and go to **[Administration > Security > Global Permissions](/#sonarqube-admin#/admin/permissions)**. 

* **Administer System**: All administration functions for the instance: global configuration.
* **Administer Quality Profiles**: Any action on quality profiles.
* **Administer Quality Gates**: Any action on quality gates
* **Execute Analysis**: Execute analyses (project, view, report, developer), and to get all settings required to perform the analysis, even the secured ones like the scm account password, and so on.
* **Create Projects**: Initialize the structure of a new project before its first analysis. This permission is also required when doing the very first analysis of a project that has not already been created via the GUI. * **
* **Create Applications**: Create a new Application. * **
* **Create Portfolios**: Create a new Portfolio. * **

\* Users with any explicit create permission will see a "+" item in the top menu giving access to these functions. If these permissions are removed from global administrators, they will loose quick access to them via the "+" menu, **but retain access to creation** via the **Administration** menu.

** Creating an item does not automatically grant rights to administer it. For that, see _Creators permission_ below.

### Project Permissions
Project permissions are available from the project-level Administration menu: **Administration > Permissions**.

Project visibility may be toggled between public or private. Making a project private hides its source code and measures from the `Anyone` group. For both public and private projects, four different permissions can be set:

* **Administer Issues**: Change the type and severity of issues, resolve issues as being "Won't Fix" or "False Positive" (users also need "Browse" permission).
* **Administer Security Hotspots**: "Detect" (convert) a Vulnerability from a "Security Hotspot", reject, clear, accept or reopen a "Security Hotspot" (users also need "Browse" permission).
* **Administer**: Access project settings and perform administration tasks (users also need "Browse" permission).
* **Execute Analysis**: Execute analyses (project, view, report, developer), and to get all settings required to perform the analysis, even the secured ones like the scm account password, the jira account password, and so on.

Private projects have two additional permissions:
* **Browse**: Access a project, browse its measures, issues and perform some issue edits (confirm/resolve/reopen, assignment, comment).
* **See Source Code**: View the project's source code.

Note that permissions _are not_ cumulative. For instance, if you want to be able to administer the project, you also have to be granted the Browse permission to be able to access the project (which is the default for Public project).

You can either manually grant permissions for each project to some users and groups or apply permission templates to projects. 

## Permission Templates for Default Permissions
SonarQube ships with a default permissions template, which automatically grants specific permissions to certain groups when a project, portfolio, or application is created. It is possible to edit this template, and to create additional templates. A separate template can be set for each type of resource. Further, for projects you can have a template apply only to a subset of new projects using a project key regular expression (the template's **Project Key Pattern**). By default, every new project with a key that matches the supplied pattern will have template's permissions applied.

Templates are empty immediately after creation. Clicking on the template name will take you to its permission editing interface.

Templates are administered through **[Administration > Security > Permission Templates](/#sonarqube-admin#/admin/permission_templates)**.

### Creators permissions
**Creators** is a special group that appears only in the permission template editing interface. Any permissions assigned to this group will at the time of project/portfolio/application creation be granted to the single user account used to create the project. This allows SonarQube administrators to let users autonomously create and administer their own projects.

While templates can be applied after project creation, applying a template that includes "Creators" permissions to an existing project/portfolio/application will not grant the relevant permissions to the project's original creator because that association is not stored.

### Reset project permissions to a template
To apply permission templates to projects go to **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management)**. You can  either apply a template to a specific project using the project-specific **Actions > Apply Permission Template** option or use the Bulk Apply Permission Template to apply a template to all selected projects.

Note that there is no relation between a project and a permission template, meaning that:
* the permissions of a project can be modified after a permission template has been applied to this project
* none of the project permissions is changed when a permission template is modified

## Settings Encryption
Encryption is mostly used to remove clear passwords from settings (database or SCM credentials for instance). The implemented solution is based on a symetric key algorithm. The key point is that the secret key is stored in a secured file on disk. This file must owned by and readable only by the system account that runs the SonarQube server.

The algorithm is AES 128 bits. Note that 256 bits cipher is not used because it's not supported by default on all Java Virtual Machines ([see this article](https://confluence.terena.org/display/~visser/No+256+bit+ciphers+for+Java+apps)).

1. **Generate the secret key**  
A unique secret key must be shared between all parts of the SonarQube infrastructure (server and analyzers). To generate it, go to **[Administration > Configuration > Encryption](/#sonarqube-admin#/admin/settings/encryption)** and click on Generate Secret Key.
1. **Store the secret key on the SonarQube server**  
   * Copy the generated secred key to a file on the machine hosting the SonarQube server. The default location is _~/.sonar/sonar-secret.txt_. If you want to store it somewhere else, set its path through the `sonar.secretKeyPath` property in _$SONARQUBE-HOME/conf/sonar.properties_
   * Restrict file permissions to the account running the SonarQube server (ownership and read-access only).
   * Restart your SonarQube server
1. **Generate the encrypted values of your settings**  
Go back to **[Administration > Configuration > Encryption](/#sonarqube-admin#/admin/settings/encryption)** and use the form that has been added to the interface to generated encrypted versions of your values.
![Encrypt values through the admin interface](/images/encrypt-value.png)
1. **Use the encrypted values in your SonarQube server configuration**  
Simply copy these encrypted values into _$SONARQUBE-HOME/conf/sonar.properties_
```
sonar.jdbc.password={aes}CCGCFg4Xpm6r+PiJb1Swfg==  # Encrypted DB password
...
sonar.secretKeyPath=C:/path/to/my/secure/location/my_secret_key.txt
```

