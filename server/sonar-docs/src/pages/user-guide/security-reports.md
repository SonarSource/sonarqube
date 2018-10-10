---
title: Security Reports
url: /user-guide/security-reports/
---

## What do the Security Reports show?
The Security Reports are designed to quickly give you the big picture on your application's security, with breakdowns of just where you stand in regard to each of the [OWASP Top 10](https://www.owasp.org/index.php/Top_10-2017_Top_10), and [SANS Top 25](https://www.sans.org/top25-software-errors) categories, and [CWE](http://cwe.mitre.org/)-specific details.
The Security Reports are fed by the analyzers, which rely on the rules activated in your quality profiles to raise security issues. If there are no rules corresponding to a given OWASP category activated in your Quality Profile, you will get no issues linked to that specific category and the rating displayed will be A. That won't mean you are safe for that category, but that you need to activate more rules (assuming some exist).

## What's the difference between a Hotspot and a Vulnerability?
Vulnerabilities are points in the code which are open to attack.
Security Hotspots are security-sensitive pieces of code that should be carefully reviewed by someone with a security auditor hat. This person can be:
* a member of the development team who is more sensitive to security problems 
* someone outside the development team contracted for the purpose of reviewing these Hotspots.

The main goal of Security Hotspots is to help focus the efforts of the security auditors who manually review application source code. The second goal is to educate developers and to increase their security-awareness. 
Having a Hotspot in your application does not mean there is a problem. What it does mean is that a human, preferably a security auditor/expert should look over the code to see if the sensitive piece of code is being used in the safest manner.

## Why are some Hotspot and Vulnerability rules very similar?
They are overlapping on purpose. The Hotspot rule is supposed to include all matches of the Vulnerability rules, and cases where the taint analysis engine is not able to detect vulnerabilities. For example, switching from one language to another (XML, JNI, etc...) or using some third party libraries will prevent the taint analysis from finding vulnerabilities. A Vulnerability rule highlights security threats only when it has a high level of confidence, which means that it will always miss some of them. Whereas a Hotspot rule guides secure code reviews by showing code where those issues might lurk, even if it could not detect any vulnerability.

## Why don't I see any Hotspots?
They are three reasons you might not see any Hotspots:
* it is possible you really have none of them because the code has been written without using any security-sensitive API. 
* it is possible that Hotspot rules are available, but not yet activated in your Quality Profile, and so naturally no issues are raised
* it is more likely that the analyzer for the langauge you're using does not yet offer Hotspot rules, and so it doesn't raise any Hotspots regardless of the quality of how many are actually there, but this last option will disappear over time.

## Why don't I see any Vulnerabilities?
You might not see any Vulnerabilities for more or less the same reasons as for Hotspots, but it may be more surprising for Vulnerabilities because you may see some Vulnerabilities reported in the Project homepage, while there are none in the Security Reports. This is because the language analyzer may not yet provide the "Security Standards" metadata required for issues to be visible on the Security Reports. This metadata is basically the link between a Rule (and its issues) and the "OWASP Top 10" or "SANS Top 25" categories. Without this link, there is no way to associate an already existing Vulnerability to the Security Standard categories and so to display security issues correctly in the reports. Every analyzer version released by SonarSource after July 2018 should feed the "Security Standards" and be compatible with the Hotspot issue type. 

## I'm a developer. Should I care about Hotspots?
Probably not. Hotspots, as such, aren't really actionable. They simply mark *potential* problems, so there's really nothing to do immediately on the code. That's why you don't receive notifications when Hotspot issues are raised, and why Hotspots aren't shown in the Issues page by default.

## What if my Hotspot really marks a Vulnerability?
If you look at the code where a Hotspot is raised and realize that there really is a problem, click on the current status (probably `Open`) to register that you've *Detect*ed a Vulnerability at that point in the code. Once you do, it will be converted to a Vulnerability, and the developer who last touched the line will receive "new issue" notifications (if she's signed up to get them).

## What happens after my Hotspot becomes a Vulnerability?
Once you've *Detect*ed that there really is a problem at a Hotspot location, it will be assigned to the appropriate developer, who will make a fix, and must then `Request Review` *via the UI*. That request moves the issue from Vulnerability back to Hotspot. From there, it's up to the security auditor to either `Accept` or `Reject` the fix. Accepting the fix will mark it `Won't Fix`, and rejecting it will turn it back into a Vulnerability, putting it back in the developer's queue.

## What does it mean for a Hotspot to be marked "Won't Fix"?
The `Won't Fix` designation is used to indicate that a Hotspot has been reviewed and there is no way, as of now, to exploit this piece of code to create an attack.


