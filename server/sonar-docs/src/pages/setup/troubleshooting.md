---
title: Troubleshooting
url: /setup/troubleshooting/
---

<!-- sonarqube -->

## Checking the logs

If you're having trouble starting your server for the first time (or any subsequent time!) the first thing to do is check your server logs. You'll find them in `$SONARQUBE_HOME/logs`:

* sonar.log - Log for the main process. Holds general information about startup and shutdown. You'll get overall status here but not details. Look to the other logs for that.
* web.log - Information about initial connection to the database, database migration and reindexing, and the processing of HTTP requests. This includes database and search engine logs related to those requests.
* ce.log - Information about background task processing and the database and search engine logs related to those tasks.
* es.log - Ops information from the search engine, such as Elasticsearch startup, health status changes, cluster-, node- and index-level operations, etc.

## Understanding the logs

When there's an error, you'll very often find a stacktrace in the logs. If you're not familiar stacktraces, they can be intimidatingly tall walls of incomprehensible text. As a sample, here's a fairly short one:

```
java.lang.IllegalStateException: Unable to blame file **/**/foo.java
    at org.sonarsource.scm.git.JGitBlameCommand.blame(JGitBlameCommand.java:128)
    at org.sonarsource.scm.git.JGitBlameCommand.access$000(JGitBlameCommand.java:44)
    at org.sonarsource.scm.git.JGitBlameCommand$1.call(JGitBlameCommand.java:112)
    at org.sonarsource.scm.git.JGitBlameCommand$1.call(JGitBlameCommand.java:109)
    at java.util.concurrent.FutureTask.run(Unknown Source)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(Unknown Source)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(Unknown Source)
    at java.lang.Thread.run(Unknown Source)
Caused by: java.lang.NullPointerException
    at org.eclipse.jgit.treewalk.filter.PathFilter.create(PathFilter.java:77)
    at org.eclipse.jgit.blame.BlameGenerator.<init>(BlameGenerator.java:161)
    at org.eclipse.jgit.api.BlameCommand.call(BlameCommand.java:203)
    at org.sonarsource.scm.git.JGitBlameCommand.blame(JGitBlameCommand.java:126)
    ... 7 more
```

Unless you wrote the code that produced this error, you really only care about:
* the first line, which ought to have a human-readable message after the colon. In this case, it's Unable to blame file `**/**/foo.java`
* and any line that starts with `Caused by:`. There are often several `Caused by` lines, and indentation makes them easy to find as you scroll through the error. Be sure to read each of these lines. Very often one of them - the last one or next to last one - contains the real problem.

<!-- /sonarqube -->
