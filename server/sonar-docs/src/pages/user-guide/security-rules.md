---
title: Security-related Rules
url: /user-guide/security-rules/
---
The {instance} Quality Model has four different types of rules: Reliability (bug), Maintainability (code smell), Security (vulnerability and hotspot) rules. There are a lot of expectations about security, so below we explain some key concepts and how the security rules differ from others.

## What to expect from security-related rules
As with other types of rules, we try to raise no false positives: you should be confident that anything reported to you as an issue is really an issue.

Under the hood SonarQube is based on different representations of the source code and technologies in order to be able to detect any kind of security issue:
* **Security-injection rules**: there is a vulnerability here when the inputs handled by your application are controlled by a user (potentially an attacker) and not validated or sanitized, when this occurs, the flow from sources (user-controlled inputs) to sinks (sensitive functions) will be presented. To do this, SonarQube uses well-known taint analysis technology on source code which allows, for example, the detection of:
  * [CWE-89](https://cwe.mitre.org/data/definitions/89.html): SQL Injection
  * [CWE-79](https://cwe.mitre.org/data/definitions/79.html): Cross-site Scripting
  * [CWE-94](https://cwe.mitre.org/data/definitions/94.html): Code Injection
* **Security-configuration rules**: here there is a security issue because the wrong parameter (eg: invalid cryptographic algorithm or TLS version) when calling a sensitive function has been set or when a check (eg: check_permissions() kind of function) was not done or not in the correct order, this problem is likely to appear often when the program is executed (no injected/complex attacks are required unlike in the previous category):
  * [CWE-1004](https://cwe.mitre.org/data/definitions/1004.html): Sensitive Cookie Without 'HttpOnly' Flag
  * [CWE-297](https://cwe.mitre.org/data/definitions/297.html): Improper Validation of Certificate with Host Mismatch
  * [CWE-327](https://cwe.mitre.org/data/definitions/327.html): Use of a Broken or Risky Cryptographic Algorithm

These security issues are then divided into two categories: vulnerabilities and hotspots (see the main differences on the [Security Hotspots](/user-guide/security-hotspots/) page). Security Hotspots have been introduced for security protections that have no direct impact on the overall application's security. Most injection rules are vulnerabilities, for example, if a SQL injection is found, it is certain that a fix (input validation) is required, so this is a vulnerability. On the contrary, the *httpOnly* flag when creating a cookie is an additional protection desired (to reduce the impact when XSS vulnerabilities appear) but not always possible to implement or relevant depending on the context of the application, so it's a hotspot. 

With Hotspots, we want to help developers understand information security risks, threats, impacts, root causes of security issues, and the choice of relevant software protections. In short, we really want to educate developers and help them develop secure, ethical, and privacy-friendly applications.

## Which security-standards are covered?
Our security rules are classified according to well-established security-standards such as:
* [CWE](https://cwe.mitre.org/): SonarQube is a CWE compatible product [since 2015](https://cwe.mitre.org/compatible/questionnaires/33.html).
* [SANS Top 25](https://www.sans.org/top25-software-errors/)
* [OWASP Top 10 ](https://www.owasp.org/index.php/Top_10-2017_Top_10)

The standards to which a rule relates will be listed in the **See** section at the bottom of the rule description. More generally, you can search for a rule on [rules.sonarsource.com](https://rules.sonarsource.com/):
* [Java-vulnerability-issue-type](https://rules.sonarsource.com/java/type/Vulnerability): all vulnerability rules for Java language.
* [Java-hotspots-issue-type](https://rules.sonarsource.com/java/type/Security%20Hotspot): all security-hotspot rules for Java language.
* [Java-tag-injection](https://rules.sonarsource.com/java/tag/injection): all security-injection rules for Java language.

## How to propose new security-rules?
Security is a lively world where new types of attacks and vulnerabilities appear very often, so we welcome any suggestions for new security-rules. You can read the [adding coding rules](/extend/adding-coding-rules/) page to see how to develop a new rule or propose a new one [on our community forum](https://community.sonarsource.com/c/suggestions/rules/13).

Regarding the security-injection rules mentioned above, it's possible to [extend the taint analysis configuration](/analysis/security_configuration/) to allow the SonarQube engine to use new sources, sanitizers, validators and sinks of the homemade-frameworks that you use. Security Engine Custom Configuration is available as part of the Enterprise Edition and above.
