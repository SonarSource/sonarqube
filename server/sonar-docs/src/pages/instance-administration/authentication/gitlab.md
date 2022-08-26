---
title: Gitlab
url: /instance-administration/authentication/gitlab/
---

You can delegate authentication to GitLab using a dedicated GitLab OAuth application.

## Creating a GitLab OAuth app
You can find general instructions for creating a GitLab OAuth app [here](https://docs.gitlab.com/ee/integration/oauth_provider.html).

Specify the following settings in your OAuth app:

- **Name** – your app's name, such as SonarQube.
- **Redirect URI** – enter your SonarQube URL with the path `/oauth2/callback/gitlab`. For example, `https://sonarqube.mycompany.com/oauth2/callback/gitlab`.
- **Scopes** – select **api** if you plan to enable group synchronization. Select **read_user** if you only plan to delegate authentication.

After saving your application, GitLab takes you to the app's page. Here you find your **Application ID** and **Secret**.

## Setting your authentication settings in SonarQube
Open your SonarQube instance, and navigate to **Administration > Configuration > General Settings > Authentication > GitLab Authentication**. Set the following settings to finish setting up GitLab authentication:

- **Enabled** – set to `true`.
- **Application ID** – the Application ID is found on your GitLab app's page.
- **Secret** – the Secret is found on your GitLab app's page.

On the login form, the new "Log in with GitLab" button allows users to connect with their GitLab accounts.

## GitLab group synchronization
Enable **Synchronize user groups** at **Administration > Configuration > General Settings > Authentication > GitLab Authentication** to associate GitLab groups with existing SonarQube groups of the same name. GitLab users inherit membership to subgroups from parent groups. 

To synchronize a GitLab group or subgroup with a SonarQube group, name the SonarQube group with the full path of the GitLab group or subgroup URL. 

For example, with the following GitLab group setup:

- GitLab group = My Group
- GitLab subgroup = My Subgroup
- GitLab subgroup URL = `https://YourGitLabURL.com/my-group/my-subgroup`

You should name your SonarQube group `my-group` to synchronize it with your GitLab group and `my-group/my-subgroup` to synchronize it with your GitLab subgroup.
