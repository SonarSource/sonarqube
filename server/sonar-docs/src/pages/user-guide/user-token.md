---
title: User Token
url: /user-guide/user-token/
---

Each user has the ability to generate tokens that can be used to run analyses or invoke web services without access to the user's actual credentials.

## How to Generate a Token

To generate a token, to go **User > My Account > Security**. Your existing tokens are listed here, each with a Revoke button.

The form at the bottom of the page allows you to generate new tokens. Once you click the **Generate** button, you will see the token value. Copy it immediately; once you dismiss the notification you will not be able to retrieve it.

## How to Use a Token

User tokens have to be used as a replacement of your usual login:

* when running analyses on your code: replace your login by the token in the `sonar.login` property. 
* when invoking web services: just pass the token instead of your login while doing the basic authentication.

In both cases, you don't need to provide a password (so when running analyses on your code, the property `sonar.password` is optional).
