---
title: Jenkins
url: /analysis/jenkins/
---

_Pull Request analysis is available as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

SonarScanners running in Jenkins can automatically detect branches and Merge or Pull Requests in certain jobs. You don't need to explicitly pass the branch or Pull Request details.

## Analysis Prerequisites

For analysis with Jenkins, you need to have the following Jenkins plugins installed and configured **in Jenkins**: 
- Depending on your ALM: the Bitbucket, GitHub, or GitLab Branch Source plugin. 
- The SonarQube Scanner plugin.

### Bitbucket Server

With Bitbucket Server, you can easily configure and analyze your projects using the tutorial in SonarQube (tutorials for other ALMs are on the way). You'll find the tutorial by selecting **With Jenkins** when analyzing a project you've created from a Bitbucket Server repository. 

Click **Configuring Jenkins Plugins for Bitbucket Server** below to expand the instructions on installing and configuring these plguins.

[[collapse]]
| ## Configuring Jenkins Plugins for Bitbucket Server
|
| ### Bitbucket Branch Source plugin
| [Bitbucket Branch Source plugin](https://plugins.jenkins.io/cloudbees-bitbucket-branch-source/) version 2.7 or later is required
| 
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Manage Plugins** and install the **Bitbucket Branch Source** plugin.
| 1. Back at the Jenkins Dashboard, navigate to **Credentials > System** from the left navigation. 
| 1. Click the **Global credentials (unrestricted)** link in the **System** table. 
| 1. Click **Add credentials** in the left navigation and add the following information:
| 	- **Kind**: Username with password
| 	- **Scope**: Global (Jenkins, nodes, items, all child items, etc)
| 	- **Username**: Your Bitbucket username.
| 	- **Password**: Generate a Personal Access Token in Bitbucket Server with **Read** permissions and copy and paste it here. 
| 1. Click **OK**.
| 1. From the Jenkins Dashboard, navigate to **Manage Jenkins > Configure System**. 
| 1. From the **Bitbucket Endpoints** section, Click the **Add** drop-down menu and select **Bitbucket Server**. Add the following information:
| 	- **Name**: Give a unique name to your Bitbucket Server instance.
| 	- **Server URL**: Your Bitbucket Server instance URL.
| 	- Check **Manage hooks**
| 	- **Credentials**: Select the credentials you created in Step 4.
| 	- **Webhook implementation to use**: Native	
| 1. Click **Save**
|
| ### SonarQube Scanner plugin
| [SonarQube Scanner](https://plugins.jenkins.io/sonar/) version 2.11 or later
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
 
With [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/), you can analyze multiple branches and Pull Requests. The automatic configuration of branches and Pull Requests relies on environment variables available in Multibranch Pipeline jobs. These are set based on information exported by Jenkins plugins. 

For configuration examples, see the [SonarScanner for Jenkins](/analysis/scan/sonarscanner-for-jenkins/) documentation.

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