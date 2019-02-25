---
title: Install a Plugin
url: /setup/install-plugin/
---

<!-- sonarqube -->
There are two options to install a plugin into SonarQube:

- Marketplace - Installs plugins automatically, from the SonarQube UI. 
- Manual Installation - You'll use this method if your SonarQube instance doesn't have access to the Internet.

## Marketplace

If you have access to the Internet and you are connected with a SonarQube user having the Global Permission "Administer System", you can go to Administration > Marketplace.

- Find the plugin you want to install
- Click on Install and wait for the download to be processed

Once download is complete, a "Restart" button will be available to restart your instance.

See [Marketplace](/instance-administration/marketplace/) for more details on how to configure your SonarQube Server to connect to the Internet.

## Manual Installation

In the page dedicated to the plugin you want to install (ex: for Python: SonarPython), click on the "Download" link of the version compatible with your SonarQube version.  

Put the downloaded jar in `$SONARQUBE_HOME/extensions/plugins`, removing any previous versions of the same plugins.

Once done, you will need to restart your SonarQube Server.

### License

If you installed a Commercial Edition, you will need to set the License Key in Administration > Configuration > License Manager before being able to use it.

<!-- /sonarqube -->
