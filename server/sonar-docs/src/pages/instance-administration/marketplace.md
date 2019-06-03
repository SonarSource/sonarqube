---
title: Marketplace
url: /instance-administration/marketplace/
---

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

## Pending Operations

When you perform an action in the Markplace (install, update or uninstall a plugin), a yellow banner appears at the top of the page showing pending operations that will be executed once SonarQube is restarted. Pending operations can be canceled until the server is restarted.

## Restart SonarQube
Restarting SonarQube can be done manually from the command line by running `sonar.sh restart` or directly from the UI:

* in the Update Center when you have Pending Changes, the restart button will be displayed in the yellow banner (see Pending Operations)
* in the System Info page at any time

## Manual Updates
If your server has no access to the internet, you won't be able to rely on the Marketplace for plugins, and will have to handle plugin installations and upgrades manually.

To see what plugins are available and which version of a plugin is appropriate for your server, use the [plugin version matrix](/instance-administration/plugin-version-matrix/), which is kept up to date with current plugin availability and compatibility.

To install a plugin, simply download it using the manual download link on the plugin documentation page, place it in _$SONARQUBE-HOME/extensions/downloads_, and restart the server.

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
