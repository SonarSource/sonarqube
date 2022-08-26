---
title: SAML
url: /instance-administration/authentication/saml/
---

You can delegate authentication to a SAML 2.0 Identity Provider using SAML Authentication.

### SAML authentication flow

1. When a user requests a SonarQube web page and is not already authenticated, SonarQube will start a SAML authentication process.
2. SonarQube creates a SAML request for the configured Identity Provider and sends it back to the user.
3. The user's browser automatically relays the SAML request to the Identity Provider.
4. The Identity Provider authenticates the user and creates a SAML assertion containing the user information and privilege. Optionally, it can encrypt this assertion with the SonarQube certificate.
5. The Identity Provider sends a SAML assertion back to the web browser
6. The user's browser then relays the SAML assertion to SonarQube to authenticate and authorize the user.
7. SonarQube responds with the originally requested resource.

![SAML Authentication flow](/images/saml_authentication_flow.png)

During the process, certificates are used to authenticate the Identity Provider and, optionally, SonarQube.
The Identity Provider public certificate is necessary to ensure that the SAML assertion is genuine.
The SonarQube certificate is optional, but ensures that only SonarQube can use the assertion provided.
### Setup

Property| Description                                                                                                                        | Default value | Required                                                                 
---|------------------------------------------------------------------------------------------------------------------------------------|-----------|--------------------------------------------------------------------------
`sonar.auth.saml.enabled`| Is SAML authentication enabled on SonarQube?                                                                                       |           | Yes                                                                      
`sonar.auth.saml.applicationId`| The ID under which SonarQube is known by the Identity Provider.                                                                    | sonarqube | Yes                                                                      
`sonar.auth.saml.providerName`| Name of the Identity Provider displayed in the login page when SAML authentication is active.                                                                                                 | SAML      | Yes                                                                      
`sonar.auth.saml.providerId`| The ID of the Identity Provider.                                                                                                   |           | Yes                                                                      
`sonar.auth.saml.loginUrl`| The Url where the Identity Provider expect to receive SAML requests.                                                               |           | Yes                                                                      
`sonar.auth.saml.certificate.secured`| The public X.509 certificate used by the Identity Provider to authenticate SAML messages.                                          |           | Yes                                                                      
`sonar.auth.saml.user.login`| The name of the attribute where the Identity Provider will put the authenticated user login.                                       |           | Yes                                                                      
`sonar.auth.saml.user.name`| The name of the attribute where the Identity Provider will put the authenticated user name.                                        |           | Yes                                                                      
`sonar.auth.saml.user.email`| The name of the attribute where the Identity Provider will put the authenticated user email.                                       |           | No                                                                       
`sonar.auth.saml.group.name`| The attribute defining the user group in SAML. Users are associated to the default group if this attribute is not defined.         |           | No                                                                       
`sonar.auth.saml.signature.enabled`| Is SonarQube expected to sign the SAML requests? If enabled both the service provider private key and certificate must be provided. |           | No                                                                       
`sonar.auth.saml.sp.privateKey.secured`| The PKCS8 private key without password used by SonarQube to sign SAML messages and to decrypt encrypted SAML responses.            |           | Only if SonarQube requests signature or responses encryption is enabled. 
`sonar.auth.saml.sp.certificate.secured`| The public key part of the previously provided private key.                                                                        |           | Only if SonarQube requests signature is enabled.                         

### Example: Using Keycloak as a SAML Identity Provider
The following example may be useful if you're using Keycloak as a SAML Identity Provider. If you're not using Keycloak, your settings are likely to be different.

