---
title: Security Reports
---

## What do the Security Reports show?
The Security Reports are designed to quickly give you the big picture on your application's security, with breakdowns of just where you stand in regard to each of the [OWASP Top 10](https://www.owasp.org/index.php/Top_10-2017_Top_10), and [SANS Top 25 categories](https://www.sans.org/top25-software-errors), and [CWE](http://cwe.mitre.org/)-specific details. 

## What's the difference between a Hotspot and a Vulnerability?
Vulnerabilities are points in the code which are open to attack.
Security Hotspots are security-sensitive pieces of code that deserve to be carefully reviewed by a security auditor. The main goal of those Security Hotspots is to drive the effort of security auditors in charge of manually review the source code of an application. The second goal is to educate developers and to increase their security-awareness. 
Having a Hotspot in your application does not mean there is a problem. What it does mean is that a human, preferably a security auditor/expert should look over the code to see if the sensitive piece of code is being used in the safest manner.

## I'm a developer. Should I care about Hotspots?
Probably not. Hotspots, as such, aren't really actionable. They simply mark *potential* problems, so there's really nothing to do. That's why you don't receive notficiations when Hotspot issues are raised, and why Hotspots aren't shown in the Issues page by default. 

## What if my Hotspot really marks a Vulnerability?
If you look at the code where a Hotspot is raised and realize that there really is a problem, click on the current status (probably `Open`) to register that you've *Detect*ed a Vulnerability at that point in the code. Once you do, it will be converted to a Vulnerability, and the developer who last touched the line will receive "new issue" notifications (if she's signed up to get them).

## What happens after my Hotspot becomes a Vulnerability?
Once you've *Detect*ed that there really is a problem at a Hotspot location, it will be assigned to the appropriate developer, who will make a fix, and must then `Request Review` *via the UI*. That request moves the issue from Vulnerability back to Hotspot. From there, it's up to the security auditor to either *Accept* or *Reject* the fix. Accepting the fix will mark it "Won't Fix", and rejecting it will turn it back into a Vulnerability, putting it back in the developer's queue.

## What does it mean for a Hotspot to be marked "Won't Fix".
The Won't Fix designation is used to indicate that a Hotspot has been reviewed and there is no way, as of now, to exploit this piece of code to create an attack.

## Why don't I see any Hotspot?
It is possible you really have no Hotspot because your code is safe in itself and you used no security-sensitive API.
Also, only SonarC#, SonarJava, SonarPHP implement the concept of Security Hotspot.
