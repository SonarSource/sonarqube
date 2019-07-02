---
title: Install the Server
url: /setup/install-server/
---


## Installing the Database

Several [database engines](/requirements/requirements/) are supported. Be sure to follow the requirements listed for your database, they are real requirements not recommendations.

Create an empty schema and a `sonarqube` user. Grant this `sonarqube` user permissions to `create`, `update`, and `delete` objects for this schema.

### Microsoft SQL Server

![](/images/exclamation.svg) Collation **MUST** be case-sensitive (CS) and accent-sensitive (AS).

![](/images/exclamation.svg) `READ_COMMITED_SNAPSHOT` **MUST** be set on the SonarQube database.

MS SQL database's shared lock strategy may impact SonarQube runtime. Making sure that `is_read_committed_snapshot_on` is set to `true` to prevent SonarQube from facing potential deadlocks under heavy loads. 

Example of query to check `is_read_committed_snapshot_on`:
```
SELECT is_read_committed_snapshot_on FROM sys.databases WHERE name='YourSonarQubeDatabase';
```
Example of query to update `is_read_committed_snapshot_on`:
```
ALTER DATABASE YourSonarQubeDatabase SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;
```
#### Integrated Security

To use integrated security: 

1. Download the [Microsoft SQL JDBC Driver 7.2.2 package](https://www.microsoft.com/en-us/download/details.aspx?id=57782) and copy the 64-bit version of `sqljdbc_auth.dll` to any folder in your path. 

2. **If you're running SonarQube as a Windows service,** make sure the Windows account under which the service is running has permission to connect your SQL server. The account should have `db_owner` database role membership. 

	**If you're running the SonarQube server from a command prompt,** the user under which the command prompt is running should have `db_owner` database role membership. 

3. Ensure that `sonar.jdbc.username` or `sonar.jdbc.password` properties are commented out or SonarQube will use SQL authentication.

```
sonar.jdbc.url=jdbc:sqlserver://localhost;databaseName=sonar;integratedSecurity=true
```

#### SQL Authentication

To use SQL Authentication, use the following connection string. Also ensure that `sonar.jdbc.username` and `sonar.jdbc.password` are set appropriately:

```
sonar.jdbc.url=jdbc:sqlserver://localhost;databaseName=sonar
sonar.jdbc.username=sonarqube
sonar.jdbc.password=mypassword
```

### Oracle

If there are two SonarQube schemas on the same Oracle instance, especially if they are for two different versions, SonarQube gets confused and picks the first it finds. To avoid this issue:

- Either privileges associated to the SonarQube Oracle user should be decreased
- Or a trigger should be defined on the Oracle side to automatically alter the SonarQube Oracle user session when establishing a new connection:

[[warning]]
| Oracle JDBC driver versions 12.1.0.1 and 12.1.0.2 have major bugs, and are not recommended for use with the SonarQube ([see more details](https://groups.google.com/forum/#!msg/sonarqube/Ahqt1iarqJg/u0BVRJZnBQAJ)).

### PostgreSQL

If you want to use a custom schema and not the default "public" one, the PostgreSQL `search_path` property must be set:

```
ALTER USER mySonarUser SET search_path to mySonarQubeSchema
```

## Installing the Web Server

First, check the [requirements](/requirements/requirements/). Then download and unzip the [distribution](http://www.sonarqube.org/downloads/) (do not unzip into a directory starting with a digit). 

SonarQube cannot be run as `root` on Unix-based systems, so create a dedicated user account to use for SonarQube if necessary.

_$SONARQUBE-HOME_ (below) refers to the path to the directory where the SonarQube distribution has been unzipped.

### Setting the Access to the Database

Edit _$SONARQUBE-HOME/conf/sonar.properties_ to configure the database settings. Templates are available for every supported database. Just uncomment and configure the template you need and comment out the lines dedicated to H2:

```
Example for PostgreSQL
sonar.jdbc.username=sonarqube
sonar.jdbc.password=mypassword
sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
```

### Adding the JDBC Driver

Drivers for the supported databases (except Oracle) are already provided. Do not replace the provided drivers; they are the only ones supported.

For Oracle, copy the JDBC driver into _$SONARQUBE-HOME/extensions/jdbc-driver/oracle_.

### Configuring the Elasticsearch storage path

By default, Elasticsearch data is stored in _$SONARQUBE-HOME/data_, but this is not recommended for production instances. Instead, you should store this data elsewhere, ideally in a dedicated volume with fast I/O. Beyond maintaining acceptable performance, doing so will also ease the upgrade of SonarQube.

Edit _$SONARQUBE-HOME/conf/sonar.properties_ to configure the following settings:

```
sonar.path.data=/var/sonarqube/data
sonar.path.temp=/var/sonarqube/temp
```

The user used to launch SonarQube must have read and write access to those directories.

### Starting the Web Server

The default port is "9000" and the context path is "/". These values can be changed in _$SONARQUBE-HOME/conf/sonar.properties_:

```
sonar.web.host=192.0.0.1
sonar.web.port=80
sonar.web.context=/sonarqube
```

Execute the following script to start the server:

- On Linux/Mac OS: bin/<YOUR OS>/sonar.sh start
- On Windows: bin/windows-x86-XX/StartSonar.bat

You can now browse SonarQube at _http://localhost:9000_ (the default System administrator credentials are `admin`/`admin`).

### Tuning the Web Server

By default, SonarQube is configured to run on any computer with a simple Java JRE.

For better performance, the first thing to do when installing a production instance is to use a Java JDK and activate the server mode by uncommenting/setting the following line in _$SONARQUBE-HOME/conf/sonar.properties_:

```
sonar.web.javaOpts=-server
```

To change the Java JVM used by SonarQube, simply edit _$SONARQUBE-HOME/conf/wrapper.conf_ and update the following line:

```
wrapper.java.command=/path/to/my/jdk/bin/java
```

### Advanced Installation Features

- Running SonarQube as a Service on [Windows](/setup/operate-server/) or [Linux](/setup/operate-server/)
- Running SonarQube [behind a Proxy](/setup/operate-server/)
- Running SonarQube Community Edition with [Docker](https://hub.docker.com/_/sonarqube/)

## Next Steps

Once your server is installed and running, you may also want to [Install Plugins](/setup/install-plugin/). Then you're ready to begin [Analyzing Source Code](/analysis/overview/).

## Troubleshooting/FAQ

### Grant more memory to the web server / compute engine / elastic search

To grant more memory to a server-side process, uncomment and edit the relevant javaOpts property in `$SONARQUBE_HOME/conf/sonar.properties`, specifically:

- `sonar.web.javaOpts` (minimum values: `-server -Xmx768m`)
- `sonar.ce.javaOpts`
- `sonar.search.javaOpts`

### Failed to start on Windows Vista

SonarQube seems unable to start when installed under the `Program Files` directory on Windows Vista. It should therefore not be installed there.

### Failed to start SonarQube with Oracle due to bad `USERS` table structure

When other `USERS` tables exist in the Oracle DB, if the `sonarqube` user has read access on this other `USERS` table, the SonarQube web server can't start and an exception like the following one is thrown:

```
ActiveRecord::ActiveRecordError: ORA-00904: "TOTO": invalid identifier
: INSERT INTO users (login, name, email, crypted_password, salt, 
created_at, updated_at, remember_token, remember_token_expires_at, toto, id)
VALUES('admin', 'Administrator', '', 'bba4c8a0f808f9798cf8b1c153a4bb4f9178cf59', '2519754f77ea67e5d7211cd1414698f465aacebb',
TIMESTAMP'2011-06-24 22:09:14', TIMESTAMP'2011-06-24 22:09:14', null, null, null, ?)
ActiveRecord::ActiveRecordError: ORA-00904: "TOTO": invalid identifier
 
: INSERT INTO users (login, name, email, crypted_password, salt, 
created_at, updated_at, remember_token, remember_token_expires_at, toto, id)
VALUES('admin', 'Administrator', '', 'bba4c8a0f808f9798cf8b1c153a4bb4f9178cf59', 
'2519754f77ea67e5d7211cd1414698f465aacebb', TIMESTAMP'2011-06-24 22:09:14', TIMESTAMP'2011-06-24 22:09:14', null, null, null, ?)
```

To fix this issue, the rights of the `sonarqube` Oracle user must be decreased to remove read access on the other `USERS` table(s).

### Failed to connect to the Marketplace via proxy

Double check that settings for proxy are correctly set in `$SONARQUBE_HOME/conf/sonar.properties`.
Note that if your proxy username contains "\" (backslash), then it should be escaped - for example username "domain\user" in file should look like:

```
http.proxyUser=domain\\user
```

For some proxies, the exception "java.net.ProtocolException: Server redirected too many times" might mean an incorrect username or password has been configured.

### Exception java.lang.RuntimeException: can not run elasticsearch as root

SonarQube starts an Elasticsearch process, and the same account that is running SonarQube itself will be used for the Elasticsearch process. Since Elasticsearch cannot be run as `root`, that means SonarQube can't be either. You must choose some other, non-`root` account with which to run SonarQube, preferably an account dedicated to the purpose.
