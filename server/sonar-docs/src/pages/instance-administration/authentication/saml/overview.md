---
title: Overview
url: /instance-administration/authentication/saml/overview/
---

You can delegate authentication to a SAML 2.0 Identity Provider using SAML Authentication.

## SAML authentication flow

1. When a user requests a SonarQube web page and is not already authenticated, SonarQube will start a SAML authentication process.
2. SonarQube creates a SAML request for the configured Identity Provider and sends it back to the user's browser.
3. The user's browser automatically relays the SAML request to the Identity Provider.
4. The Identity Provider authenticates the user and creates a SAML assertion containing the user information and privilege. Optionally, it can encrypt this assertion with the SonarQube certificate.
5. The Identity Provider sends a SAML assertion back to the web browser
6. The user's browser then relays the SAML assertion to SonarQube to authenticate and authorize the user.
7. SonarQube responds with the originally requested resource.

![SAML Authentication flow](/images/saml_authentication_flow.png)

During the process, certificates are used to authenticate the Identity Provider and, optionally, SonarQube.
The Identity Provider public certificate is necessary to ensure that the SAML assertion is genuine.
The SonarQube certificate is optional, but ensures that only SonarQube can use the assertion provided.

## Settings

| Property                                 | UI Name                       | Description                                                                                                                                             | Required                                                                                                                               |
|------------------------------------------|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `sonar.auth.saml.enabled`                | Enabled                       | Controls whether SAML authentication is enabled on SonarQube.                                                                                           | Yes                                                                                                                                    |
| `sonar.auth.saml.applicationId`          | Application ID                | The ID under which SonarQube is known to the Identity Provider.                                                                                         | Yes                                                                                                                                    |
| `sonar.auth.saml.providerName`           | Provider Name                 | The name of the Identity Provider displayed in the login page when SAML authentication is active.                                                       | Yes                                                                                                                                    |
| `sonar.auth.saml.providerId`             | Provider ID                   | The ID of the Identity Provider.                                                                                                                        | Yes                                                                                                                                    |
| `sonar.auth.saml.loginUrl`               | SAML login url                | The URL at which the Identity Provider expects to receive SAML requests.                                                                                | Yes                                                                                                                                    |
| `sonar.auth.saml.certificate.secured`    | Identity provider certificate | The public X.509 certificate used by the Identity Provider to authenticate SAML messages.                                                               | Yes                                                                                                                                    |
| `sonar.auth.saml.user.login`             | SAML user login attribute     | The name of the attribute that the Identity Provider will use to store the authenticated user login.                                                    | Yes                                                                                                                                    |
| `sonar.auth.saml.user.name`              | SAML user name attribute      | The name of the attribute that the Identity Provider will use to store the authenticated user name.                                                     | Yes                                                                                                                                    |
| `sonar.auth.saml.user.email`             | SAML user email attribute     | The name of the attribute that the Identity Provider will use to store the authenticated user email.                                                    | No                                                                                                                                     |
| `sonar.auth.saml.group.name`             | SAML group attribute          | The attribute defining the user group in SAML. If this attribute is not defined, users are associated with the default group.                           | No                                                                                                                                     |
| `sonar.auth.saml.signature.enabled`      | Sign requests                 | Controls whether SonarQube is expected to sign the SAML requests. If enabled, both the service provider's private key and certificate must be provided. | No                                                                                                                                     |
| `sonar.auth.saml.sp.privateKey.secured`  | Service provider private key  | The PKCS8 private key without password used by SonarQube to sign SAML requests and to decrypt encrypted SAML responses.                                 | This is only required if `sonar.auth.saml.signature.enabled` is set to `true` or the Identity Provider sends encrypted SAML responses. |
| `sonar.auth.saml.sp.certificate.secured` | Service provider certificate  | The public key part of the previously provided private key.                                                                                             | This is only required if `sonar.auth.saml.signature.enabled` is set to `true`.                                                         |

### Testing
After all the mandatory settings are filled, the SAML integration with the Identity Provider can be tested by clicking the **Test configuration** button.
A new tab will open with more information regarding the success of the integration, attributes received from the Identity Provider, and any warnings or errors that occur.

## SAML configuration related information and limitations

* **SAML and reverse proxy configuration**: When using SAML, make sure your reverse proxy is properly configured. See [Operating the Server](/setup/operate-server/) for more information.
* **Migrating from LDAP to SAML as Identity Provider**: A guide on how to perform this migration is available [here](https://community.sonarsource.com/t/migrating-sonarqube-users-between-identity-providers-with-a-focus-on-ldap-saml/48653).
* **Identity Provider initiated authentication is not supported**: This is a known limitation of SonarQube when using SAML as the authentication mechanism. Only Service Provider initiated authentication is fully supported.
* **SAML Single Sign Out is not supported**: Logging off from SonarQube when SAML authentication is enabled, will not result in a disconnection from the other services linked to the same Identity Provider.
