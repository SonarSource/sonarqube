---
title: Fixing the Water Leak
url: /user-guide/fixing-the-water-leak/
---

## What is the Water Leak

Imagine you come home one day to find a puddle of water on the kitchen floor. As you watch, the puddle slowly gets larger.

Do you reach for the mop? Or do you try to find the source and fix it? The choice is obvious, right? You find the source of the leak!

So why do anything different with code quality? When you analyze an application with {instance} and realize that it has a lot of technical debt, the knee-jerk reaction is generally to start remediating – either that or put together a remediation plan. This is like mopping the floor once a day while ignoring the source of the water.

Typically in this traditional approach, just before release a periodic code quality audit result in findings the developers should act on before releasing. This approach might work in the short term, especially with strong management backing, but it consistently fails in the mid to long run, because:

* The code review comes too late in the process, and no stakeholder is keen to get the problems fixed; everyone wants the new version to ship.
* Developers typically push back on the recommendations made by an external team that doesn't know the context of the project. And by the way the code under review is obsolete already.
* There is a clear lack of ownership for code quality with this approach. Who owns quality? No one!
* What gets reviewed is the entire application before it goes to production and it is obviously not possible to apply the same criteria to all applications. A negotiation will happen for each project, which will drain all credibility from the process

Instead, why not apply the same simple logic you use at home to the way you manage code quality? Fixing the leak means putting the focus on the “new” code, i.e. the code that was added or changed since the last release. Then things get much easier:

* The [Quality Gate](/user-guide/quality-gates/) can be run every day, and passing it is achievable. There are no surprises at release time.
* It's pretty difficult for developers to push back on problems they introduced the previous day. Instead, they're generally happy to fix the problems while the code is still fresh.
* There is a clear ownership of code quality
* The criteria for go/no-go are consistent across applications, and are shared among teams. Indeed new code is new code, regardless of which application it is done in
* The cost is insignificant because it is part of the development process

As a bonus, the code that gets changed the most has the highest maintainability, and the code that doesn't get changed has the lowest, which makes a lot of sense. Because of the nature of software, and the fact that we keep making changes to it, the debt will naturally be reduced. Where it isn’t is where it doesn't need to be.

## How to do it

<!-- sonarqube -->SonarQube<!-- /sonarqube --><!-- sonarcloud -->SonarCloud<!-- /sonarcloud --> offers two main tools to help you find your leaks:

* New Code metrics show the variance in your measures between the current code and a specific point you choose in its history, typically the `previous_version`
* New Code is primarily detected based on SCM "blame" data starting from the first analysis within your New Code Period (formerly the "Leak Period"), with fallback mechanisms when needed. See [SCM integration](/analysis/scm-integration/) for more details.
* [Quality Gates](/user-guide/quality-gates/) allow you to set boolean thresholds against which your code is measured. Use them with differential metrics to ensure that your code quality moves in the right direction over time.
