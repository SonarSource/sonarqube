---
title: Security Hotspots
url: /user-guide/security-hotspots/
---

## What is a Security Hotspot?
A Security Hotspot highlights a security-sensitive piece of code that the developer needs to review. Upon review, you'll either find there is no threat or you need to apply a fix to secure the code. 

Another way of looking at hotspots may be [the concept of defense in depth](https://en.wikipedia.org/wiki/Defense_in_depth_(computing)) in which several redundant protection layers are placed in an application so that it becomes more resilient in the event of an attack.

## Vulnerability or Hotspot?
The main difference between a hotspot and a vulnerability is **the need of a review** before deciding whether to apply a fix:

* With a Hotspot, a security-sensitive piece of code is highlighted, but the overall application security may not be impacted. It's up to the developer to review the code to determine whether or not a fix is needed to secure the code.
* With a vulnerability, a problem that impacts the application's security has been discovered that needs to be fixed immediately.

An example is the [RSPEC-2092](https://jira.sonarsource.com/browse/RSPEC-2092) where the use of *cookie secure flag* is recommended to prevent cookies from being sent over non-HTTPS connections but a review is needed because:
* HTTPS is the main protection against MITM attacks and so the secure flag is only an additional protection in case of some failures of network security. 
* The cookie may be designed to be sent everywhere (non-HTTPS websites included) because it's a tracking cookie or similar.

With hotspots we try to give some freedom to users and to educate them on how to choose the most relevant/appropriate protections depending on the context (budget, threats, etc).

## Why are Security Hotspots Important?
While the need to fix individual hotspots depends on the context, you should view Security Hotspots as an essential part of improving an application's robustness. The more fixed hotspots there are, the more secure your code is in the event of an attack. Reviewing Security Hotspots allows you to:

* **Understand the risk** – Understanding when and why you need to apply a fix in order to reduce an information security risk (threats and impacts).
* **Identify protections** – While reviewing Hotspots, you'll see how to avoid writing code that's at risk, determine which fixes are in place, and determine which fixes still need to be implemented to fix the highlighted code.
* **Identify impacts** – With hotspots you'll learn how to apply fixes to secure your code based on the impact on overall application security. Recommended secure coding practices are included on the Hotspots page to assist you during your review.

## Lifecycle
Security Hotspots have a dedicated lifecycle. To make status changes, the user needs the **Administer Security Hotspots** permission. This permission is enabled by default. Users with the **Browse** permission can comment on or change the user assigned to a Security Hotspot.

### Statuses  
Through the lifecycle, a Security Hotspot takes one of the following statuses:

* **To Review** – the default status of new Security Hotspots set by SonarQube. A Security Hotspot has been reported and needs to be checked.
* **Reviewed** – a developer has manually assessed the Security Hotspot and decided whether or not to apply a fix.

## Workflow  
Follow this workflow to review Security Hotspots and apply any fixes needed to secure your code.

### Review Priority
When SonarQube detects a Security Hotspot, it's added to the list of Security Hotspots according to its review priority from High to Low. Hotspots with a High Review Priority are the most likely to contain code that needs to be secured and require your attention first. 

Review Priority is determined by the security category of each security rule. Rules in categories that are ranked high on the OWASP Top 10 and CWE Top 25 standards are considered to have a High Review Priority. Rules in categories that aren't ranked high or aren't mentioned on the OWASP Top 10 or CWE Top 25 standards are rated as Medium or Low.

### Reviewing Hotspots  
When reviewing a Hotspot, you should:

1. Review the **What's the risk** tab to understand why the Security Hotspot was raised.
1. From the **Are you at risk** tab, read the **Ask Yourself Whether** section to determine if you need to apply a fix to secure the code highlighted in the Hotspot.
1. From the **How can you fix it** tab, follow the **Recommended Secure Coding Practices** to fix your code if you've determined it's unsafe.

After following these steps, set the Security Hotspot to one of the following:

* **Fixed** – if you have applied a fix to secure the code highlighted by the Hotspot.
* **Safe** – if the code is already secure and doesn't need to be fixed. (for example, other more relevant protections are already in place).
* **Needs additional review** – if you need another user's review.

### Review History

The **Review history** tab shows the history of the Security Hotspot including the status it's been assigned and any comments the reviewer had regarding the Hotspot.  