[[collapse]]
| ## In the Keycloak server, create a new SAML client
| Create a new client
|
| 1. **Client ID**: Something like "sonarqube", it must not contain whitespace.
| 1. **Client Protocol**: *saml*
| 1. **Client SAML Endpoint**: Can be left empty
|
| Configure the new client
|
| 1. Under *Settings*
|     1. **Client Signature Required:** ON only if the signature of the requests will be active on the SonarQube SAML configuration.
|     1. **Encrypt Assertions**: ON if the responses from the IdP have to be encrypted.
|     1. **Valid Redirect URIs**: "<Your SonarQube URL>/oauth2/callback/saml" (e.g., https://sonarqube.mycompany.com/oauth2/callback/saml).
| 1. Under *Keys*
|     1. (Optional) **Signing Key**: Add the service provider private key and the certificate if the signature of the requests is enabled on the SonarQube side (Keycloak generated keys can be used). This private key will have to be provided in PKCS8 format in SonarQube.
|     1. (Optional) **Encryption Key**: Add the service provider certificate if you want to activate the encryption of Keycloak responses. If request signature is used, you must use the same certificate for the encryption.
| 1. In **Client Scopes > Default Client Scopes**, remove "role_list" from "Assigned Default Client Scopes" (to prevent the error `com.onelogin.saml2.exception.ValidationError: Found an Attribute element with duplicated Name` during authentication)
| 1. Under *Mappers*, create a mapper for each user attribute:
|     1. Create a mapper for the login:
|         1. **Name**: "Login"
|         1. **Mapper Type**: *User Property*
|         1. **Property**: "Username" (note that the login should not contain any special characters other than `.-_@` to meet SonarQube restrictions)
|         1. **SAML Attribute Name**: "login"
|     1. Create a mapper for the name:
|         1. **Name**: "Name"
|         1. **Mapper Type**: *User Property*
|         1. **Property**: "Username" (it can also be another attribute you would previously have specified for the users)
|         1. **SAML Attribute Name**: "name"
|     1. (Optional) Create a mapper for the email:
|         1. **Name**: "Email"
|         1. **Mapper Type**: *User Property*
|         1. **Property**: "Email"
|         1. **SAML Attribute Name**: "email"
|     1. (Optional) Create a mapper for the groups (if you rely on a list of roles defined in "Roles" of the Realm , not in "Roles" of the client):
|         1. **Name**: "Groups"
|         1. **Mapper Type**: *Role list*
|         1. **Role Attribute Name**: "groups"
|         1. **Single Role Attribute**: *ON*
|     1. If you rely on a list of groups defined in "Groups":
|         1. **Name**: "Groups"
|         1. **Mapper Type**: *Group list*
|         1. **Role Attribute Name**: "groups"
|         1. **Single Role Attribute**: *ON*
|         1. **Full Group Path**: *OFF*
| 1. In **Realm Settings > General > Endpoints**, click on "SAML 2.0 Identify Provider Metadata" to obtain the XML configuration file from Keycloak.

[[collapse]]
| ## In SonarQube, Configure SAML authentication
| Go to **[Administration > Configuration > General Settings > Authentication > SAML](/#sonarqube-admin#/admin/settings?category=authentication)**
| * **Enabled**: *true*
| * **Application ID**: The value of the "Client ID" you set in Keycloak (for example "sonarqube")
| * **Provider ID**: The value of the `EntityDescriptor > entityID` attribute in the XML configuration file (e.g., "http://keycloak:8080/auth/realms/sonarqube")
| * **SAML login url**: The value of `SingleSignOnService > Location` attribute in the XML configuration file (e.g., "http://keycloak:8080/auth/realms/sonarqube/protocol/saml")
| * **Identity provider certificate**: The value you get from **Realm Settings > Keys > RS256**; click on the *Certificate* button
| * **SAML user login attribute**: "login" (or whatever you configured above when doing the mapping)
| * **SAML user name attribute**: "name" (or whatever you configured above when doing the mapping)
| * (Optional) **SAML user email attribute**: "email" (or whatever you configured above when doing the mapping)
| * (Optional) **SAML group attribute** "groups" (or whatever you configured above when doing the mapping)
| * **Sign requests**: Set to true to activate the signature of the SAML requests. It needs both the service provider private key and certificate to be set.
| * **Service provider private key**: The service provider private key shared with the identity provider. This key is required for both request signature and response encryption, which can be activated individually. The key should be provided for SonarQube in PKCS8 format without password protection.
| * **Service provider certificate**: The service provider certificate shared with the identity provider in order to activate the requests signature.
|
| You can find [here](https://manpages.ubuntu.com/manpages/focal/man1/pkcs8.1ssl.html) some instructions to convert different key formats.
|
| In the login form, the new button "Log in with SAML" allows users to connect with their SAML account.

### SAML configuration related information and limitations

* **SAML and reverse proxy configuration**: When using SAML, make sure your reverse proxy is properly configured. See [Operating the Server](/setup/operate-server/) for more information.
* **Migrating from LDAP to SAML as Identity Provider**: A guide on how to perform this migration is available [here](https://community.sonarsource.com/t/migrating-sonarqube-users-between-identity-providers-with-a-focus-on-ldap-saml/48653).
* **Identity Provider initiated authentication is not supported**: This is a known limitation of SonarQube only Service Provider authentication is supported. 
* **SAML Single Sign Out is not supported**: Logging off from SonarQube when SAML authentication is enabled, will not result in a disconnection from the other services linked to the same Identity Provider.
