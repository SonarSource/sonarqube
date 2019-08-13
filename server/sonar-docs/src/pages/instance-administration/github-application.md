---
title: GitHub Enterprise Integration
url: /instance-administration/github-application/
---
_GitHub Enterprise Integration is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

You can add Pull Request decoration to Checks in GitHub Enterprise by creating a GitHub Application, configuring your SonarQube instance, and installing the app in your organizations.

## Creating a GitHub Application

Click [here](https://developer.github.com/apps/building-github-apps/creating-a-github-app/) for general instructions on creating a GitHub App.

From the "Register new GitHub App" page, follow these steps to create your GitHub App for PR decoration:

1. Under **GitHub App name**, give your app a name (such as SonarQubePRChecks).
2. GitHub requires a **Homepage URL** and a **Webhook URL**. These values aren't important for Pull Request decoration, so you can use any URL (such as https://www.sonarqube.org/).
3. Grant access for the following **Permissions**

	| Permission          | Access       |
	|---------------------|--------------|
	| Checks              | Read & write |
	| Repository metadata | Read-only    |
	| Pull Requests       | Read-only    |
	| Commit statuses     | Read-only    |

4. Under "Where can this GitHub App be installed?," select **Any account**.
5. Click **Create GitHub App**.

## Generating and Setting Your Private Key

After creating your app, you'll be prompted with a link to "generate a private key" at the top of the page. Click the link to download your `.pem` private key file. You can also download your `.pem` file by scrolling down to "Private Key" and clicking **Generate Private Key**.

After downloading your `.pem` file, you'll need to set your private key in SonarQube global settings by copying and pasting the content of your private key file into the text box at [**Administration > Pull Requests > GitHub > GitHub App private key**](/#sonarqube-admin#/admin/settings?category=pull_request/).

## Configuring Your SonarQube Instance

You'll need to configure the following global settings in SonarQube.

### Setting Your GitHub Enterprise Instance API URL

Enter your GitHub Enterprise Instance API URL in the text box at [**Administration > Pull Requests > GitHub > GitHub API URL**](/#sonarqube-admin#/admin/settings?category=pull_request/).
Your GitHub Enterprise instance API URL is formatted as follows: `https://<your-github-enterprise-address>/api/v3`

### Setting Your App Name and ID
 
1. Enter your app name in the text box at [**Administration > Pull Requests > GitHub > GitHub App name**](/#sonarqube-admin#/admin/settings?category=pull_request/).
2. Enter your app ID in the text box at [**Administration > Pull Requests > GitHub > GitHub App ID**](/#sonarqube-admin#/admin/settings?category=pull_request/). You can find your GitHub App ID on the app's settings page at `https://<your-github-enterprise>/settings/apps/<appname>`.

## Installing Your App

To install your app in your organizations:

1. Go to your GitHub App URL.
	* GitHub App URLs are specific to your GitHub Enterprise Address and your app name and are formatted as follows: `https://<your-github-enterprise-address>/github-apps/<YourAppName>`.
	* For example, if your GitHub Enterprise address is `github-enterprise-1.yoursite.com` and your app name is `SonarQubePRChecks`, your GitHub App URL will be `https://github-enterprise-1.yoursite.com/github-apps/SonarQubePRChecks`.
2. From your GitHub App page, click the **Install** or **Configure** button.
3. Choose the organization where you want to install your app from the list.
4. Click the **Install** button.
