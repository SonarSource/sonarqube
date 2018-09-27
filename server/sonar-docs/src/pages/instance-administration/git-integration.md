---
title: Git Integration
url: /instance-administration/git-integration/
---

[Git](http://www.git-scm.com/) integration is supported out of the box with a pure Java implementation so there's no need to have Git command line tool installed on the machine where analysis is performed.

Auto-detection of Git during analysis will happen if there is a .git folder in the project root directory or in one of its parent folders. Otherwise you can force the provider using `-Dsonar.scm.provider=git`. A full clone is required for this integration to be able to collect the required blame information (see Known Issues). If a shallow clone is detected, a warning will be logged and no attempt will be made to retrieve blame information..

### Known Issues

* Git doesn't consider old "Mac" line ends (CR) as new lines. As a result the blame operation will contain fewer lines than expected by SonarQube and analysis will fail. The solution is to fix line ends to use either Windows (CR/LF) or Unix (LF) line ends.
* JGit doesn't support .mailmap file to "clean" email adress during the blame

### Advanced information
The plugin uses [JGit](https://www.eclipse.org/jgit/) 4.9.0. JGit is a pure Java implementation of Git client.

### How to investigate error during blame (only possible on Unix/Linux)?

If you get an error when blame is executed on a file, it may be a limitation or a bug in JGit. To confirm please follow these steps:

1. Download the standalone JGit command line distribution

2. Try to execute the blame command on the offending file:  
    `chmod +x /path/to/org.eclipse.jgit.pgm-4.9.0.201710071750-r.sh /path/to/org.eclipse.jgit.pgm-4.9.0.201710071750-r.sh blame -w /path/to/offending/file`

3. If you get the same error as during analysis, then this really looks like a bug in JGit (especially if you don't have an issue with the native git command line tool). Please try to do previous steps with latest version of JGit and report all information to the [SonarQube Community Forum](https://community.sonarsource.com/).
