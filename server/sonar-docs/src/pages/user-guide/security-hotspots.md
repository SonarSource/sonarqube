---
title: Security Hotspots
url: /user-guide/security-hotspots/
---

## What is a Security Hotspot?

Unlike Vulnerabilities, Security Hotspots aren't necessarily issues that are open to attack. Instead, Security Hotspots highlight security-sensitive pieces of code that need to be manually reviewed. Upon review, you'll either find a Vulnerability that needs to be fixed or that there is no threat. 

## Why are Security Hotspots Important?

Security Hotspots help focus the efforts of developers who are manually checking security-sensitive code. Reviewing Security Hotspots allows you to:

* **Fix security issues** – Reviewing Security Hotspots gives you the opportunity to detect vulnerabilities and ensure issues are fixed before merging pull requests or releasing your branch.
* **Learn about security** – {instance} explains why your code was identified as a Security Hotspot and the link between your Security Hotspots and well-known attacks or weaknesses such as SQL Injection, Weak Cryptography, or Authentication. This helps you to know when you're working on security-sensitive code and to avoid creating Vulnerabilities.

## Security Hotspot Lifecycle
Security Hotspots have a dedicated lifecycle and must be reviewed by someone with the "Administer Security Hotspots" permission. 

### Status

Through the lifecycle, a Security Hotspot takes one of the following statuses:

* **To Review** – the default status of new Security Hotspots set by {instance}. A Security Hotspot has been reported and needs to be checked.
* **In Review** – the Security Hotspot is being checked to make sure there isn't a vulnerability in the code.
* **Reviewed** – the Security Hotspot has been checked and no security issue was found.

A Security Hotspot is only closed if the code containing it is deleted or the rule is deactivated.

### Actions

You can perform the following actions on Security Hotspots:

* **Resolve as Reviewed** - There is no vulnerability in the code.
* **Set as In Review** - A review is in progress to check for a Vulnerability.
* **Reset as To Review** - The Security Hotspot needs to be analyzed again.
* **Open as Vulnerability** - There's a Vulnerability in the code that must be fixed.

### Workflow

When {instance} detects a Security Hotspot, it is set as **To Review**. From here, you can perform the following actions on the Security Hotspot:
* **Open as a Vulnerablility** if you find a Vulnerability in the code at the Security Hotspot location that must be fixed.
* **Resolve as Reviewed** if you don't find a Vulnerability in the code at the Security Hotspot location.
* **Set as In Review** if you want to flag the Security Hotspot to show that you are checking it or about to check it for Vulnerabilities. This is an optional step in the workflow. 

If you set a Security Hotspot to **In Review**, it can either be marked:
* **Open as Vulnerability** if you find a Vulnerability in the code that must be fixed.
* **Resolve as Reviewed** if you don't find a Vulnerability in the code.

When you determine there is a Vulnerability at a Security Hotspot location and select **Open as a Vulnerability**, its status changes from **To Review** or **In Review** to **Open**. This converts the Security Hotspot to a Vulnerability, and the developer who last touched the line of code will receive "new issue" notifications (if she's signed up to get them).

Once a Vulnerability is **Open**ed at a Security Hotspot location, the following occurs:

1. The Security Hotspot is assigned to the appropriate developer, and the developer makes a fix.
2. The developer then marks the Vulnerability **Resolve as Reviewed** *via the UI* which moves the Vulnerability back to being a Security Hotspot. 
3. The Security Hotspot is then marked as **Reviewed** and it's status is **Fixed**. 

A reviewed Security Hotspot can be reopened as a Vulnerability at any point if it's determined to be a true issue.
