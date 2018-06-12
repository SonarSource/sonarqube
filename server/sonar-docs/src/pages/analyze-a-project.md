---
title: Analyze a Project
scope: sonarcloud
---

## Prepare your organization

A project must belong to an [organization](/organizations/index). Create one if you intend to collaborate with your team mates, or use your personal organization for test purposes.

** /!\ Important note for private code:** Newly created organizations and personal organizations are under a free plan by default. This means projects analyzed on these organizations are public by default: the code will be browsable by anyone. If you want private projects, you should [upgrade your organization to a paid plan](/sonarcloud-pricing) in the "Administration > Billing" page of your organization.

Find the key of your organization, you will need it at later stages. It can be found on the top right corner of your organization's header.

## Run analysis

SonarCloud currently does not trigger analyses automatically - this feature will come in a near future. Currently, it's up to you to launch them inside your
existing CI scripts.

Depending on which cloud solution you are using for your developments, you can rely on dedicated integrations to help you:

* VSTS: [read our dedicated documentation](/integrations/vsts)
* Bitbucket Cloud: [read our dedicated documentation](/integrations/bitbucketcloud)
* GitHub: [read our dedicated documentation](/integrations/github)

If you are not using those solutions, you will have to find out what command to execute to run the analysis. Our [tutorial](/#sonarcloud#/onboarding) will help you on this.
