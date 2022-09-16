---
title: Marketplace
url: /instance-administration/marketplace/
---

[[info]]
| You can only install and update plugins from the Marketplace in SonarQube Community Edition. With commercial editions, you need manually install and update your plugins. See [Install a Plugin](/setup/install-plugin/) for more information.

Administrators can access the Marketplace via **[Administration > Marketplace](/#sonarqube-admin#/admin/marketplace)**. The Marketplace is the place for keeping the pieces of the SonarQube platform up to date. It lets you:

See

* The currently installed SonarQube Edition
* Which plugins are installed
* Whether plugin updates are available
* Which other plugins are compatible with your version of SonarQube

Discover

* Which other Editions are available, to enable more features

Install

* New plugins
* Plugin updates

To view/install plugin updates, your SonarQube server needs internet access. Installations require the platform to be restarted before they take effect.

[[info]]
| sonarplugins.com is not endorsed by, affiliated with, maintained, authorized, or sponsored by Sonar.

## Pending Operations

When you perform an action in the Marketplace (install, update, or uninstall a plugin), a yellow banner appears at the top of the page showing pending operations that will be executed once SonarQube is restarted. Pending operations can be canceled until the server is restarted.

## Restart SonarQube
Restarting SonarQube can be done manually from the command line by running `sonar.sh restart`.
In SonarQube Community Edition, you can also restart from the UI, in the Update Center. When you have Pending Changes, the restart button will be displayed in the yellow banner (see Pending Operations). Please note that restarting the server won't reload the changes applied to the **sonar.properties**.

## Manual Updates
If you're using a commercial edition or your server doesn't have internet access, you won't be able to rely on the Marketplace for plugins, and you will have to handle plugin installations and upgrades manually.

To see what plugins are available and which version of a plugin is appropriate for your server, use the [plugin version matrix](/instance-administration/plugin-version-matrix/), which is kept up to date with current plugin availability and compatibility.

To install a plugin, simply download it using the manual download link on the plugin documentation page, place it in `$SONARQUBE-HOME/extensions/plugins`, and restart the server.

### Stopping the Marketplace from searching for plugin updates
Your SonarQube server needs internet access for the Marketplace to search for plugin updates. If your server doesn't have internet access, you may get errors in your logs when the Marketplace tries to search for new plugins. You can stop this by updating `sonar.updatecenter.activate` in `$SONARQUBE-HOME/conf/sonar.properties`.

## Which URLs does the Marketplace connect to?
The SonarQube Marketplace connects to https://update.sonarsource.org/ to get the list of plugins. Most of the referenced plugins are downloaded from:
* https://binaries.sonarsource.com/
* https://github.com/

## Using the Marketplace behind a Proxy
Marketplace uses HTTP(S) connections to external servers to provide these services. If SonarQube is located behind a proxy, additional information must be provided in the _$SONARQUBE-HOME/conf/sonar.properties_ configuration file:
```
http.proxyHost=<your.proxy.host>
http.proxyPort=<yout.proxy.port>

#If proxy authentication is required
http.proxyUser=<your.proxy.user>
http.proxyPassword=<your.proxy.password> 
```
Note: the same properties can be used in the 'https.' form for HTTPS connections.

## Deploying to the Marketplace

If you have developed a SonarQube plugin, you can check out the [requirements](https://community.sonarsource.com/t/deploying-to-the-marketplace/35236) to add it to the marketplace in the [Plugin Development community](https://community.sonarsource.com/c/plugins/15).