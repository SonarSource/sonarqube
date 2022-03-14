---
title: Security Reports
url: /user-guide/security-reports/
---

*Security Reports are available starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html).*

## What do Security Reports show?
Security Reports quickly give you the big picture on your application's security. They allow you to know where you stand compared to the most common security mistakes made in the past: [OWASP Top 10 2021](https://owasp.org/Top10/), [OWASP Top 10 2017](https://owasp.org/www-project-top-ten/2017), [CWE Top 25 2021](https://cwe.mitre.org/top25/archive/2021/2021_cwe_top25.html), [CWE Top 25 2020](https://cwe.mitre.org/top25/archive/2020/2020_cwe_top25.html), and [CWE Top 25 2019](https://cwe.mitre.org/top25/archive/2019/2019_cwe_top25.html). They represent a bare minimum to comply with for anyone putting in place secure development lifecycle.

[[warning]]
| The SANS Top 25 report is based on outdated statistics and should no longer be used. Instead, we recommend using the CWE Top 25 reports.

Security Reports rely on the rules activated in your Quality Profiles to raise security issues. If there are no rules corresponding to a given OWASP category activated in your Quality Profile, you won't get issues linked to that specific category and the rating displayed will be **A**. That doesn't mean you are safe for that category, but that you need to activate more rules (assuming some exist) in your Quality Profile.

## What's the difference between a Security Hotspot and a Vulnerability?

Security Hotspots and Vulnerabilities differ in that:

* A Security Hotspot is a security-sensitive piece of code that is highlighted but doesn't necessarily impact the overall application security. It's up to the developer to review the code to determine whether or not a fix is needed to secure the code.
* A vulnerability is a problem that impacts the application's security that needs to be fixed immediately.

For more details, see the [Security Hotspots](/user-guide/security-hotspots/) page. 

## Why don't I see any Vulnerabilities or Security Hotspots?
You might not see any Vulnerabilities or Security Hotspots for the following reasons:
* Your code has been written without using any security-sensitive API. 
* Vulnerability or Security Hotspot rules are available but not activated in your Quality Profile so no Security Hotspots or Vulnerabilities are raised.
* SonarQube might not currently have many rules for your language, so it won't raise any or only a few Vulnerabilities or Security Hotspots.

## Downloading a PDF copy
You can download a PDF copy of your Security Reports by clicking the **Download as PDF** button in the upper-right corner of the **Security Reports** page. 

The PDF contains:

- the number of open Vulnerabilities and the Security Rating on both overall code and new code.
- the number of Security Hotspots, the percentage of reviewed Security Hotspots, and the Security Review rating on both overall and new code. 
- your SonarSource, OWASP Top 10, and CWE Top 25 2020 reports.
