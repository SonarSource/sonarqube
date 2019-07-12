---
title: User Account
url: /user-guide/user-account/
---

As a {instance} user you have your own space where you can see the things that are relevant to you:

## Profile

<!-- sonarqube -->

It gives you a summary of:

- your Groups
- your SCM accounts

## Security

If your instance is not using a 3rd party authentication mechanism such as LDAP or an OAuth provider (GitHub, Google Account, ...), you can change your password from here. Additionally, you can also manage your own authentication tokens.

You can create as many Tokens as you want. Once a Token is created, you can use it to perform analysis on a project where you have the [Execute Analysis](/instance-administration/security/) permission.

<!-- /sonarqube -->

<!-- sonarcloud -->

It gives you a summary of your SCM accounts and allows you to delete your account.

## Security

You can create as many Tokens as you want. Once a Token is created, you can use it to perform analysis on a project where you have the [Execute Analysis](/instance-administration/security/) permission.

## Organizations

This is an overview of all the organizations you are member of.

## Delete your user account

Go to [User > My Account > Profile](/#sonarcloud#/account) and click on **Delete**. Once your account is deleted, all you data will be removed except your login that will still be displayed in different places:

- issues assignee
- issues comments
- issues changelog

Note that you can manually unassign yourself from all your issues and/or remove your comments before deleting your account.

The information used to identify yourself in SCM (name, email) are part of the SCM data and can not be removed.

<!-- /sonarcloud -->
