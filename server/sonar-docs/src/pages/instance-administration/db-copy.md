---
title: SonarQube DB Copy Tool
url: /instance-administration/db-copy/
---

_The SonarQube DB Copy Tool is available to customers with [SonarSource support](https://www.sonarsource.com/support/)._

This tool is provided to help you migrate your SonarQube database from one DB vendor to another. If, for instance, you've been using your SonarQube instance with Oracle and you want to migrate to PostgreSQL without loosing your analysis history, the SonarQube DB Copy Tool is what you need. 

In the following lines we will talk about "source" and "target" SonarQube database instances. The source instance is the database you want to discard and the target is the one you want to move to.

The procedure is basically as follows:

* connect to both the source and target databases
* read the data from the source database table by table
* save the data into the target database table by table
* recreate the sequences, index, ... on the target database

## Installation
The SonarQube DB Copy Tool is provided as a standalone JAR file. **It must not be installed in your source or target SonarQube instances**. Put the JAR wherever your want on your machine, the only prerequisite is that this machine must be authorized to access your source and target SonarQube databases.

The version of the JAR to use must be at least **1.3.3.627**

## DB Copy Preparation Phase
In the preparation phase, you ready the target database by setting up SonarQube schema and populating it with the necessary tables so that you end up with the same  database schema in the source and the target.

1. Make sure your target database is up and running
1. On your target database, create the `sonar` schema. 
1. Download and expand a copy of SonarQube that exactly matches the version you're running. 
1. Configure your SonarQube copy to connect to the target database. (If you've placed your SonarQube copy on the same server that runs your primary SonarQube instance, you'll also need to configure non-default ports for your copy SonarQube instance.)
1. Start your copy SonarQube instance. It will connect to your empty target and populate the schema.
1. Once your copy instance is up and running (this indicates that the schema is fully populated), you can stop and delete it.
1. Refresh the Database Statistics on the target database before restarting SonarQube

At this point, you have in your source and target databases the exact same lists of tables.

## DB Copy Run Phase
There are only four steps in this phase:

1. **Stop your primary SonarQube instance.**
1. Execute the base command jar with the correct parameters. 
1. Update your primary SonarQube instance's configuration to point to the target DB
1. Restart your primary SonarQube instance.

### Base command
```
java -jar sonar-db-copy-1.3.3.627-jar-with-dependencies.jar
```

### Parameters
Name | Description | Required
---|---|---|---
`-help`|Print this parameters help| no  
`-urlSrc`|JDBC URL of the source database|yes
`-userSrc`|Username of the source database|yes
`-pwdSrc`|Password of the source database|yes
`-urlDest`|JDBC URL of the target database|yes
`-userDest`|Username of the target database|yes
`-pwdDest`|Password of the target database|yes
`-driverDest`|JDBC Driver of the target database|no
`-driverSrc`|JDBC Driver of the source database|no
`-T`|Comma separated list of tables to migrate|no

## Execution Examples
First sonar-db-copy verifies if URLs can be reached and the database versions:  
![verify urls](/images/db-copy/verify-urls.png)

When the versions are different, the application stops.  
![stop for different versions](/images/db-copy/verify-versions.png)

Sometime when you have restarted the copy, the destination database version is 0. This is not a problem, the copy will continue.  
![version 0 in target is okay](/images/db-copy/version0-ok.png)

Then it searches tables in source and destination database:  
![search tables](/images/db-copy/search-tables.png)

If there are missing tables, you will read this log:  
![missing table warning](/images/db-copy/missing-table-warning.png)

Second sonar-db-copy truncates tables in target database and indicates the number of tables purged:  
![truncate tables in target](/images/db-copy/truncate-tables.png)

Of course, the tables missing can not be purged:  
![missing tables aren't purged](/images/db-copy/missing-table-not-purged.png)

Third, sonar-db-copy reproduces data from source to destination and adjusts the sequence of destination database after the copy:  
![copy data](/images/db-copy/copy-data.png)

If there are some missing tables:  
![missing tables not copied](/images/db-copy/missing-table-not-copied.png)

If errors appear during the copy, the process does NOT stop but the errors are displayed:  
![copy errors displayed](/images/db-copy/copy-errors-shown.png)

At the end sonar-db-copy reiterates the difference between source and destination database. An error message is displayed if the databases are different. 
![final warning of remaining differences](/images/db-copy/summary-of-differences.png)
