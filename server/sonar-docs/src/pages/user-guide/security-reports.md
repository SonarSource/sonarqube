---
title: Security Reports
url: /user-guide/security-reports/
---

*Security Reports are available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

## What do Security Reports show?
Security Reports quickly give you the big picture on your application's security, with breakdowns of just where you stand in regard to each of the [OWASP Top 10](https://www.owasp.org/index.php/Top_10-2017_Top_10), and [SANS Top 25](https://www.sans.org/top25-software-errors) categories, and [CWE](https://cwe.mitre.org/)-specific details.

The Security Reports are fed by the analyzers, which rely on the rules activated in your quality profiles to raise security issues. If there are no rules corresponding to a given OWASP category activated in your Quality Profile, you will get no issues linked to that specific category and the rating displayed will be A. That won't mean you are safe for that category, but that you need to activate more rules (assuming some exist).

## What's the difference between a Security Hotspot and a Vulnerability?

For more details, see [Security Hotspots page](/user-guide/security-hotspots/) and to sum-up:
* With a Hotspot, a security-sensitive piece of code is highlighted, but the overall application security may not be impacted. It's up to the developer to review the code to determine whether or not a fix is needed to secure the code.
* With a vulnerability, a problem that impacts the application's security has been discovered that needs to be fixed immediately.
 

## Why don't I see any Vulnerabilities or Security Hotspots?
You might not see any Vulnerabilities or Security Hotspots for the following reasons:
* You don't have any because the code has been written without using any security-sensitive API. 
* Vulnerability or Security Hotspot rules are available but not activated in your Quality Profile so no Security Hotspots or Vulnerabilities are raised.
* The analyzer for your language might only currently offer a few rules and won't raise any or only a small number of Vulnerabilities or Security Hotspots.
