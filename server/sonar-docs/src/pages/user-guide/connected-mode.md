---
title: SonarLint Connected Mode
url: /user-guide/connected-mode/
---
SonarLint Connected Mode connects SonarLint to your SonarQube project and provides additional benefits you won't get by using SonarLint or SonarQube alone.

**Shared code quality and security expectations**  
When using SonarLint alone, the Sonar way Quality Profile is used by default. If you're using a different Quality Profile in SonarQube, new issues may be raised in SonarQube even though your commit looked clean in SonarLint. With Connected Mode, the same customized rule set is applied in both your IDE and in SonarQube, and you're notified in your IDE when your project isn't meeting [Quality Gate](/user-guide/quality-gates/) standards.

**More security**  
When using SonarLint alone, taint analysis issues found by commercial editions of SonarQube aren't raised in SonarLint for performance reasons (we don't want to slow down your editing). In Connected Mode, you'll see the taint analysis issues SonarQube raised in your project. You'll get all of the context in your IDE that you need to triage and fix security problems and make sure the code you commit is safe.

**Smart Notifications**  
Connected mode sends smart alerts to individuals or teams when new issues are discovered. With everyone in the loop, issues can be addressed promptly, improving the overall software quality and delivery. You'll receive Smart Notifications in your IDE when:

* the [Quality Gate](/user-guide/quality-gates/) status of a project _open in your IDE_ changes
* a SonarQube analysis raises new issues _that you've introduced in a project open in your IDE_

You can activate or deactivate Smart Notifications in SonarLint on the IDE side on a server-by-server basis.

## Setting up Connected Mode
See the following links for instructions on setting up Connected Mode for each supported IDE:

* [Eclipse](https://github.com/SonarSource/sonarlint-eclipse/wiki/Connected-Mode)
* [IntelliJ IDEA](https://www.sonarlint.org/intellij/#intellij-connected-mode)
* [Visual Studio](https://github.com/SonarSource/sonarlint-visualstudio/wiki/Connected-Mode)
* [VS Code](https://marketplace.visualstudio.com/items?itemName=SonarSource.sonarlint-vscode#connected-mode)