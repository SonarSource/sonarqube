---
title: Security Hotspots
url: /user-guide/security-hotspots/
---

## What is a Security Hotspot?
Security Hotspots highlight security-sensitive pieces of code that need to be manually reviewed. Upon review, you'll either find that there is no threat or that there is vulnerable code that needs to be fixed. 

## Why are Security Hotspots Important?
Security Hotspots help you focus your efforts as you manually checking security-sensitive code. Reviewing Security Hotspots allows you to:

* **Detect security issues** – You can detect vulnerable code and ensure issues are fixed before merging pull requests or releasing your branch.
* **Learn about security** – You can see why your code was identified as a Security Hotspot and the link between your Security Hotspots and well-known attacks or weaknesses such as SQL Injection, Weak Cryptography, or Authentication. This helps you to know when you're working on security-sensitive code and to avoid creating vulnerable code.
* **Learn how to fix vulnerable code** – From the Security Hotspots page, you can see why the Hotspot was raised, determine if you've used code in a security-sensitive way, and learn how to fix it if you have.

## Lifecycle
Security Hotspots have a dedicated lifecycle. To make status changes, the user needs the **Administer Security Hotspots** permission. This permission is enabled by default. Users with the **Browse** permission can comment on or change the user assigned to a Security Hotspot.

### Statuses  
Through the lifecycle, a Security Hotspot takes one of the following statuses:

* **To Review** – the default status of new Security Hotspots set by SonarQube. A Security Hotspot has been reported and needs to be checked.
* **Reviewed** – a developer has manually assessed the Security Hotspot and either considered the code not vulnerable or any vulnerability issue that was found has been fixed.

A Security Hotspot is only closed if the code containing it is deleted or the rule is deactivated.

## Workflow  
Follow this workflow to review Security Hotspots and fix any vulnerable code that you find.

### Review Priority
When SonarQube detects a Security Hotspot, it's added to the list of Security Hotspots according to its review priority from High to Low. Hotspots with a High Review Priority are the most likely to contain vulnerable code and need your attention first. 

Review Priority is determined by the security category of each security rule. Rules in categories that are ranked high on the OWASP Top 10 and CWE Top 25 standards are considered to have a High Review Priority. Rules in categories that aren't ranked high or aren't mentioned on the OWASP Top 10 or CWE Top 25 standards are rated as Medium or Low.

### Reviewing Hotspots  
When reviewing a Hotspot, you should:

1. Review the **What's the risk** tab to understand why the Security Hotspot was raised.
1. From the **Are you vulnerable** tab, read the **Ask Yourself Whether** section to determine if the code is used in a security-sensitive way.
1. From the **How can you fix it** tab, follow the **Recommended Secure Coding Practices** to fix your code if you've determined it's unsafe.

After following these steps, set the Security Hotspot to one of the following:

* **Fixed** – if you found vulnerable code and have modified it to follow secure coding practices.
* **Safe** – if the code already follows secure coding practices and doesn't need to be modified.
* **Needs additional review** – if you need another user's review to make sure the Security Hotspot doesn't contain vulnerable code.

### Review History

The **Review history** tab shows the history of the Security Hotspot including the status it's been assigned and any comments the reviewer had regarding the Hotspot.  