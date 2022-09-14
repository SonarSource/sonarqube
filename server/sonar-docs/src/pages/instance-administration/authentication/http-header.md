---
title: HTTP Header
url: /instance-administration/authentication/http-header/
---

You can delegate user authentication to third-party systems (proxies/servers) using HTTP Header Authentication. See `SSO AUTHENTICATION` section within `sonar.properties` file.

When this feature is activated, SonarQube expects that the authentication is handled prior any query reaching the server. 
The tool that handles the authentication should:

* intercept calls to the SonarQube server
* take care of the authentication
* update the HTTP request header with the relevant SonarQube user information
* re-route the request to SonarQube with the appropriate header information

![HTTP Header Authentication flow](/images/http-header-authentication.png)

All the parameters required to activate and configure this feature are available in SonarQube server configuration file (in _$SONARQUBE_HOME/conf/sonar.properties_).

Using Http header authentication is an easy way integrate your SonarQube deployment with an in-house SSO implementation.