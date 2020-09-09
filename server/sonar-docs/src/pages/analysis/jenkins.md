---
title: Jenkins
url: /analysis/jenkins/
---

_Pull Request analysis is available starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)._

SonarScanners running in Jenkins can automatically detect branches and Merge or Pull Requests in certain jobs. You don't need to explicitly pass the branch or Pull Request details.

## Analysis Prerequisites

For analysis with Jenkins, you need to have the following Jenkins plugins installed and configured _in Jenkins_: 
- The Branch Source plugin that corresponds to your ALM (Bitbucket, GitHub, or GitLab). 
- The SonarQube Scanner plugin.

### Bitbucket Server and GitHub Tutorial

With Bitbucket Server and GitHub, you can easily configure and analyze your projects by following the tutorial in SonarQube (which you can find by selecting **with Jenkins** when asked how you want to analyze your repository). Before going through the tutorial, you need to set up your Branch Source plugin and SonarQube Scanner plugin. Below you'll find recommended configurations.

#### **Branch Source plugin**

Click your ALM below to expand the instructions on installing and configuring the Branch Source plugin.

[[collapse]]
| ## BitBucket Server
|
| [Bitbucket Branch Source plugin](https://plugins.jenkins.io/cloudbees-bitbucket-branch-source/) version 2.7 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **Bitbucket Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **Bitbucket Endpoints** section, Click the **Add** drop-down menu and select **Bitbucket Server**. Add the following information:
| 	- **Name**: Give a unique name to your Bitbucket Server instance.
| 	- **Server URL**: Your Bitbucket Server instance URL.
| 	- Check **Manage hooks**
| 	- **Credentials**: In your credentials, use a Bitbucket Server personal access token with **Read** permissions.
| 	- **Webhook implementation to use**: Native	
| 1. Click **Save**.

[[collapse]]
| ## GitHub
|
| [GitHub Branch Source plugin](https://plugins.jenkins.io/github-branch-source/) version 2.7.1 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **GitHub Branch Source** plugin.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **GitHub** or **GitHub Enterprise Servers** section, add your GitHub server.
| 1. Click **Save**.

#### **SonarQube Scanner plugin**

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

## Configuring Single Branch Pipeline jobs
With Community Edition, you can only analyze a single branch. For more information, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

## Configuring Multibranch Pipeline jobs 
 
Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can analyze multiple branches and Pull Requests. The automatic configuration of branches and Pull Requests relies on environment variables available in Multibranch Pipeline jobs. These are set based on information exported by Jenkins plugins. 

For configuration examples, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

### Setting your Branch Source Plugin for Pull Request Decoration
You need to configure your Multibranch Pipeline job correctly to avoid issues with Pull Request decoration. From your Multibranch Pipeline job in Jenkins, go to **Configure > Branch Sources > Behaviors > Discover pull requests from origin** and make sure **The current pull request revision** is selected.

## Detecting changed code in Pull Requests
SonarScanners need access to a Pull Request's target branch to detect code changes in the Pull Request. If you're using a Jenkins Pull Request discovery strategy that only fetches the Pull Request and doesn't merge with the target branch, the target branch is not fetched and is not available in the local git clone for the scanner to read. 

In this case, the code highlighted as “new” in the Pull Request may be inaccurate, and you’ll see the following warning in the scanner’s log:

```
File '[name]' was detected as changed but without having changed lines
```

To fix this, either change the discovery strategy or manually fetch the target branch before running the SonarScanner. For example:

```
git fetch +refs/heads/${CHANGE_TARGET}:refs/remotes/origin/${CHANGE_TARGET}
```



