---
title: Built-in Rule Tags
url: /user-guide/built-in-rule-tags/
---
Tags are a way to categorize rules and issues. Issues inherit the tags on the rules that raised them. Some tags are language-specific, but many more appear across languages. Users can add tags to rules and issues, but most rules have some tags out of the box. Here is a non-comprehensive list of what some of those built-in tags mean:

*NOTE : Links below to [rules.sonarsource.com](https://rules.sonarsource.com) will be initially filtered for Java language rules*

* [brain-overload](https://rules.sonarsource.com/java/tag/brain-overload) - there is too much to keep in your head at one time
* [bad-practice](https://rules.sonarsource.com/java/tag/bad-practice) - the code likely works as designed, but the way it was designed is widely recognized as being a bad idea.
* [cert](https://rules.sonarsource.com/java/tag/cert) - relates to a rule in a [CERT](https://www.securecoding.cert.org/confluence/x/BgE) standard. There are currently three CERT standards: [C](https://www.securecoding.cert.org/confluence/x/HQE), [C++](https://www.securecoding.cert.org/confluence/x/fQI), and [Java](https://www.securecoding.cert.org/confluence/x/Ux). Many of these rules are not language-specific, but are good programming practices. That's why you'll see this tag on non-C/C++, Java rules.
* [clumsy](https://rules.sonarsource.com/java/tag/clumsy) - extra steps are used to accomplish something that could be done more clearly and concisely. (E.G. calling .toString() on a String).
* [confusing](https://rules.sonarsource.com/java/tag/confusing) - will take maintainers longer to understand than is really justified by what the code actually does
* [convention](https://rules.sonarsource.com/java/tag/convention) - coding convention - typically formatting, naming, whitespace...
* [cwe](https://rules.sonarsource.com/java/tag/cwe) - relates to a rule in the [Common Weakness Enumeration](http://cwe.mitre.org/). For more on CWE and on security-related rules in general, see [Security-related rules](/user-guide/security-rules/).
* [design](https://rules.sonarsource.com/java/tag/design) - there is something questionable about the design of the code
* [lock-in](https://rules.sonarsource.com/java/tag/lock-in) - environment-specific features are used
* [misra](https://rules.sonarsource.com/java/tag/misra) - relates to a rule in one of the [MISRA](http://www.misra.org.uk/) standards. While the MISRA rules are primarily about C and C++, many of them are not language-specific (E.G. don't use a float as a loop counter) but are simply good programming practices. That's why you'll see these tags on non-C/C++ rules.
* [owasp](https://rules.sonarsource.com/java/tag/owasp) - relates to a rule in the [OWASP Top Ten](https://www.owasp.org/index.php/Category:OWASP_Top_Ten_Project) security standards. Note, that the OWASP Top Ten is a list of high-level vulnerabilities which translates to many, many potential rules.
* [pitfall](https://rules.sonarsource.com/java/tag/pitfall) - nothing is wrong yet, but something could go wrong in the future; a trap has been set for the next guy and he'll probably fall into it and screw up the code.
* [sans-top25](https://rules.sonarsource.com/java/tag/sans-top25) - relates to the [SANS Top 25 Coding Errors](http://www.sans.org/top25-software-errors/), which are security-related. Note that  the SANS Top 25 list is pulled directly from the CWE.
* [suspicious](https://rules.sonarsource.com/java/tag/suspicious) - it's not guaranteed that this is a **bug**, but it looks suspiciously like one. At the very least, the code should be re-examined & likely refactored for clarity.
* [unpredictable](https://rules.sonarsource.com/java/tag/unpredictable) - the code may work fine under current conditions, but may fail erratically if conditions change.
* [unused](https://rules.sonarsource.com/java/tag/unused) - unused code, E.G. a private variable that is never used.
* [user-experience](https://rules.sonarsource.com/java/tag/user-experience) - there's nothing technically wrong with your code, but it may make some or all of your users hate you.
