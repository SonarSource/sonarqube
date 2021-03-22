---
title: Install a Plugin
url: /setup/install-plugin/
---

There are two ways to install plugins in SonarQube:

- **Marketplace** - Automatically installs plugins from the SonarQube UI. 
- **Manual Installation** - You can use this method if your SonarQube instance doesn't have Internet access, if you're using Data Center Edition, or if the plugin isn't in the Marketplace.

[[warning]]
| Installing third-party plugins is inherently risky. You must acknowledge this risk before you install or update plugins.

## Marketplace

If your SonarQube instance has access to the Internet, and you're connected with a SonarQube user with the **Administer System** global permission, you can find the Marketplace at **Administration > Marketplace**. From here:

- Find the plugin you want to install
- Click **Install** and wait for the download to be processed

Once the download is complete, a **Restart** button will be available to restart your instance.

See [Marketplace](/instance-administration/marketplace/) for more details on how to configure your SonarQube Server to connect to the Internet.

## Manual Installation

Download the plugin you want to install. The version needs to be compatible with your SonarQube version.  

Put the downloaded jar in `$SONARQUBE_HOME/extensions/plugins`, removing any previous versions of the same plugins.

Once done, you need to restart your SonarQube server.

