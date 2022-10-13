---
title: How to setup Azure AD
url: /instance-administration/authentication/saml/azuread/
---

The following content may be useful if you're using Azure AD as a SAML Identity Provider.

To integrate Azure AD (Identity Provider) with SonarQube (Service Provider), both sides need to be configured.

For SonarQube, navigate to **Administration > Authentication > SAML**.
For Azure AD, login to Azure and navigate to Azure AD.

## Set up the SonarQube application in Azure AD
1. In Azure AD, navigate to **Enterprise applications** and add a **New Application**.
  ![SAML Azure AD New Application](/images/azure/saml-azure-new.jpg)
1. Create your **own application** and fill in the **name**.
  ![SAML Azure AD Create application](/images/azure/saml-azure-create-application.jpg)

## Link SonarQube with Azure AD
1. Navigate to **Single sign-on** and select **SAML**.
  ![SAML Azure AD SSO](/images/azure/saml-azure-sso.jpg)
1. Edit the **Basic SAML Configuration** and fill in the **Identifier** and the **Reply URL**. The **Identifier** has to be the same as the **Application ID** in SonarQube. The **Reply URL** must have the format `<Your SonarQube URL>/oauth2/callback/saml`.
  ![SAML Azure AD Basic SAML configuration](/images/azure/saml-azure-basic-saml.jpg)

   [[info]]
   |The **Reply URL** uses the **Server base URL** provided in SonarQube under **Administration > General**.
1. Make sure that the **Application ID** in SonarQube has the same value as the **Identifier** in the Identity Provider.
  ![SAML Azure AD SonarQube Application ID](/images/azure/saml-azure-sq-appid.png)
1. In the Azure AD SAML configuration, navigate to **Set up <application name>** and copy the **Login URL** and **Azure AD Identifier**
  ![SAML Azure AD Links](/images/azure/saml-azure-links.jpg)
1. Paste the **Login URL** into the **SAML login url** and the **Azure AD Identifier** into the **Provider ID** field in the SonarQube SAML configuration.
  ![SAML Azure AD SonarQube Links](/images/azure/saml-azure-sq-links.png)

## Attributes and Claims
1. In the Azure AD SAML configuration, edit **Attributes & Claims** to view, edit or add attributes.
  ![SAML Azure AD Attributes](/images/azure/saml-azure-attributes.jpg)
  SonarQube uses the following attributes:
   - **Login** (required) A unique name to identify the user in SonarQube. The default Azure AD attribute `emailaddress` is used in the example.
   - **Name** (required) The full name of the user. The default Azure AD attribute `givenname` is used in the example.
   - **Email** (optional) The email of the user.
   - **Group** (optional) Supports mapping to group names in SonarQube. Group name passed by Azure AD and the group name in SonarQube should match. Otherwise, the default **sonar-users** group is assigned.
   [[warning]]
   |The **NameID** attribute is *not* used in SonarQube.
1. Corresponding configuration in SonarQube. The namespace + name of the attribute should be used, as defined in Azure AD.
  ![SAML Azure AD SonarQube Attributes](/images/azure/saml-azure-sq-attributes.png)

## Certificates & Signatures
1. Navigate to **SAML Certificates** and download **Certificate (Base64)**.
  ![SAML Azure AD Certificate](/images/azure/saml-azure-certificate.jpg)
1. The certificate should be copied into the **Identity provider certificate** field in the SonarQube SAML configuration.
  ![SAML Azure AD SonarQube Certificate](/images/azure/saml-azure-sq-certificate.png)
1. (Optional) Encryption for SonarQube requests can be activated by generating an asymmetric key pair. (For more information, see [SAML token encryption in Azure](https://docs.microsoft.com/en-us/azure/active-directory/manage-apps/howto-saml-token-encryption?tabs=azure-portal))
  Add the private key in SonarQube.
  ![SAML Azure AD SonarQube Encryption](/images/azure/saml-azure-sq-encryption.png)
  Import the public key certificate (.cer) file in Azure AD and activate token encryption
  ![SAML Azure AD Encryption](/images/azure/saml-azure-encryption.jpg)
1. (Optional) Azure AD supports signed SAML requests from the Service Provider (under Preview).
  Edit the **Verification certificates**, upload a certificate and enable the **Require verification certificates** option.
  ![SAML Azure AD Encryption](/images/azure/saml-azure-signature.jpg)
  In SonarQube, fill in the corresponding private key and the same certificate and enable the **Sign requests** option.
  ![SAML Azure AD SonarQube certs](/images/azure/saml-azure-sq-certs.png)

## Users and Groups
1. In the Azure AD SonarQube application, navigate to **Users and groups** and assign users or groups to the application.
  ![SAML Azure AD SonarQube Links](/images/azure/saml-azure-users.jpg)
  
## Group mapping
Group mapping between Azure AD and SonarQube can be achieved either by using the Azure AD roles or the Azure AD groups.
For either case, the corresponding group name should exist in SonarQube under **Administration > Security > Groups**. (For more information, see [Authorization](/instance-administration/security/))

- For mapping with the Azure AD groups, a group claim must be added with `sAMAccountName` as a source attribute.
   [[warning]]
   |According to Azure: This source attribute only works for groups synchronized from an on-premises Active Directory using AAD Connect Sync 1.2.70.0 or above
  ![SAML Azure AD SonarQube Links](/images/azure/saml-azure-group-claim.jpg)
  ![SAML Azure AD SonarQube Links](/images/azure/saml-azure-sq-groups.png)
- For mapping with the Azure AD app roles, an application role should be assigned to the user. Azure AD sends the role claim automatically with `http://schemas.microsoft.com/ws/2008/06/identity/claims/role` as a key.
  ![SAML Azure AD SonarQube Links](/images/azure/saml-azure-sq-group-role.png)

## Enabling and testing SAML authentication
1. In the SonarQube SAML settings, enable SAML.
  ![SAML Azure AD SonarQube SAML](/images/azure/saml-azure-sq-saml.png)
1. In the login form, the new button **Log in with SAML** (or a custom name specified in the `sonar.auth.saml.providerName` setting) allows users to connect with their SAML account.
  
  ![SAML Azure AD SonarQube Login](/images/azure/saml-azure-sq-login.png)

Before enabling the SAML authentication on SonarQube, you can verify that the configuration is correct by clicking on the “Test Configuration” button. A SAML login will be initiated and useful information about the SAML response obtained from the Identity provider will be returned. 