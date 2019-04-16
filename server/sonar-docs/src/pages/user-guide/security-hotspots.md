---
title: Security Hotspots
url: /user-guide/security-hotspots/
---

## What is a Security Hotspot?

Unlike Vulnerabilities, Security Hotspots aren't necessarily issues that are open to attack. However, Security Hotspots highlight security-sensitive pieces of code that someone focused on security needs to review. Upon review, that security-focused developer or reviewer might find that a Security Hotspot is in fact a Vulnerability that needs to be fixed. 

## Why are Security Hotspots Important?

Security Hotspots help focus the efforts of developers or reviewers who are manually checking security-sensitive code. Reviewing Security Hotspots allows you to:

* **Fix security issues** – Reviewing Security Hotspots gives you the opportunity to detect true vulnerabilities and ensure issues are fixed before merging pull requests or releasing your branch.
* **Learn about security** – {instance} explains why your code was identified as a Security Hotspot and the link between your Hotspots and well-known attacks or weaknesses such as SQL Injection, Weak Cryptography, or Authentication. Knowing this makes developers aware of when they're working on security-sensitive code and helps them avoid creating Vulnerabilities.

## Security Hotspot Lifecycle
Security Hotspots have a dedicated lifecycle and must be reviewed by a user with the "Administer Security Hotspots" permission. 

### Status

Through the lifecycle, a Security Hotspot takes one of the following statuses:

* **Open** - set by {instance} on new Security Hotspots
* **Resolved** (Won't Fix) - set automatically by {instance} when a reviewer accepts the fix done by a developer on a Manual Vulnerability or when a reviewer clears an open Hotspot or manual Vulnerability.
* **To Review** - set automatically when a developer requests a review on a manual Vulnerability.
* **Reopened** - set when a developer dismisses an open manual Vulnerability or when a reviewer manually reopens a resolved Security Hotspot in order to run a new audit on it.

A Security Hotspot is only closed if the code containing it is deleted. A Security Hotspot's status will be "Removed" if the rule in the project's Quality Profile identifying it is removed.

### Actions

Security Hotspots allow the following actions:

* **Detect** - Confirms a Security Hotspot as a true issue and manually opens a Vulnerability. Requires "Administer Security Hotspots" permission on the project.
* **Clear** - Marks a Security Hotspot or manually opened Vulnerability as being without issue and shouldn't be fixed. Requires "Administer Security Hotspots" permission on the project.
* **Request Review** - Request that a reviewer analyzes changes made to fix a manually opened Vulnerability.
* **Reject** - After review, reject the fix for a manually opened Vulnerability and return it to an open issue. Requires "Administer Security Hotspots" permission on the project.

### Workflow

If a reviewer with "Administer Security Hotspots" permission determines a Hotspot is actually a Vulnerability, the reviewer changes the status from its current state (probably **Open**) to **Detect**. This converts the Security Hotspot to a Vulnerability, and the developer who last touched the line will receive "new issue" notifications (if she's signed up to get them).

Once a Vulnerability is *Detect*ed at a Security Hotspot location, the following occurs:

1. The Security Hotspot is assigned to the appropriate developer, and the developer makes a fix.
2. The developer must then **Request Review** *via the UI*. The request moves the Vulnerability back to Security Hotspot. 
3. The reviewer either **Accepts** or **Rejects** the fix. 
	* Accepting the fix marks the Security Hotspot "Won't Fix." 
	* Rejecting the fix turns the Security Hotspot back into a Vulnerability and puts it back in the developer's queue.

### Marking a Hotspot "Won't Fix"
The `Won't Fix` designation indicates that a Hotspot has been reviewed and there is no way, as of now, to exploit this piece of code to create an attack.
