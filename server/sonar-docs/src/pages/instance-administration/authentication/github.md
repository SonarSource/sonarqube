---
title: Github
url: /instance-administration/authentication/github/
---

To allow users to log in with GitHub credentials, you must rely on a GitHub App. You can reuse one that you previously created although we highly recommend to create a dedicated one.

## Creating a dedicated app for authentication
If you want to use a dedicated app for GitHub authentication, you can create a GitHub OAuth app. You'll find general instructions for creating a GitHub OAuth App [here](https://docs.github.com/en/free-pro-team@latest/developers/apps/creating-an-oauth-app). Specify the following settings in your OAuth App:

- **Homepage URL** – the public URL of your SonarQube server. For example, `https://sonarqube.mycompany.com`. For security reasons, HTTP is not supported, and you must use HTTPS. The public URL is configured in SonarQube at **[Administration > General > Server base URL](/#sonarqube-admin#/admin/settings)**.
- **Authorization callback URL** – your instance's base URL. For example, `https://yourinstance.sonarqube.com`.

## Setting your authentication settings in SonarQube

Navigate to **Administration > Configuration > General Settings > Authentication > GitHub Authentication** and update the following:

1. **Enabled** – set the switch to `true`.
1. **Client ID** – the Client ID is found below the GitHub App ID on your GitHub App's page.
1. **Client Secret** – the Client secret is found below the Client ID on your GitHub App's page.
  
Now, from the login page, your users can connect their GitHub accounts with the new "Log in with GitHub" button.
