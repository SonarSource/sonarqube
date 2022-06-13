---
title: Install the Server
url: /setup/install-server/
---

## Overview

This section describes a single-node SonarQube instance. For details on clustered setup, see [Install the Server as a Cluster](/setup/install-cluster/).

### Instance components

A SonarQube instance comprises three components:

![SonarQube Instance Components](/images/SQ-instance-components.png)

1. The SonarQube server running the following processes:
	- a web server that serves the SonarQube user interface.
	- a search server based on Elasticsearch.
	- the compute engine in charge of processing code analysis reports and saving them in the SonarQube database.

2. The database to store the following:
	- Metrics and issues for code quality and security generated during code scans.
	- The SonarQube instance configuration.

3. One or more scanners running on your build or continuous integration servers to analyze projects.

### Hosts and locations

For optimal performance, the SonarQube server and database should be installed on separate hosts, and the server host should be dedicated. The server and database hosts should be located in the same network.

All hosts must be time-synchronized.

## Installing the database

Several [database engines](/requirements/requirements/) are supported. Be sure to follow the requirements listed for your database. They are real requirements not recommendations.

Create an empty schema and a `sonarqube` user. Grant this `sonarqube` user permissions to `create`, `update`, and `delete` objects for this schema.

[[collapse]]
| ## Microsoft SQL Server
|
|[[warning]]
|| Collation **MUST** be case-sensitive (CS) and accent-sensitive (AS).  
|| `READ_COMMITED_SNAPSHOT` **MUST** be set on the SonarQube database.
|
|MS SQL database's shared lock strategy may impact SonarQube runtime. Making sure that `is_read_committed_snapshot_on` is set to `true` to prevent SonarQube from facing potential deadlocks under heavy loads. 
|
|Example of query to check `is_read_committed_snapshot_on`:
|```
|SELECT is_read_committed_snapshot_on FROM sys.databases WHERE name='YourSonarQubeDatabase';
|```
|Example of query to update `is_read_committed_snapshot_on`:
|```
|ALTER DATABASE YourSonarQubeDatabase SET READ_COMMITTED_SNAPSHOT ON WITH ROLLBACK IMMEDIATE;
|```
|### Integrated Security
|
|To use integrated security: 
|
|1. Download the [Microsoft SQL JDBC Auth 10.2.1 package](https://github.com/microsoft/mssql-jdbc/releases/download/v10.2.1/mssql-jdbc_auth.zip) and copy `mssql-jdbc_auth-10.2.1.x64.dll` to any folder in your path. 
|
|2. **If you're running SonarQube as a Windows service,** make sure the Windows account under which the service is running has permission to connect your SQL server. The account should have `db_owner` database role membership. 
|
|	**If you're running the SonarQube server from a command prompt,** the user under which the command prompt is running should have `db_owner` database role membership. 
|
|3. Ensure that `sonar.jdbc.username` or `sonar.jdbc.password` properties are commented out or SonarQube will use SQL authentication.
|
|```
|sonar.jdbc.url=jdbc:sqlserver://localhost;databaseName=sonar;integratedSecurity=true
|```
|
|### SQL Authentication
|
|To use SQL Authentication, use the following connection string. Also ensure that `sonar.jdbc.username` and `sonar.jdbc.password` are set appropriately:
|
|```
|sonar.jdbc.url=jdbc:sqlserver://localhost;databaseName=sonar
|sonar.jdbc.username=sonarqube
|sonar.jdbc.password=mypassword
|```

[[collapse]]
| ## Oracle
|
|If there are two SonarQube schemas on the same Oracle instance, especially if they are for two different versions, SonarQube gets confused and picks the first it finds. To avoid this issue:
|
|- Either privileges associated to the SonarQube Oracle user should be decreased
|- Or a trigger should be defined on the Oracle side to automatically alter the SonarQube Oracle user session when establishing a new connection:
|
|[[warning]]
|| Oracle JDBC driver versions 12.1.0.1 and 12.1.0.2 have major bugs, and are not recommended for use with the SonarQube ([see more details](https://groups.google.com/forum/#!msg/sonarqube/Ahqt1iarqJg/u0BVRJZnBQAJ)).

[[collapse]]
| ## PostgreSQL
|
|If you want to use a custom schema and not the default "public" one, the PostgreSQL `search_path` property must be set:
|
|```
|ALTER USER mySonarUser SET search_path to mySonarQubeSchema
|```

## Installing SonarQube from the ZIP file

First, check the [requirements](/requirements/requirements/). Then download and unzip the [distribution](http://www.sonarqube.org/downloads/) (do not unzip into a directory starting with a digit). 

SonarQube cannot be run as `root` on Unix-based systems, so create a dedicated user account for SonarQube if necessary.

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
sonar.web.host=192.168.0.1
sonar.web.port=80
sonar.web.context=/sonarqube
```

Execute the following script to start the server:

- On Linux: bin/linux-x86-64/sonar.sh start
- On macOS: bin/macosx-universal-64/sonar.sh start
- On Windows: bin/windows-x86-64/StartSonar.bat

You can now browse SonarQube at _http://localhost:9000_ (the default System administrator credentials are `admin`/`admin`).

### Adjusting the Java Installation

If there are multiple versions of Java installed on your server, you may need to explicitly define which version of Java is used.

To change the Java JVM used by SonarQube, edit _$SONARQUBE-HOME/conf/wrapper.conf_ and update the following line:

```
wrapper.java.command=/path/to/my/jdk/bin/java
```

### Advanced Installation Features

- Running SonarQube as a Service on [Windows](/setup/operate-server/) or [Linux](/setup/operate-server/)
- Running SonarQube [behind a Proxy](/setup/operate-server/)
- Monitoring and adjusting [Java Process Memory](/instance-administration/monitoring/)

## Installing SonarQube from the Docker Image

Follow these steps for your first installation:

1.	Creating the following volumes helps prevent the loss of information when updating to a new version or upgrading to a higher edition:
	- `sonarqube_data` – contains data files, such as the embedded H2 database and Elasticsearch indexes
	- `sonarqube_logs` – contains SonarQube logs about access, web process, CE process, and Elasticsearch
	- `sonarqube_extensions` – will contain any plugins you install and the Oracle JDBC driver if necessary.
	
	Create the volumes with the following commands:
	```bash
	$> docker volume create --name sonarqube_data
	$> docker volume create --name sonarqube_logs
	$> docker volume create --name sonarqube_extensions
	``` 
	[[warning]]
    | Make sure you're using [volumes](https://docs.docker.com/storage/volumes/) as shown with the above commands, and not [bind mounts](https://docs.docker.com/storage/bind-mounts/). Using bind mounts prevents plugins from populating correctly.

2. Drivers for supported databases (except Oracle) are already provided. If you're using an Oracle database, you need to add the JDBC driver to the `sonar_extensions` volume. To do this:

	a. Start the SonarQube container with the embedded H2 database:
   
    ```
	$ docker run --rm \
		-p 9000:9000 \
		-v sonarqube_extensions:/opt/sonarqube/extensions \
		<image_name>
	```
	
	b. Exit once SonarQube has started properly. 
   
	c. Copy the Oracle JDBC driver into `sonarqube_extensions/jdbc-driver/oracle`.
   
3. Run the image with your database properties defined using the -e environment variable flag:

	```bash
	$> docker run -d --name sonarqube \
		-p 9000:9000 \
		-e SONAR_JDBC_URL=... \
		-e SONAR_JDBC_USERNAME=... \
		-e SONAR_JDBC_PASSWORD=... \
		-v sonarqube_data:/opt/sonarqube/data \
		-v sonarqube_extensions:/opt/sonarqube/extensions \
		-v sonarqube_logs:/opt/sonarqube/logs \
		<image_name>
	```
	
	For docker based setups, environment variables supersede all parameters that were provided with properties. See  [Docker Environment Variables](/setup/environment-variables/).
	
	[[warning]]
    | Use of the environment variables `SONARQUBE_JDBC_USERNAME`, `SONARQUBE_JDBC_PASSWORD`, and `SONARQUBE_JDBC_URL` is deprecated and will stop working in future releases.

####**Example Docker Compose configuration**
If you're using [Docker Compose](https://docs.docker.com/compose/), use the following example as a reference when configuring your `.yml` file. Click the heading below to expand the `.yml` file.

[[info]]
| The example below will use the latest version of the SonarQube Docker image. If want to use the LTS version of SonarQube, you need to update the example with the `sonarqube:lts-community` image tag.

[[collapse]]
| ## Docker Compose .yml file example
|
| ```
| version: "3"
| 
| services:
|   sonarqube:
|     image: sonarqube:community
|     depends_on:
|       - db
|     environment:
|       SONAR_JDBC_URL: jdbc:postgresql://db:5432/sonar
|       SONAR_JDBC_USERNAME: sonar
|       SONAR_JDBC_PASSWORD: sonar
|     volumes:
|       - sonarqube_data:/opt/sonarqube/data
|       - sonarqube_extensions:/opt/sonarqube/extensions
|       - sonarqube_logs:/opt/sonarqube/logs
|     ports:
|       - "9000:9000"
|   db:
|     image: postgres:12
|     environment:
|       POSTGRES_USER: sonar
|       POSTGRES_PASSWORD: sonar
|     volumes:
|       - postgresql:/var/lib/postgresql
|       - postgresql_data:/var/lib/postgresql/data
| 
| volumes:
|   sonarqube_data:
|   sonarqube_extensions:
|   sonarqube_logs:
|   postgresql:
|   postgresql_data:
| ```

## Next Steps

Once your server is installed and running, you may also want to [Install Plugins](/setup/install-plugin/). Then you're ready to begin [Analyzing Source Code](/analysis/overview/).

## Troubleshooting/FAQ

### Failed to connect to the Marketplace via proxy

Double check that settings for proxy are correctly set in `$SONARQUBE_HOME/conf/sonar.properties`.
Note that if your proxy username contains a backslash, then it should be escaped - for example username "domain\user" in file should look like:

```
http.proxyUser=domain\\user
```

For some proxies, the exception "java.net.ProtocolException: Server redirected too many times" might mean an incorrect username or password has been configured.

### Exception java.lang.RuntimeException: can not run elasticsearch as root

SonarQube starts an Elasticsearch process, and the same account that is running SonarQube itself will be used for the Elasticsearch process. Since Elasticsearch cannot be run as `root`, that means SonarQube can't be either. You must choose some other, non-`root` account with which to run SonarQube, preferably an account dedicated to the purpose.

### Sonarqube DNS cache

When reporting Quality Gate status to DevOps platforms, SonarQube uses a DNS cache time to live policy of 30 seconds. If necessary, you can change this setting in your JVM:

```bash
echo "networkaddress.cache.ttl=5" >> "${JAVA_HOME}/conf/security/java.security" 
```

Please be aware that low values increases the risk of DNS spoofing attacks.

### Self Signed Certificates of DevOps platforms

When running in an environment where the DevOps platform or other related tooling is secured by self signed certificates, the CA needs to be added to the java truststore of SonarQube.

On a zip installation the systems truststore can be found in `$JAVA_HOME/lib/security/cacerts`. In order to add a new certificate to the truststore you can use the following command as an example:

```bash
keytool -importcert -file $PATH_TO_CERTIFICATE -alias $CERTIFICATE_NAME -keystore /$JAVA_HOME/lib/security/cacerts -storepass changeit -trustcacerts -noprompt
```

In our official Docker images you can find the systems truststore in `$JAVA_HOME/lib/security/cacerts`. In order to add new certificates here as well you can:

* bind mount an existing truststore containing your certificates to `$JAVA_HOME/lib/security/cacerts`

[[collapse]]
| example: 
| 
| ```bash
| docker run -d --name sonarqube -v /path/to/your/cacerts.truststore:/usr/lib/jvm/java-11-openjdk/lib/security/cacerts:ro -p 9000:9000 sonarqube 
| ```

* import your CA certificate the same way as in the zip installation but inside the container.

If you deploy SonarQube on Kubernetes using the official Helm Chart, you can create a new secret containing your required certificates and reference this via:

```yaml
caCerts:
  enabled: true
  image: adoptopenjdk/openjdk11:alpine
  secret: your-secret
```
