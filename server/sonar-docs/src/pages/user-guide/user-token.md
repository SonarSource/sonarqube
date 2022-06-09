---
title: Generating and Using Tokens
url: /user-guide/user-token/
---

Users can generate tokens that can be used to run analyses or invoke web services without access to the user's actual credentials.

## Types of Tokens

### User Tokens
These tokens can be used to run analysis and to invoke web services, based on the token author's permissions.

### Project Analysis Tokens
These tokens can be used to run analysis on a specific project. 

In order to create this type of token, the user should have Global Execute Analysis permission or Execute Analysis permission on the token's associated project.

If the token's author loses Execute Analysis permissions for the associated project, the token will no longer be valid for performing an
analysis.

[[info]]
| The usage of Project Analysis Tokens is encouraged for security reasons.
| If such a token were to leak, an attacker would only gain access to analyze a single project or to interact with the related web services requiring Execute Analysis permissions.


### Global Analysis Tokens
These tokens can be used to run analysis on every project.

In order to create this type of tokens, the user should have Global Execute Analysis Permission.

If the token's author loses the Global Execute Analysis permission, the token will no longer be valid for performing an analysis.

## Generating a token

You can generate new tokens at **User > My Account > Security**.

The form at the top of the page allows you to generate new tokens, specifying their token type.

Once you click the **Generate** button, you will see the token value. Copy it immediately; once you dismiss the notification you will not be able to retrieve it.

## Revoking a token

You can revoke an existing token at **User > My Account > Security** by clicking the **Revoke** button next to the token.

## Using a token

User tokens must replace your normal login process in the following scenarios:

* when running analyses on your code: replace your login with the token in the `sonar.login` property. 
* when invoking web services: just pass the token instead of your login while doing the basic authentication.

In both cases, you don't need to provide a password (so when running analyses on your code, the property `sonar.password` is optional). Using a token is the preferred method over using a login and password.
