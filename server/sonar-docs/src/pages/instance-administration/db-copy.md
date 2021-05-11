---
title: SonarQube DB Copy Tool
url: /instance-administration/db-copy/
---

_The SonarQube DB Copy Tool is available to customers with [SonarSource support](https://www.sonarsource.com/support/)._

We provide this tool to help you migrate your SonarQube database from one database vendor to another. For example, if you've been using your SonarQube instance with Oracle and want to migrate to PostgreSQL without losing your analysis history, the SonarQube DB Copy Tool can help. 

In the following lines, we mention "source" and "target" SonarQube database instances. The source instance is the database you're moving from, and the target instance is the one you're moving to.

Here's an overview of the general procedure:

* Connect to both the source and target databases.
* Read the data from the source database table by table.
* Save the data into the target database table by table.
* Recreate the sequences, index, ... on the target database.

## Installation
The SonarQube DB Copy Tool is provided as a standalone JAR file. You need to make sure you're meeting the following conditions:

* The JAR file must not be installed in your source or target SonarQube instances. You can put the JAR file anywhere on your machine as long as your machine is authorized to access your source and target SonarQube databases.
* You must be using at least version **1.3.3.627** of the JAR file.

## DB Copy preparation phase
To prepare for the DB Copy Tool, you need to ready the target instance by setting up a SonarQube schema and populating it with the necessary tables so that your source and target instances have the same database schema.

1. Make sure your target database is up and running.
1. On your target database, create the `sonar` schema. 
1. Download and expand a copy of SonarQube that exactly matches the version you're running. 
1. Configure your SonarQube copy to connect to the target database. (If you've placed your SonarQube copy on the same server that runs your primary SonarQube instance, you'll also need to configure non-default ports for your copy SonarQube instance.)
1. Start your copy SonarQube instance. It will connect to your empty target and populate the schema.
1. Once your copy instance is up and running (this indicates that the schema is fully populated), you can stop and delete it.
1. Refresh the Database Statistics on the target database before restarting SonarQube

At this point, you have the exact same list of tables in your source and target databases.

## DB Copy run phase
To run the DB Copy Tool, perform the following steps:

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
First, sonar-db-copy verifies if URLs can be reached and the database versions:
  
```
***************** CONFIGURATION VERIFICATIONS ***************
Database SOURCE has been reached at :          jdbc:h2:tcp://localhost:9092/sonar-db-copy
Database DESTINATION has been reached at :     mysql://localhost:13306/sonar?autoReconnect=true
The version of SonarQube schema migration are the same between source (433) and destination (433).
```

When the versions are different, the application stops.  

```
***************** CONFIGURATION VERIFICATIONS ***************
Database SOURCE has been reached at :          jdbc:h2:tcp://localhost:9092/sonar-db-copy
Database DESTINATION has been reached at :     mysql://localhost:13306/sonar?autoReconnect=true
Exception in thread "main" Version of the schema migration are not the same between source (433) and destination (494).
```

Sometimes when you have restarted the copy, the destination database version is 0. This is not a problem, and the copy will continue.  

```
***************** CONFIGURATION VERIFICATIONS ***************
Database SOURCE has been reached at :          jdbc:h2:tcp://localhost:9092/sonar-db-copy
Database DESTINATION has been reached at :     mysql://localhost:13306/sonar?autoReconnect=true
!  WARNING – The versions of SonarQube schema migration source is (433) when destination is (0).
```

Then it searches tables in the source and destination databases:  

```
*************** SEARCH TABLES ***************
START GETTING METADATA IN SOURCE...
  53 TABLES GETTED.
START GETTING METADATA IN DESTINATION...
  53 TABLES GETTED.
*************** FOUND TABLES ***************

FOUND TABLE : action_plans
  SOURCE:
        COLUMNS : (id,kee,name,description,deadline,user_login,project_id,status,created_at,updated_at)
		TYPES:  : (INTEGER,VARCHAR,VARCHAR,VARCHAR,TIMESTAMP,VARCHAR,INTEGER,VARCHAR,TIMESTAMP,TIMESTAMP)
  DESTINATION:
		COLUMNS : (id,create_at,updated_at,name,description,deadline,user_login,project_id,status,kee)
		TYPES   : (BIGINT,TIMESTAMP,TIMESTAMP,VARCHAR,VARCHAR,TIMESTAMP,VARCHAR,INTEGER,VARCHAR,VARCHAR)
		
FOUND TABLE : active_dashboards
	SOURCE:
		COLUMNS : (id,dashboard_id,user_id,order_index)
		TYPES   : (INTEGER,INTEGER,INTEGER,INTEGER)
	DESTINATION:
		COLUMNS : (id,dashboard_id,user_id,order_index)
		TYPES   : (INTEGER,INTEGER,INTEGER,INTEGER)
```
		
If there are missing tables, you will see this log:  

```
FOUND TABLE : action_plans
  SOURCE:
        COLUMNS : (id,person_id,login,create_at,updated_at)
		TYPES:  : (INTEGER,INTEGER,VARCHAR,TIMESTAMP,TIMESTAMP)
  DESTINATION:
 ! WARNING - TABLE authors is not present in the DESTINATION database.
``` 

Then, sonar-db-copy truncates tables in the target database and indicates the number of tables purged. Of course, the tables missing can not be purged:  

```
*************** DELETE TABLES FROM DESTINATION ***************
START DELETING...
 ! WARNING - Can't DELETE TABLE :authors because it doesn't exist in the destination database.
   52 TABLES DELETED IN DESTINATION.
```

Next, sonar-db-copy reproduces data from source to destination and adjusts the sequence of destination database after the copy:  

```
*************** COPY DATA ***************
action_plans                 0 / 0
action_plans                 0 / 0
active_dashboards            0 / 5
active_dashboards            5 / 5
active_rules                 0 / 629
active_rules                 629 / 629
active_rule_changes          0 / 0
active_rule_changes          0 / 0
active_rule_notes            0 / 0
active_rule_notes            0 / 0
active_rule_parameters       0 / 58
active_rule_parameters       58 / 58
```

If there are some missing tables, you'll see the following warning:  

```
! WARNING - Can't WRITE in TABLE :authors because it doesn't exist in destination database.
```

If errors appear during the copy, the process does NOT stop but the errors are displayed:  

```
** ERROR ** IN TABLE: users when read and write at col: 10 and id=1.
** ERROR ** SORUCE COLUMNS      ( name,admin,remarks,id,id,login,name).
** ERROR ** DESTINATION COLUMNS ( id,login,name,email,crypted_password).
** ERROR ** LINES NOT COPIED at ROW (0) WITH id = (1).
** ERROR ** Cannot parse "TIMESTAMP" constant "TRUE" [22007-172]
```

At the end, sonar-db-copy reiterates the difference between the source and destination databases. An error message is displayed if the databases are different. 

```
INFO *************** CHECK DESCREPANCIES ***************
WARN TABLE projects has 65 ROWS in SOURCE while 0 ROWS in DESTINATION
INFO ********************************************
INFO ** THE COPY HAS FINISHED UNSUCCESSFULLY !!! WATCH OUT THE LOG!!! **
INFO ********************************************
```
