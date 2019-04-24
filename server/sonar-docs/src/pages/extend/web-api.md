---
title: Web API Authentication
url: /extend/web-api/
---
SonarQube provides web API to access its functionalities from applications. The web services composing the web API are documented within SonarQube, through the URL [/web_api](/#sonarqube#/web_api), which can also be reached from a link in the page footer. 

Administrative web services are secured and require the user to have specific permissions. In order to be authenticated, the user must provide credentials as described below.

## User Token

This is the recommended way. Benefits are described in the page [User Token](/user-guide/user-token/). The token is sent via the login field of HTTP basic authentication, without any password.
```
# note that the colon after the token is required in curl to set an empty password 
curl -u THIS_IS_MY_TOKEN: https://sonarqube.com/api/user_tokens/search
```

## HTTP Basic Access
Login and password are sent via the standard HTTP Basic fields:
```
curl -u MY_LOGIN:MY_PASSWORD https://sonarqube.com/api/user_tokens/search
```
Users who authenticate in web application through an OAuth provider, for instance GitHub or Bitbucket, don't have credentials and can't use HTTP Basic mode. They must generate and use tokens.
