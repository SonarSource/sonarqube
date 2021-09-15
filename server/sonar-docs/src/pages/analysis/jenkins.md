---
title: Jenkins
url: /analysis/jenkins/
---

SonarScanners running in Jenkins can automatically detect branches and Merge or Pull Requests in certain jobs. You don't need to explicitly pass the branch or Pull Request details.

## Analysis Prerequisites

To run project analysis with Jenkins, you need to install and configure the following Jenkins plugins _in Jenkins_:
 
- The SonarQube Scanner plugin.
- The Branch Source plugin that corresponds to your DevOps Platform (Bitbucket Server, GitHub, or GitLab) if you're analyzing multibranch pipeline jobs in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above. 

See the **Installing and Configuring your Jenkins plugins** section below for more information.

## Installing and Configuring your Jenkins plugins

### SonarQube Scanner plugin

Click SonarQube Scanner below to expand instructions on installing and configuring the plugin.
 
[[collapse]]
| ## SonarQube Scanner
|
| [SonarQube Scanner plugin](https://plugins.jenkins.io/sonar/) version 2.11 or later is required. 
|
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **SonarQube Scanner** plugin.
| 1. Back at the Jenkins Dashboard, navigate to **Credentials > System** from the left navigation. 
| 1. Click the **Global credentials (unrestricted)** link in the **System** table. 
| 1. Click **Add credentials** in the left navigation and add the following information:
| 	- **Kind**: Secret Text
| 	- **Scope**: Global  
| 	- **Secret**: Generate a token at **User > My Account > Security** in SonarQube, and copy and paste it here.
| 1. Click **OK**.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **SonarQube Servers** section, click **Add SonarQube**. Add the following information:
| 	- **Name**: Give a unique name to your SonarQube instance.
| 	- **Server URL**: Your SonarQube instance URL.
| 	- **Credentials**: Select the credentials created during step 4.
| 1. Click **Save**

### Branch Source plugin
_Required to analyze multibranch pipeline jobs in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) or above_

Click your DevOps Platform below to expand the instructions on installing and configuring the Branch Source plugin.

[[collapse]]
| ## BitBucket Server
|
| [Bitbucket Branch Source plugin](https://plugins.jenkins.io/cloudbees-bitbucket-branch-source/) version 2.7 or later is required
| 
| From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **Bitbucket Branch Source** plugin. Then configure the following:
|
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **Bitbucket Endpoints** section, Click the **Add** drop-down menu and select **Bitbucket Server**. Add the following information:
| 	- **Name**: Give a unique name to your Bitbucket Server instance.
| 	- **Server URL**: Your Bitbucket Server instance URL.
| 1. Click **Save**.

[[collapse]]
| ## BitBucket Cloud
|
| [Bitbucket Branch Source plugin](https://plugins.jenkins.io/cloudbees-bitbucket-branch-source/) version 2.7 or later is required
|
| From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **Bitbucket Branch Source** plugin. 

[[collapse]]
| ## GitHub
|
| [GitHub Branch Source plugin](https://plugins.jenkins.io/github-branch-source/) version 2.7.1 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **GitHub Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **GitHub** or **GitHub Enterprise Servers** section, add your GitHub server.
| 1. Click **Save**.

[[collapse]]
| ## GitLab
|
| [GitLab Branch Source plugin](https://plugins.jenkins.io/gitlab-branch-source/) version 1.5.3 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **GitLab Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **GitLab** section, add your GitLab server. Make sure to check the **Manage Web Hooks** checkbox.
| 1. Click **Save**.

### Configuring Jenkins through the SonarQube tutorial

You can easily configure and analyze your projects with Jenkins through the tutorial in SonarQube. 

[[info]]
| You need to set up SonarQube to import your repositories before accessing the tutorial. See the **DevOps Platform Integrations** in the left-side navigation of this documentation for more information.
|
| A tutorial is currently available for all supported DevOps Platforms except Azure DevOps.

To access the tutorial:

1. Click the **Add project** drop-down in the upper-right corner of the **Projects** page in SonarQube and select your DevOps platform.
1. Select the repository you want to import into SonarQube.
1. When asked **How do you want to analyze your repository?**, select **With Jenkins**.

See the **Installing and Configuring your Jenkins plugins** section below to set up your Jenkins plugins before going through the tutorial. 

## Configuring Single Branch Pipeline jobs
With Community Edition, you can only analyze a single branch. For more information, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

## Configuring Multibranch Pipeline jobs 
 
Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and Pull Requests. The automatic configuration of branches and Pull Requests relies on environment variables available in Multibranch Pipeline jobs. These are set based on information exported by Jenkins plugins. 

For configuration examples, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

### Configuring Multibranch Pipeline jobs for Pull Request Decoration
You need to configure your Multibranch Pipeline job correctly to avoid issues with Pull Request decoration. From your Multibranch Pipeline job in Jenkins, go to **Configure > Branch Sources > Behaviors**.

For Bitbucket and GitHub, under **Discover pull requests from origin**, make sure **The current pull request revision** is selected.

For GitLab, under **Discover merge requests from origin**, make sure **The current merge request revision** is selected.
