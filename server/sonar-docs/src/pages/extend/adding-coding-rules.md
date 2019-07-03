---
title: Adding Coding Rules
url: /extend/adding-coding-rules/
---

There are two ways to extend coding rules:

* Writing coding rules using Java via a SonarQube plugin
* Adding XPath rules directly through the SonarQube web interface

If both are available, the Java API will be more fully-featured than what's available for XPath, and is generally preferable.

[[info]]
| ![](/images/info.svg) Before implementing a new coding rule, you should consider whether it is specific to your own context or might benefit others. If it might benefit others, you can propose it on the [Community Forum](https://community.sonarsource.com/). If there is shared interest, then it might be implemented for you directly in the related language plugin. It means less maintenance for you, and benefit to others.

## Custom rule support by language
Languages not listed here don't support custom rules

 &nbsp;  |XPath 1.0|Java|Other
---|---|---|---
C#| -  | -  | ![](/images/check.svg)[Importing Issues from Third-Party Roslyn Analyzers (C#, VB.NET)](/analysis/external-issues/)
COBOL| ![](/images/check.svg) | -  | - 
Flex|![](/images/check.svg) |  -  | - 
Java |  -  | ![](/images/check.svg) |  - 
JavaScript |  -  | ![](/images/check.svg) | - 
PHP |  -  | ![](/images/check.svg)|  - 
PL/SQL | ![](/images/check.svg) |  -  | - 
PL/I | ![](/images/check.svg) |  -  | - 
RPG |  -  | ![](/images/check.svg)| - 
VB.NET|  -  |  -  | ![](/images/check.svg)[Importing Issues from Third-Party Roslyn Analyzers (C#, VB.NET)](/analysis/external-issues/)
XML |  ![](/images/check.svg) |  -  | - 


## Adding coding rules using Java
Writing coding rules in Java is a six-step process:

* Create a SonarQube plugin.
* Put a dependency on the API of the language plugin for which you are writing coding rules.
* Create as many custom rules as required.
* Generate the SonarQube plugin (jar file).
* Place this jar file in the SONARQUBE_HOME/extensions/plugins directory.
* Restart SonarQube server.

See the following pages to see samples and details about how to create coding rules 

* [for COBOL](/analysis/languages/cobol/)
* [for Java](/analysis/languages/java/)
* [for JavaScript](/analysis/languages/javascript/)
* [for PHP](/analysis/languages/php/)
* [for RPG](/analysis/languages/rpg/)


## Adding coding rules using XPATH

SonarQube provides a quick and easy way to add new coding rules directly via the web interface for certain languages using XPath 1.0 expressions. For XML, which is already immediately accessible to XPath, you can simply write your rules and check them using any of the [freely available tools](http://codebeautify.org/Xpath-Tester) for examining XPath on XML. If you're writing rules for XML, skip down to the Adding your rule to the server section once you've got your rules written.

For other languages how to access a variable, for example, in XPath is less obvious, so we've provided tools.

### Writing an XPath Rule using SSLR Toolkit
The rules must be written in XPath (version 1.0) to navigate the language's [Abstract Syntax Tree](http://en.wikipedia.org/wiki/Abstract_syntax_tree) (AST). For most languages, an SSLR Toolkit is provided to help you navigate the AST. You need to download the `sslr-{language}-toolkit-{version}.jar` file corresponding to the version of your language plugin you have on your SonarQube instance.

Each language's SSLR Toolkit is a standalone application that displays the AST for a piece of code source that you feed into it, allowing you to read the node names and attributes from your code sample and write your XPath expression. Knowing the XPath language is the only prerequisite, and there are a lot of tutorials on XPath online.

The latest version of SSLR Toolkit can be downloaded from following locations:

* [Flex](https://binaries.sonarsource.com/Distribution/sonar-flex-plugin/)
* [PL/SQL](https://binaries.sonarsource.com/CommercialDistribution/sslr-plsql-toolkit/)
* [PL/I](https://binaries.sonarsource.com/CommercialDistribution/sslr-pli-toolkit/)
* [Python](https://binaries.sonarsource.com/Distribution/sslr-python-toolkit/)

For an SSLR preview, consider the following source code sample:
```
function HelloWorld(hour) {
  if (hour) {
    this.hour = hour;
  } else {
    var date = new Date();
    this.hour = date.getHours();
  }
  this.displayGreeting = function() {
    if (this.hour >= 22 || this.hour <= 5)
      document.write("Good night, World!");
    else
      document.write("Hello, World!");
  } 
}
```
While parsing source code, SonarQube builds an Abstract Syntax Tree (AST) for it, and the SSLR Toolkit provided for each language will show you SonarQube's AST for a given piece of code. Here's the AST for our sample:

![AST example](/images/astSample.png)

The [XPath](http://en.wikipedia.org/wiki/XPath) language provides a way to write coding rules by navigating this AST, and the SSLR Toolkit for the language will give you the ability to test your new rules against your sample code.

### Adding your Rule to the Server
Once your new rule is written, you can add it SonarQube:

1. Login as an Quality Profile Administrator
1. Go to Rules page
1. Select the Language for which you want to create the XPath rule
1. Tick the Template criterion and select "Show Templates Only" 
1. Look for the XPath rule template
1. Click on it to select it, then use the interface controls to create a new instance
1. Fill in the form that pops up
1. Once you've created your rule, you'll need to add it to a Quality Profile and run analysis to see it in action.


## Coding rule guidelines
These are the guidelines that SonarSource uses internally to specify new rules. Rules in community plugins are not required to adhere to these guidelines. They are provided here only in case they are useful.

Note that fields "title", "description" and "message" have a different format when the rule type is "Hotspot".

### Guidelines for Bug, Vulnerability, and Code Smell rules
#### Titles

* The title of the rule should match the pattern "X should [ not ] Y" for most rules. Note that the "should [ not ]" pattern is too strong for Finding rules, which are about observations on the code. Finding titles should be neutral, such as "Track x".
* All other things being equal, the positive form is preferred. E.G.
   * "X should Y" is preferred to 
   * "X should not Z"
* Titles should be written in plural form if at all possible. E.G.
   * ![](/images/check.svg)Flibbers should gibbet
   * ![](/images/cross.svg)A Flibber should gibbet
* Any piece of code in the rule title should be double-quoted (and not single-quoted).
* There should be no category/tag prefixed to the rule title, such as "Accessibility - Image tags should have an alternate text attribute"
* Titles should be as concise as possible. Somewhere around 70 or 80 characters is an ideal maximum, although this is not always achievable.

Noncompliant Title Examples:

* File should not have too many lines of code  // Noncompliant; singular form used
* Avoid file with too many lines of code  // Noncompliant; doesn't follow "x should [not] y" pattern
* Too many lines of code  // Noncompliant
* Don't use "System.(out/err)"  // Noncompliant
* Parameters in an overriding virtual function should either use the same default arguments as the function they override, or not specify any default arguments  // Noncompliant; waaaay too long

Compliant Solutions:

* Files should not have too many lines of code  
* "System.(out/err)" should not be used to log messages
* Overriding virtual functions should not change parameter defaults

Starting with the subject, such as "Files", will ensure that all rules applying to files will be grouped together.

#### Descriptions
Rule descriptions should contain the following sections in the listed order:

* **Rationale** (unlabeled) - explaining why this rule make sense. 
If it's not absolutely clear from the rationale what circumstances will cause an issue to be raised, then this section should end with "This rule raises an issue when \[ insert circumstances here ]".
* **Noncompliant Code Example** - providing some examples of issues
   * Ideally, the examples should depend upon the default values of any parameters the rule has, and these default values should be mentioned before the code block. This is for the benefit of users whose rule parameters are tuned to something other than the default values. E.G.
With a parameter of: <code>*:.*log4j.*</code>
   * The lines in these code samples where issues are expected should be marked with a "Noncompliant" comment
   * "Compliant" comments may be used to help demonstrate the difference between what is and is not allowed by the rule
   * It is acceptable to omit this section when demonstrating noncompliance would take too long, e.g. "Classes should not have too many lines of code"
* **Compliant Solution** - demonstrating how to fix the previous issues. Good to have but not required for rules that detect bugs. 
   * There is no need to mark anything "Compliant" in the Compliant Solution; everything here is compliant by definition
   * It is acceptable to omit this section when there are too many equally viable solutions.
* **Exceptions** (optional) - listing and explaining some specific use cases where no issues are logged even though some might be expected. Note that this is sometimes incorporated into the rationale, instead.
* **See** (optional) - listing references and/or links to external standards like MISRA, SEI, CERT, &etc.
Deprecated (optional): listing replacement rules with links

Code samples for COBOL should be in upper case. 

When displayed in SonarQube, any code or keywords in the description should be enclosed in <code> tags. For descriptions written in JIRA, this means using double curly braces (`{{` and `}}`) to enclose such text. They will be translated in the final output.

#### Messages
Issue messages should contain the remediation message for bug and quality rules. For potential-bug rules, it should make it explicit that a manual review is required. It should be in the imperative mood ("Do x"), and therefore start with a verb.

An issue message should always end with a period ('.') since it is an actual sentence, unless it ends with a regular expression, in which case the regular expression should be preceded by a colon and should end the message.

Any piece of code in the the rule message should be double-quoted (and not single-quoted). Moreover, if an issue is triggered because a number was above a threshold value, then both the number and the threshold value should be mentioned in the issue message. 

Sample messages:

* Remove or refactor this useless "switch" statement. // Compliant
* This "switch" statement is useless and should be refactored or removed. // Noncompliant
* Every "switch" statement shall have at least one case-clause. // Noncompliant
* Rename this variable to comply with the regular expression: [a-z]+  // Compliant

[[collapse]]
| ## Sample Specification
| ### Generic exceptions should not be thrown
|
| Using generic exceptions such as `Error`, `RuntimeException`, `Throwable`, and `Exception` prevents calling methods from handling true, system-generated exceptions differently than application-generated errors.
| 
| **Noncompliant Code Example**  
| ```
| With the default regular expression [a-z][a-zA-Z0-9]+:
| 
| try { /* ... */ } catch (Exception e) { LOGGER.info("context"); } // Noncompliant; exception is lost
| try { /* ... */ } catch (Exception e) { LOGGER.info(e); } // Noncompliant; context is required
| try { /* ... */ } catch (Exception e) { LOGGER.info(e.getMessage()); } // Noncompliant; exception is lost (only message is preserved)
| try {
| /* ... */
| } catch (Exception e) { // Noncompliant - exception is lost
| throw new RuntimeException("context");
| }
| ```
|
| **Compliant Solution**  
| ```
| try { /* ... */ } catch (Exception e) { LOGGER.info("context", e); }
| try {
| /* ... */
| } catch (Exception e) {
| throw new RuntimeException("context", e);
| }
| ```
| **Exceptions**  
| Generic exceptions in the signatures of overriding methods are ignored.
| ```
| @Override
| public void myMethod() throws Exception {...}
| ```
| **See**  
| * MISRA C:2004, 4.5.2
| * MITRE, [CWE-580](http://cwe.mitre.org/data/definitions/580.html) - clone() Method Without super.clone()
|
| **See also**  
| S4567 - Rule title here

### Guidelines for Hotspot rules

See [RSPEC-4721](https://jira.sonarsource.com/browse/RSPEC-4721) for an example of Hotspot rule.

#### Titles
* The title should start with a verb in the present participle form (-ing)
* The title should end with "is security-sensitive"

Noncompliant Title Examples:

*  Avoid executing OS commands

Compliant Solution:

* Executing OS commands is security-sensitive
* Deserializing objects from an untrusted source is security-sensitive

#### Descriptions
Rule descriptions should contain the following sections in the listed order:

* **Rationale** (unlabeled) - explaining why this rule make sense.
   * It starts with a copy of the title. The "is security sensitive" part can be replaced with "can lead to ...<DESCRIBE RISK>" when there is one risk and it is easy to describe in a short manner.
   * Next is added the phrase "For example, it has led in the past to the following vulnerabilities:".
   * Next is a list of CVE links formatted as bullet points. Each CVE should point to their description on the (Example: CVE-2018-12465).
* **Ask Yourself Whether** - listing a set of questions which the developer should ask herself/himself.
   * Those questions should check if the context in which the code is makes it dangerous.
For example, if some code enables a user to insert custom data the database, one of the questions could be: Is the user input sanitized?
   * Some additional questions can be added to remind the developer that there might not be a need for this code.
For example, if some code enables a user to send and then execute custom code, the question could be: Does the user really need to execute code dynamically?
   * This section ends with "You are at risk if you answered yes to any of those questions." with an asterisk marking the corresponding questions if it is not the case for all of them.
* **Recommended Secure Coding Practices** - describing all the ways to mitigate the risk.
   * It usually contains a mix of all the advices provided by OWASP rules.
   * Add detailed solutions whenever possible.
* one of the following:
   * **Noncompliant Code Example** - same as for Bug, Vulnerability and Code Smell rules.
some code can be added to give an example of dangerous context. For example: putting a password in an insecure cookie.
   * **Questionable Code Example** - use this instead of "Noncompilant code example" when the Hotspot highlights some code which is not dangerous but might be the source of some vulnerability. Example: an opening Socket.
* **See** (optional) -  same as for Bug, Vulnerability and Code Smell rules.
* **Deprecated** (optional) -  listing replacement rules with links.

Guidelines regarding COBOL, keywords and code are the same as for other rules.

#### Messages
Most of the time you can paraphrase the title:
* start the sentence with "Make sure that"
* replace "is security-sensitive" with "is safe here"

However for some rules it can make sense to change the title. See the examples below: 

* Title: Executing OS commands is security-sensitive
   * Message: make sure that executing this OS command is safe here.
* Title: Delivering code in production with debug mode activated is security-sensitive
   * Message: Make sure this debug mode is deactivated before delivering the code in production.

### Guidelines applicable to all rules
#### See/References
When a reference is made to a standards specification, e.g. MISRA, the following steps must also be taken:

* add any related tags, such as: security, bug, &etc.
* add the relevant standard-related tag/label such as cwe, misra, etc. (If you forget, the overnight automation will remember for you.) 
* update the appropriate field on the References tab with the cited id. (If you forget, the overnight automation will remember for you.) 

If needed, references to other rules should be listed under a "See also" heading. If a "See" heading exists in the rule, then the "See also" title should be at the h3 level. Otherwise, use an h2 for it.

Other rules should be linked to only if they are related or contradictory (such as a pair of rules about where `{` should go).

Why list references to other rules under "see also" instead of "see"? The see section is used to support the current rule, and one rule cannot be used as justification for another rule. 

#### Rule Type
Now that you've fleshed out the description, you should have a fairly clear idea of what type of rule this is, but to be explicit:

**Bug** - Something that's wrong or potentially wrong. 

**Code Smell** - Something that will confuse a maintainer or cause her to stumble in her reading of the code.

**Vulnerability** - Something that has a high chance of being exploited by an attacker.

**HotSpot** - Something that could result in a vulnerability depending on the context in which this code is present. 

Sometimes the line between Bug and Code Smell is fuzzy. When in doubt, ask yourself: "Is code that breaks this rule doing what the programmer probably intended?" If the answer is "probably not" then it's a Bug. Everything else is a Code Smell.

#### Default severities
When assessing the default severity of a rule, the first thing to do is ask yourself "what's the worst thing that could happen?" In answering, you should factor in Murphy's Law without predicting Armageddon.

Once you have your answer, it's time to assess whether the Impact and Likelihood of the Worst Thing are High or Low. To do that, ask yourself these specific questions:

Vulnerability
* Impact: Could the exploitation of the vulnerability result in significant damage to your assets or your users? (Yes = High)
* Likelihood: What is the probability a hacker will be able to exploit the issue?

Bug
* Impact: Could the bug cause the application to crash or corrupt stored data?
(Languages where an error can cause program termination: COBOL, Python, PL/SQL, RPG.) 
* Likelihood: What is the probability the worst will happen?

Code Smell
* Impact: Could the Code Smell lead a maintainer to introduce a bug?
* Likelihood: What is the probability the worst will happen?

Once you have your Impact and Likelihood assessments, the rest is easy:

&nbsp;| impact|likelihood
---|---|---
Blocker|![](/images/check.svg)|![](/images/check.svg)
Critical|![](/images/check.svg)|![](/images/cross.svg)
Major|![](/images/cross.svg)|![](/images/check.svg)
Minor|![](/images/cross.svg)|![](/images/cross.svg)

#### Tags
Rules can have 0-n tags, although most rules should have at least one. Many of the common-across-languages tags are described in [the issues docs](/user-guide/issues/).

#### Evaluation of the remediation cost
For most rules, the SQALE remediation cost is constant per issue. The goal of this section is to help defining the value of this constant and to unify the way those estimations are done to prevent having some big discrepancies among language plugins. 

First step, classify the effort to do the remediation :

1. TRIVIAL
No need to understand the logic and no potential impact. 
Examples: remove unused imports, replace tabulations by spaces, remove call to System.out.println() used for debugging purpose, ...
1. EASY
No need to understand the logic but potential impacts. 
Examples: rename a method, rename a parameter, remove unused private method, ...
1. MEDIUM
Understanding the logic of a piece of code is required before doing a little and easy refactoring (1 or 2 lines of code). But understanding the big picture is not required.
Examples : CURSORs should not be declared inside a loop, EXAMINE statement should not be used, IF should be closed with END-IF, ...
1. MAJOR
Understanding the logic of a piece of code is required and it's up to the developer to define the remediation action.
Examples: Too many nested IF statements, Methods should not have too many parameters, UNION should not be used in SQL SELECT statements, Public java method should have a javadoc, Avoid using deprecated methods, ...
1. HIGH
The remediation action might lead to locally impact the design of the application.
Examples: Classes should not have too many responsibilities, Cobol programs should not have too many lines of code, Architectural constraint, ...
1. COMPLEX
The remediation action might lead to impact the overall design of the application.
Examples: Avoid cycles between packages, ...

Then use the following table to get the remediation cost according to the required remediation effort and to the language:

&nbsp;|Trivial|Easy|Medium|Major|High|Complex
---|---|---|---|---|---|---
ABAP, COBOL, ...| 10min | 20min | 30min | 1h | 3h | 1d 
Other languages| 5min |10min|20min|1h|3h|1d

For rules using either the "linear" or "linear with offset" remediation functions, the "Effort To Fix" field must be fed on each issue and this field is used to compute the remediation cost.  

#### Issue location(s) and highlighting
For any given rule, highlighting behavior should be consistent across languages within the bounds of what's relevant for each language.

When possible, each issue should be raised on the line of code that needs correction, with highlighting limited to the portion of the line to be corrected. For example:

* an issue for a misnamed method should be raised on the line with the method name, and the method name itself should be highlighted.

When correcting an issue requires action across multiple lines, the issue should be raised on the the lowest block that encloses all relevant lines. For example an issue for:

* method complexity should be raised on the method signature
* method count in a class should be raised on the class declaration

When an issue could be made clearer by highlighting multiple code segments, such as a method complexity issue, additional issue locations may be highlighted, and additional messages may optionally be logged for those locations. In general, these guidelines should be followed for secondary issue locations:

* highlight the minimum code to show the line's contribution to the issue. 
* avoid using an additional message if the secondary location is likely to be on the same issue as the issue itself. For example: the rule "Parameters should be final" will raise an issue on the method name, and highlight each non-final parameter. Since all locations are likely to be on the same line, additional messages would only confuse the issue.
* don't write a novel. The message for a secondary location is meant to be a hint to push the user in the right direction. Don't take over the interface with a narrative.

