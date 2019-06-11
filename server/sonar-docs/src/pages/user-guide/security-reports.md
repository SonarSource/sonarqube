---
title: Security Reports
url: /user-guide/security-reports/
---

## What do Security Reports show?
Security Reports quickly give you the big picture on your application's security, with breakdowns of just where you stand in regard to each of the [OWASP Top 10](https://www.owasp.org/index.php/Top_10-2017_Top_10), and [SANS Top 25](https://www.sans.org/top25-software-errors) categories, and [CWE](http://cwe.mitre.org/)-specific details.

The Security Reports are fed by the analyzers, which rely on the rules activated in your quality profiles to raise security issues. If there are no rules corresponding to a given OWASP category activated in your Quality Profile, you will get no issues linked to that specific category and the rating displayed will be A. That won't mean you are safe for that category, but that you need to activate more rules (assuming some exist).

## What's the difference between a Security Hotspot and a Vulnerability?
Vulnerabilities are points in the code which are open to attack. Security Hotspots highlight security-sensitive pieces of code that need to be manually reviewed to ensure the sensitive piece of code is being used in the safest manner. Security hotspots also help educate developers on security issues. 

For more details, see [Security Hotspots](/user-guide/security-hotspots/)

## Why are some Security Hotspot and Vulnerability rules very similar?
They are overlapping on purpose. The Security Hotspot rule is supposed to include all matches of the Vulnerability rules, and cases where the taint analysis engine is not able to detect vulnerabilities. For example, switching from one language to another (XML, JNI, etc...) or using some third party libraries will prevent the taint analysis from finding vulnerabilities. A Vulnerability rule highlights security threats only when it has a high level of confidence, which means that it will always miss some of them. Whereas a Security Hotspot rule guides secure code reviews by showing code where those issues might lurk, even if it could not detect any vulnerability.

## Why don't I see any Vulnerabilities or Security Hotspots?
You might not see any Vulnerabilities or Security Hotspots for the following reasons:
* You don't have any because the code has been written without using any security-sensitive API. 
* Vulnerability or Security Hotspot rules are available but not activated in your Quality Profile so no Security Hotspots or Vulnerabilities are raised.
* The analyzer for your language might only currently offer a few rules and won't raise any or only a small number of Vulnerabilities or Security Hotspots.
