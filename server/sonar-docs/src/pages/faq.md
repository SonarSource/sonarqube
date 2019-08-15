---
title: Frequently Asked Questions
url: /faq/
---

## How do I get rid of issues that are False-Positives?
**False-Positive and Won't Fix**  
You can mark individual issues False Positive or Won't Fix through the issues interface. If you're using Short-lived branch and PR analysis provided by the Developer Edition, issues marked False Positive or Won't Fix will retain that status after merge. This is the preferred approach.

**//NOSONAR**  
Most language analyzers support the use of the generic mechanism: `//NOSONAR` at the end of the line of the issue. This will suppress the all issues - now and in the future - that might be raised on the line.

## How do I find and remove projects that haven't been analyzed in a while?
In **[Administration > Projects > Management](/#sonarqube-admin#/admin/projects_management)** you can search for **Last analysis before** to filter projects not analyzed since a specific date, and then use bulk **Delete** to remove the projects that match your filter.

This can be automated by using the corresponding Web API: `api/projects/bulk_delete?analyzedBefore=YYYY-MM-DD`.

<!-- sonarqube -->
## How do I trigger a full ElasticSearch reindex?
Currently, the only way to force a reindex is to:

* Stop your server
* Remove the contents of the $SQ_HOME/data/es6 directory
* Start your server

Before doing this, you should be aware first that processes are in place on the SonarQube side that out-of-sync indices are detected and corrected, and second that a full re-index can be quite lengthy depending on the size of your instance.

## Why can't I use my HTTP Proxy since I upgraded to Java8u111?

If you are getting this error in the logs when trying to use the Marketplace:
```
java.io.IOException: Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required
```
... you probably upgraded your Java8 installation with an update greater than 111. To fix that, update _$SONARQUBE_HOME/conf/sonar.properties` like this:
```
sonar.web.javaOpts=-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError -Djdk.http.auth.tunneling.disabledSchemes=""
```
Reference: http://www.oracle.com/technetwork/java/javase/8u111-relnotes-3124969.html
<!-- /sonarqube -->

