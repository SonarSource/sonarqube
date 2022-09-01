---
title: How to setup Keycloak
url: /instance-administration/authentication/saml/keycloak/
---

The following content may be useful if you're using Keycloak as a SAML Identity Provider.

## Keycloak server configuration
Create a new SAML client

1. **Client ID**: Something like "sonarqube", it must not contain whitespace.
1. **Client Protocol**: *saml*
1. **Client SAML Endpoint**: Can be left empty

Configure the new SAML client

1. Under *Settings*
    1. **Client Signature Required:** ON only if the signature of the requests will be active on the SonarQube SAML configuration.
    1. **Encrypt Assertions**: ON if the responses from the IdP have to be encrypted.
    1. **Valid Redirect URIs**: "<Your SonarQube URL>/oauth2/callback/saml" (e.g., https://sonarqube.mycompany.com/oauth2/callback/saml).
1. Under *Keys*
    1. (Optional) **Signing Key**: Add the service provider private key and the certificate if the signature of the requests is enabled on the SonarQube side (Keycloak generated keys can be used). This private key will have to be provided in PKCS8 format in SonarQube.
    1. (Optional) **Encryption Key**: Add the service provider certificate if you want to activate the encryption of Keycloak responses. If request signature is used, you must use the same certificate for the encryption.
1. In **Client Scopes > Default Client Scopes**, remove "role_list" from "Assigned Default Client Scopes" (to prevent the error `com.onelogin.saml2.exception.ValidationError: Found an Attribute element with duplicated Name` during authentication)
1. Under *Mappers*, create a mapper for each user attribute:
    1. Create a mapper for the login:
        1. **Name**: "Login"
        1. **Mapper Type**: *User Property*
        1. **Property**: "Username" (note that the login should not contain any special characters other than `.-_@` to meet SonarQube restrictions)
        1. **SAML Attribute Name**: "login"
    1. Create a mapper for the name:
        1. **Name**: "Name"
        1. **Mapper Type**: *User Property*
        1. **Property**: "Username" (it can also be another attribute you would previously have specified for the users)
        1. **SAML Attribute Name**: "name"
    1. (Optional) Create a mapper for the email:
        1. **Name**: "Email"
        1. **Mapper Type**: *User Property*
        1. **Property**: "Email"
        1. **SAML Attribute Name**: "email"
    1. (Optional) Create a mapper for the groups (if you rely on a list of roles defined in "Roles" of the Realm , not in "Roles" of the client):
        1. **Name**: "Groups"
        1. **Mapper Type**: *Role list*
        1. **Role Attribute Name**: "groups"
        1. **Single Role Attribute**: *ON*
    1. If you rely on a list of groups defined in "Groups":
        1. **Name**: "Groups"
        1. **Mapper Type**: *Group list*
        1. **Role Attribute Name**: "groups"
        1. **Single Role Attribute**: *ON*
        1. **Full Group Path**: *OFF*
1. In **Realm Settings > General > Endpoints**, click on "SAML 2.0 Identify Provider Metadata" to obtain the XML configuration file from Keycloak.

## SonarQube configuration
Configure the SAML authentication: **[Administration > Configuration > General Settings > Authentication > SAML](/#sonarqube-admin#/admin/settings?category=authentication)**

* **Enabled**: *true*
* **Application ID**: The value of the "Client ID" you set in Keycloak (for example "sonarqube")
* **Provider ID**: The value of the `EntityDescriptor > entityID` attribute in the XML configuration file (e.g., "http://keycloak:8080/auth/realms/sonarqube")
* **SAML login url**: The value of `SingleSignOnService > Location` attribute in the XML configuration file (e.g., "http://keycloak:8080/auth/realms/sonarqube/protocol/saml")
* **Identity provider certificate**: The value you get from **Realm Settings > Keys > RS256**; click on the *Certificate* button
* **SAML user login attribute**: "login" (or whatever you configured above when doing the mapping)
* **SAML user name attribute**: "name" (or whatever you configured above when doing the mapping)
* (Optional) **SAML user email attribute**: "email" (or whatever you configured above when doing the mapping)
* (Optional) **SAML group attribute** "groups" (or whatever you configured above when doing the mapping)
* **Sign requests**: Set to true to activate the signature of the SAML requests. It needs both the service provider private key and certificate to be set.
* **Service provider private key**: The service provider private key shared with the identity provider. This key is required for both request signature and response encryption, which can be activated individually. The key should be provided for SonarQube in PKCS8 format without password protection.
* **Service provider certificate**: The service provider certificate shared with the identity provider in order to activate the requests signature.

You can find [here](https://manpages.ubuntu.com/manpages/focal/man1/pkcs8.1ssl.html) some instructions to convert different key formats.

In the login form, the new button "Log in with SAML" allows users to connect with their SAML account.
