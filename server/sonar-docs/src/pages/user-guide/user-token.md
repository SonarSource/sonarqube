---
title: Generating and Using Tokens
url: /user-guide/user-token/
---

Users can generate tokens that can be used to run analyses or invoke web services without access to the user's actual credentials.

## Generating a token

You can generate new tokens at **User > My Account > Security**.

The form at the bottom of the page allows you to generate new tokens. Once you click the **Generate** button, you will see the token value. Copy it immediately; once you dismiss the notification you will not be able to retrieve it.

## Revoking a token

You can revoke an existing token at **User > My Account > Security** by clicking the **Revoke** button next to the token.

## Using a token

User tokens must replace your normal login process in the following scenarios:

* when running analyses on your code: replace your login with the token in the `sonar.login` property. 
* when invoking web services: just pass the token instead of your login while doing the basic authentication.

In both cases, you don't need to provide a password (so when running analyses on your code, the property `sonar.password` is optional). Using a token is the preferred method over using a login and password.