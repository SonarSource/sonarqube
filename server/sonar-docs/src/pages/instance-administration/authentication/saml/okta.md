---
title: How to setup Okta
url: /instance-administration/authentication/saml/okta/
---

The following example may be useful if you are using Okta as a SAML Identity Provider.
Note that Okta does not support service provider signed requests even if they are enabled on the SonarQube side.


## Create a new application in Okta admin dashboard

1. Under  **Applications**, choose **Create App Integration**.

   ![Create new application](/images/okta/okta-create-application.png)

2. Choose **SAML 2.0** in the **Sign-in Method** dialog.

3. Under **General Settings**, fill in **App name** with *SonarQube* (or another name that you prefer) and opt-in to **Do not display application icon to users**.

   ![General settings](/images/okta/okta-general-settings.png)


### Configure SAML settings

Under *General Settings*, configure the following fields:

- **Single sign on URL**: `<Your SonarQube URL>/oauth2/callback/saml` (e.g., `https://sonarqube.mycompany.com/oauth2/callback/saml`).

- **Audience URI (SP Entity ID)**: Something like `sonarqube` (SonarQube default value). It must not contain whitespace. 

![SAML settings](/images/okta/okta-saml-settings.png)

(Optional) If you want to enable assertion encryption, expand *Show Advanced Settings* and configure the following fields:

- **Assertion Encryption**: Choose *Encrypted*.

- **Encryption Algorithm**: Choose *AES256-GCM* for high security.

- **Key Transport Algorithm**: Choose *RSA-OAEP*.

- **Encryption Certificate**: Add the service provider certificate. It should be the same certificate as that found in the SonarQube SAML settings under "Service provider certificate".

![Encryption attributes](/images/okta/okta-encryption-attributes.png)

Under **Attribute Statements**, add the following attribute mappings:

- Create a mapping for the *name*:

  1. **Name**: `name`.

  2. **Name format**: *Unspecified*.

  3. **Value**: Choose `user.firstName`.

- Create a mapping for the *login*:

  1. **Name**: `login`.

  2. **Name format**: *Unspecified*.

  3. **Value**: Choose `user.login`.

- (Optional) Create a mapping for the *email*:

  1. **Name**: `email`.

  2. **Name format**: *Unspecified*.

  3. **Value**: Choose `user.email`.

  ![Attributes](/images/okta/okta-attributes.png)

- (Optional) Under *Group Attribute Statements* (See details in [Group Mapping](/instance-administration/authentication/overview/)):

  1. **Name**: `groups`.

  2. **Name format**: *Unspecified*.

  3. **Filter**: Choose *Matches regex* and set the value to `.*`.

  ![Group attribute](/images/okta/okta-group-attribute.png)

Click **Finish** in the **Feedback** dialog to confirm the creation of the application.

You can now add users and groups in the *Assignments* tab of the application.

![Assign users](/images/okta/okta-assign-users.png)


After the application creation, navigate to the **Sign On** tab of the *SonarQube* application in Okta.

![Signon tab](/images/okta/okta-signon.png)

Next to the **SAML Signing Certificates** subsection, you will find the configurations needed for setting up SonarQube, under **View SAML setup instructions**.

![Setup instructions](/images/okta/okta-setup-instructions.png)



## In SonarQube, Configure SAML authentication

Go to **[Administration > Configuration > General Settings > Authentication > SAML](/#sonarqube-admin#/admin/settings?category=authentication)**

- **Enabled**: *true*.

- **Application ID**: The value of the *Audience URI (SP Entity ID)* you set in Okta (for example, `sonarqube`).

- **Provider ID**: The value of *Identity Provider Issuer* provided in **View SAML setup instructions** from Okta.

- **SAML login url**: The value of *Identity Provider Single Sign-On URL* provided in **View SAML setup instructions** from Okta.

- **Identity provider certificate**: The value of *X.509 Certificate* provided in **View SAML setup instructions** from Okta.

- **SAML user login attribute**: `login` (or whatever you configured above when doing the mapping).

- **SAML user name attribute**: `name` (or whatever you configured above when doing the mapping).

- (Optional) **SAML user email attribute**: `email` (or whatever you configured above when doing the mapping).

- (Optional) **SAML group attribute** `groups` (or whatever you configured above when doing the mapping).

- **Sign requests**: Not supported for Okta.

- (Optional) **Service provider private key**: The private key is required for assertion encryption support. It must be provided for SonarQube in `PKCS8` format without encryption. You can find instructions for converting to different key formats [here](https://manpages.ubuntu.com/manpages/focal/man1/pkcs8.1ssl.html).

- (Optional) **Service provider certificate**: The certificate is required for assertion encryption support. It must be shared with Okta in order to activate the assertion encryption.

The service provider private key and certificate can be either a new self-signed pair or any existing pair available in your infrastructure.

In the login form, the new button **Log in with SAML** (or a custom name specified in the `sonar.auth.saml.providerName` setting) allows users to connect with their SAML account.
