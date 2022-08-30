---
title: How to setup Azure AD
url: /instance-administration/authentication/saml/azuread/
---

## Using Azure AD as a SAML Identity Provider
The following content may be useful if you're using Azure AD as a SAML Identity Provider.

To integrate Azure AD (Identity Provider) with SonarQube SAML configuration (Service Provider), both sides need to be configured.

For SonarQube, navigate to **Administration > Authentication > SAML**.
For Azure AD, login to Azure and navigate to Azure AD.

### Set up the SonarQube application in Azure AD
- In Azure AD, navigate to **Enterprise applications** and add a **New Application**.
  ![SAML Azure AD New Application](/images/saml/saml-azure-new.jpg)
- Create your **own application** and fill in the **name**.
  ![SAML Azure AD Create application](/images/saml/saml-azure-create-application.jpg)

### Link SonarQube with Azure AD
- Navigate to **Single sign-on** and select **SAML**.
  ![SAML Azure AD SSO](/images/saml/saml-azure-sso.jpg)
- Edit the **Basic SAML Configuration** and fill in the **Identifier** and the **Reply URL**. The **Identifier** has to be the same as the **Application ID** in SonarQube. The **Reply URL** must have the format `<Your SonarQube URL>/oauth2/callback/saml`.
  ![SAML Azure AD Basic SAML configuration](/images/saml/saml-azure-basic-saml.jpg)
- Fill in the corresponding SonarQube configuration.
  ![SAML Azure AD SonarQube Application ID](/images/saml/saml-azure-sq-appid.png)
- In the Azure AD SAML configuration, navigate to **Set up "application name"** and copy the **Login URL** and **Azure AD Identifier**
  ![SAML Azure AD Links](/images/saml/saml-azure-links.jpg)
- Paste them into the corresponding fields in the SonarQube SAML configuration.
  ![SAML Azure AD SonarQube Links](/images/saml/saml-azure-sq-links.png)

### Attributes and Claims
- In the Azure AD SAML configuration, edit **Attributes & Claims** to view, edit or add attributes.
  ![SAML Azure AD Attributes](/images/saml/saml-azure-attributes.jpg)
  SonarQube uses the following attributes:
  - **Login** (required) A unique name to identify the user in SonarQube. The default Azure AD attribute `emailaddress` is used in the example.
  - **Name** (required) The full name of the user. The default Azure AD attribute `givenname` is used in the example.
  - **Email** (optional) The email of the user.
  - **Group** (optional) Supports mapping to group names in SonarQube. These have to be the same as the group name passed by Azure AD. Otherwise, the default **sonar-users** group is assigned.
    **Note:** The **NameID** attribute is *not* used in SonarQube.
- Corresponding configuration in SonarQube. The full namespace of the attribute should be used.
  ![SAML Azure AD SonarQube Attributes](/images/saml/saml-azure-sq-attributes.png)

### Certificates & Signatures
- Navigate to **SAML Certificates** and download **Certificate (Base64)**.
  ![SAML Azure AD Certificate](/images/saml/saml-azure-certificate.jpg)
- The certificate should be copied into the **Identity provider certificate** field in the SonarQube SAML configuration.
  ![SAML Azure AD SonarQube Certificate](/images/saml/saml-azure-sq-certificate.png)

### Users and Groups
- In the Azure AD SonarQube application, navigate to **Users and groups** and assign users or groups to the application.
  ![SAML Azure AD SonarQube Links](/images/saml/saml-azure-users.jpg)

### Enabling and testing SAML authentication
- In the SonarQube SAML settings, enable SAML.
  ![SAML Azure AD SonarQube SAML](/images/saml/saml-azure-sq-saml.png)
- Logout and try to log in again. If all the mandatory fields are filled in, the Azure SAML integration should appear.
  ![SAML Azure AD SonarQube Login](/images/saml/saml-azure-sq-login.png)
