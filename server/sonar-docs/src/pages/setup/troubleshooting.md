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

## Recovering from Elasticsearch read-only indices

You may encounter issues with Elasticsearch (ES) indices becoming locked in read-only mode. ES requires free disk space available and implements a safety mechanism to prevent the disk from being flooded with index data that:

* **For non-DCE** –  locks all indices in read-only mode when the 95% used disk usage watermark is reached.  
* **For DCE** – locks all or some indices in read-only mode when one or more node reaches the 95% used disk usage watermark.

ES shows warnings in the logs as soon as disk usage reaches 85% and 90%. At 95% usage and above, indices turning read-only causes errors in the web and compute engine.

Freeing disk space will *not* automatically make the indices return to read-write. To make indices read-write, you also need to:

* **For non-DCE** – restart SonarQube.
* **For DCE** – restart *ALL* application nodes (the first application node restarted after all have been stopped will make the indices read-write).  

SonarQube's built-in resilience mechanism allows SonarQube to eventually recover from the indices being behind data in the DB (this process can take a while).

If you still have inconsistencies, you'll need to rebuild the indices (this operation can take a long time depending on the number of issues and components):

**non-DCE:**  

1. Stop SonarQube  
1. Delete the data/es6 directory  
1. Restart SonarQube  

**DCE:**  

1. Stop the whole cluster (ES and application nodes)  
1. Delete the data/es6 directory on each ES node  
1. Restart the whole cluster  
    
**Note:** See [Configure & Operate a Cluster](/setup/operate-cluster/) for information on stopping and starting a cluster.

<!-- /sonarqube -->
