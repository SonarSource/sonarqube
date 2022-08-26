---
title: Bitbucket Cloud
url: /instance-administration/authentication/bitbucket-cloud/
---

To allow users to log in with Bitbucket Cloud credentials, you need to use an [OAuth consumer](https://support.atlassian.com/bitbucket-cloud/docs/use-oauth-on-bitbucket-cloud/) and set the authentication settings in SonarQube. See the following sections for more on setting up authentication.

## Setting your OAuth consumer settings
Create your OAuth consumer in your Bitbucket Cloud workspace settings and specify the following:

- **Name** – the name of your OAuth consumer.
- **Callback URL** – your SonarQube instance URL.
- **Permissions**: 
	* **Account**: **Read** and **Email** access.
	* **Workspace membership**: **Read** access.

## Setting your authentication settings in SonarQube
To set your global authentication settings, navigate to **Administration > Configuration > General Settings > Authentication > Bitbucket Cloud Authentication** and update the following settings:

- **Enabled** - set to true.
- **OAuth consumer key** - enter the **Key** from your OAuth consumer page in Bitbucket.
- **OAuth consumer secret** - enter the **Secret** from your OAuth consumer page in Bitbucket.
- **Workspaces** - Only users from Bitbucket Workspaces that you add here will be able to authenticate in SonarQube. This is optional, but _highly_ recommended to ensure only the users you want to log in with Bitbucket credentials are able to. 
