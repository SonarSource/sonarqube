---
title: Contributing
url: /extend/contributing/
---

Please be aware that we are not actively looking for feature contributions to SonarQube itself because it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes for SonarQube, but we do happily welcome contributions to the other open source projects under the SonarSource umbrella. 


## General guidelines
* Choose an open ticket in [JIRA](https://jira.sonarsource.com/secure/Dashboard.jspa) or propose your change on the [SonarQube Community Forum](https://community.sonarsource.com) - the discussion there is likely to result in an open JIRA ticket. ;-)
* Use the SonarSource conventions, which you'll find neatly packaged here: https://github.com/SonarSource/sonar-developer-toolset#the-almost-unbreakable-build
* Use pull requests to submit your work

## New rule implementations in existing plugins
* Start from an existing [RSpec](https://jira.sonarsource.com/browse/RSPEC-1973?filter=10375) (Rule Specification) that lists your language of interest in the "Targeted languages" field. 
   * If the RSpec you're interested in doesn't target the language where you want to implement it, raise the question on the Community Forums .
   * If no RSpec exists for the rule you want to implement, raise the question on the [Community Forum](https://community.sonarsource.com/).
* Put your rule implementation class in the [language]-checks (e.g. java-checks, javascript-checks, &etc.) module, in the checks sub-package
* The naming convention for implementation classes is [A-Z][A-Za-z]+Check.java. (Yes, put "Check" in the name too.) The class name should be descriptive and not reflect the rule key. E.G. FindBadCodeCheck.java, not S007.java.
* A good way to get started on a rule implementation is to look at the implementations of rules that do similar things.
* During development there's no need to load the plugin in a server to test your implementation, use the rule's unit test for that.
* For a complete implementation, make sure all of the following are done:
   * create HTML description file and metadata file
   * write test class
   * register the rule in CheckList.java
   * add the rule to the profile used for the integration test in `profile.xml`
   * run the integration test and add any new issues to the set of expected issues 
