---
title: Environment Variables
url: /setup/environment-variables/
---

This page provides environment variables used for configuring SonarQube with Docker. The values provided in the following environment variables are the default values.

## Database

[[info]]
|- The embedded H2 database is used by default. It is recommended for tests but not for production use. Supported databases are Oracle, PostgreSQL, and Microsoft SQLServer.
|- Changes to the database connection URL (sonar.jdbc.url) can affect SonarSource licensed products.

### User Credentials

**`SONAR_JDBC_USERNAME=`**  
**`SONAR_JDBC_PASSWORD=`**  
Permissions to create tables, indices, and triggers must be granted to JDBC user. The schema must be created first.  

### Embedded Database (default)

**`SONAR_EMBEDDEDDATABASE_PORT=9092`**  
H2 embedded database server listening port, defaults to 9092.  

### Oracle 12c/18c/19c

**`SONAR_JDBC_URL=jdbc:oracle:thin:@localhost:1521/XE`**  
The Oracle JDBC driver must be copied into the directory extensions/jdbc-driver/oracle/. Only the thin client is supported, and we recommend using the latest Oracle JDBC driver. See https://jira.sonarsource.com/browse/SONAR-9758 for more details. If you need to set the schema, please refer to http://jira.sonarsource.com/browse/SONAR-5000.

### PostgreSQL 9.6 or greater

**`SONAR_JDBC_URL=jdbc:postgresql://localhost/sonarqube?currentSchema=my_schema`**  
By default the schema named "public" is used. It can be overridden with the parameter "currentSchema".

### Microsoft SQLServer 2014/2016/2017/2019 and SQL Azure

**`SONAR_JDBC_URL=jdbc:sqlserver://localhost;databaseName=sonar;integratedSecurity=true`**  
A database named sonar must exist and its collation must be case-sensitive (CS) and accent-sensitive (AS). Use this connection string if you want to use integrated security with Microsoft Sql Server. Do not set the `SONAR_JDBC_USERNAME` or `SONAR_JDBC_PASSWORD` property if you are using Integrated Security. 

For Integrated Security to work, you have to download the Microsoft SQL JDBC Auth 10.2.1 package [here](https://github.com/microsoft/mssql-jdbc/releases/download/v10.2.1/mssql-jdbc_auth.zip) and copy `mssql-jdbc_auth-10.2.1.x64.dll` to your path.

**`SONAR_JDBC_URL=jdbc:sqlserver://localhost;databaseName=sonar`**  
Use this connection string if you want to use SQL Auth while connecting to MS Sql Server. Set the `SONAR_JDBC_USERNAME` and `SONAR_JDBC_PASSWORD` appropriately.

### Connection pool settings

**`SONAR_JDBC_MAXACTIVE=60`**   
The maximum number of active connections that can be allocated at the same time, or negative for no limit. The recommended value is 1.2 * max sizes of HTTP pools. For example, if HTTP ports are enabled with default sizes (50, see property `sonar.web.http.maxThreads`) then `SONAR_JDBC_MAXACTIVE` should be 1.2 * 50 = 60.

**`SONAR_JDBC_MAXIDLE=5`**  
The maximum number of connections that can remain idle in the pool, without extra ones being released, or negative for no limit.

**`SONAR_JDBC_MINIDLE=2`**  
The minimum number of connections that can remain idle in the pool, without extra ones being created, or zero to create none.

**`SONAR_JDBC_MAXWAIT=5000`**  
The maximum number of milliseconds that the pool will wait (when there are no available connections) for a connection to be returned before throwing an exception, or <= 0 to wait indefinitely.

**`SONAR_JDBC_MINEVICTABLEIDLETIMEMILLIS=600000`**  
**`SONAR_JDBC_TIMEBETWEENEVICTIONRUNSMILLIS=30000`**

## Web Server

**`SONAR_WEB_JAVAOPTS=`**  
the web server is executed in a dedicated Java process. Use this property to customize JVM options.

[[info]]
| The HotSpot Server VM is recommended. The property -server should be added if server mode
| is not enabled by default on your environment. See [here](http://docs.oracle.com/javase/8/docs/technotes/guides/vm/server-class.html).
|
| Startup can be long if the entropy source is short of entropy. Adding
| -Djava.security.egd=file:/dev/./urandom is an option to resolve the problem. See [Here](https://cwiki.apache.org/confluence/display/TOMCAT/HowTo+FasterStartUp#HowToFasterStartUp-EntropySource)

**`SONAR_WEB_JAVAADDITIONALOPTS=`**  
Same as previous property, but allows to not repeat all other settings like -Xmx

**`SONAR_WEB_HOST=0.0.0.0`**  
Binding IP address. For servers with more than one IP address, this property specifies which address will be used for listening on the specified ports. By default, ports will be used on all IP addresses associated with the server.

**`SONAR_WEB_CONTEXT=`**
Web context. When set, it must start with a forward slash (for example /sonarqube).
The default value is root context (empty value).

**`SONAR_WEB_PORT=9000`**  
TCP port for incoming HTTP connections. Default value is 9000.

**`SONAR_WEB_HTTP_MAXTHREADS=50`**  
The maximum number of connections that the server will accept and process at any given time. When this number has been reached, the server will not accept any more connections until the number of connections falls below this value. The operating system may still accept connections based on the `SONAR_WEB_CONNECTIONS_ACCEPTCOUNT` property. The default value is 50.

**`SONAR_WEB_HTTP_MINTHREADS=5`**  
The minimum number of threads always kept running. The default value is 5.

**`SONAR_WEB_HTTP_ACCEPTCOUNT=25`**  
The maximum queue length for incoming connection requests when all possible request processing threads are in use. Any requests received when the queue is full will be refused. The default value is 25.

**`SONAR_WEB_HTTP_KEEPALIVETIMEOUT=60000`**  
The number of milliseconds this Connector will wait for another HTTP request before closing the connection. Use a value of -1 to indicate no (i.e. infinite) timeout. The default value is 60000 (ms).

**`SONAR_AUTH_JWTBASE64HS256SECRET=`**  
By default users are logged out and sessions closed when server is restarted. If you prefer keeping user sessions open, a secret should be defined. Value is HS256 key encoded with base64. It must be unique for each installation of SonarQube. Example of command-line:  
echo -n "type_what_you_want" | openssl dgst -sha256 -hmac "key" -binary | base64

**`SONAR_WEB_SESSIONTIMEOUTINMINUTES=4320`**  
The inactivity timeout duration of user sessions, in minutes. After the configured period of time, the user is logged out. The default value is 3 days (4320 minutes). The value cannot be less than 6 minutes or greater than 3 months (129600 minutes). Value must be strictly positive.

**`SONAR_WEB_SYSTEMPASSCODE=`**  
A passcode can be defined to access some web services from monitoring tools without having to use the credentials of a system administrator. Check the Web API documentation to know which web services are supporting this authentication mode. The passcode should be provided in HTTP requests with the header "X-Sonar-Passcode". By default feature is disabled.

## SSO Authentication

**`SONAR_WEB_SSO_ENABLE=false`**  
Enable authentication using HTTP headers  

**`SONAR_WEB_SSO_LOGINHEADER=X-Forwarded-Login`**  
Name of the header to get the user login. Only alphanumeric, '.' and '@' characters are allowed  

**`SONAR_WEB_SSO_NAMEHEADER=X-Forwarded-Name`**  
Name of the header to get the user name  

**`SONAR_WEB_SSO_EMAILHEADER=X-Forwarded-Email`**  
Name of the header to get the user email (optional)  

**`SONAR_WEB_SSO_GROUPSHEADER=X-Forwarded-Groups`**  
Name of the header to get the list of user groups, separated by comma (optional). If the SONAR_SSO_GROUPSHEADER is set, the user will belong to those groups if groups exist in SonarQube. If none of the provided groups exists in SonarQube, the user will only belong to the default group. Note that the default group will always be set.  
 
**`SONAR_WEB_SSO_REFRESHINTERVALINMINUTES=5`**  
Interval used to know when to refresh name, email, and groups. During this interval, if for instance the name of the user is changed in the header, it will only be updated after X minutes. 

## LDAP Configuration

**`SONAR_SECURITY_REALM=LDAP`**  
Enable the LDAP feature

**`SONAR_AUTHENTICATOR_DOWNCASE=true`**  
Set to true when connecting to a LDAP server using a case-insensitive setup.

**`LDAP_URL=ldap://localhost:10389`**  
URL of the LDAP server. Note that if you are using LDAPS, then you should install the server certificate into the Java truststore.

**`LDAP_BINDDN=cn=sonar,ou=users,o=mycompany`**  
Bind DN is the username of an LDAP user to connect (or bind) with. Leave this blank for anonymous access to the LDAP directory (optional)

**`LDAP_BINDPASSWORD=secret`**  
Bind Password is the password of the user to connect with. Leave this blank for anonymous access to the LDAP directory (optional)

**`LDAP_AUTHENTICATION=simple`**  
Possible values: simple | CRAM-MD5 | DIGEST-MD5 | GSSAPI See http://java.sun.com/products/jndi/tutorial/ldap/security/auth.html (default: simple)

**`LDAP_REALM=example.org`**  
See :
  * http://java.sun.com/products/jndi/tutorial/ldap/security/digest.html
  * http://java.sun.com/products/jndi/tutorial/ldap/security/crammd5.html
(optional)

**`LDAP_CONTEXTFACTORYCLASS=com.sun.jndi.ldap.LdapCtxFactory`**  
Context factory class (optional)

**`LDAP_STARTTLS=true`**  
Enable usage of StartTLS (default : false)

**`LDAP_FOLLOWREFERRALS=false`**
Follow or not referrals. See http://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html (default: true)

### User Mapping

**`LDAP_USER_BASEDN=cn=users,dc=example,dc=org`**  
Distinguished Name (DN) of the root node in LDAP from which to search for users (mandatory)

**`LDAP_USER_REQUEST=(&(objectClass=user)(sAMAccountName={login}))`**  
LDAP user request. (default: (&(objectClass=inetOrgPerson)(uid={login})) )

**`LDAP_USER_REALNAMEATTRIBUTE=name`**
Attribute in LDAP defining the user’s real name. (default: cn)

**`LDAP_USER_EMAILATTRIBUTE=email`**  
Attribute in LDAP defining the user’s email. (default: mail)

### Group Mapping

**`LDAP_GROUP_BASEDN=cn=groups,dc=example,dc=org`**  
Distinguished Name (DN) of the root node in LDAP from which to search for groups. (optional, default: empty)

**`LDAP_GROUP_REQUEST=(&(objectClass=group)(member={dn}))`**  
LDAP group request (default: (&(objectClass=groupOfUniqueNames)(uniqueMember={dn})) )

**`LDAP_GROUP_IDATTRIBUTE=sAMAccountName`**  
Property used to specifiy the attribute to be used for returning the list of user groups in the compatibility mode. (default: cn)

## Compute Engine

**`SONAR_CE_JAVAOPTS=**  
The Compute Engine is responsible for processing background tasks.  
Compute Engine is executed in a dedicated Java process.  
Use the following property to customize JVM options.

[[info]]
| The HotSpot Server VM is recommended. The property -server should be added if server mode
| is not enabled by default on your environment:
| http://docs.oracle.com/javase/8/docs/technotes/guides/vm/server-class.html

**`SONAR_CE_JAVAADDITIONALOPTS=`**  
Same as previous property, but allows to not repeat all other settings like -Xmx

## Elasticsearch

Elasticsearch is used to facilitate fast and accurate information retrieval.
It is executed in a dedicated Java process. 

[[warning]]
| Linux users on 64-bit systems, ensure Virtual Memory on your system is correctly configured for Elasticsearch to run properly (see [here](https://www.elastic.co/guide/en/elasticsearch/reference/5.5/vm-max-map-count.html) for details).
|
| When SonarQube runs standalone, a warning such as the following may appear in logs/es.log:
|     "max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]"
|
| When SonarQube runs as a cluster, however, Elasticsearch will refuse to start.

**`SONAR_SEARCH_JAVAOPTS=`**  
JVM options of Elasticsearch process

**`SONAR_SEARCH_JAVAADDITIONALOPTS=`**  
Same as previous property, but allows to not repeat all other settings like -Xmx

**`SONAR_SEARCH_PORT=9001`**  
Elasticsearch port. Default is 9001. Use 0 to get a free port.
As a security precaution, should be blocked by a firewall and not exposed to the Internet.

**`SONAR_SEARCH_HOST=`**  
Elasticsearch host. The search server will bind this address and the search client will connect to it.
Default is loopback address.
As a security precaution, should NOT be set to a publicly available address.

## Update Center

**`SONAR_UPDATECENTER_ACTIVATE=true`**  
Update Center requires an internet connection to request https://update.sonarsource.org
It is enabled by default.

**`HTTP_PROXYHOST=`**  
**`HTTP_PROXYPORT=`**  
HTTP proxy (default none)  

**`HTTPS_PROXYHOST=`**  
**`HTTPS_PROXYPORT=`**  
HTTPS proxy (defaults are values of HTTP_PROXYHOST and HTTP_PROXYPORT)  
  
**`HTTP_AUTH_NTLM_DOMAIN=`**  
NT domain name if NTLM proxy is used  

**`SOCKSPROXYHOST=`**  
**`SOCKSPROXYPORT=`**  
SOCKS proxy (default none)  

**`HTTP_PROXYUSER=`**  
**`HTTP_PROXYPASSWORD=`**  
Proxy authentication (used for HTTP, HTTPS and SOCKS proxies)  
  
**`HTTP_NONPROXYHOSTS=`**  
Proxy exceptions: list of hosts that can be accessed without going through the proxy separated by the '|' character, wildcard character '*' can be used for pattern matching used for HTTP and HTTPS (default none) (note: localhost and its literal notations (127.0.0.1, ...) are always excluded).

## Logging

SonarQube produces logs in four logs files located in the same directory (see property `SONAR_PATH_LOGS` below),
one per process:
* Main process (aka. App) logs in sonar.log
* Web Server (aka. Web) logs in web.log
* Compute Engine (aka. CE) logs in ce.log
* Elasticsearch (aka. ES) logs in es.log

All four files follow the same rolling policy (see `SONAR_LOG_ROLLINGPOLICY` and `SONAR_LOG_MAXFILES`) but it applies
individually (eg. if `SONAR_LOG_MAXFILES=4`, there can be at most 4 of each files, ie. 16 files in total).

All four files have logs in the same format:

|1|2|3|
|----|----|-|--------------------|------------------------------|----|
2016.11.16 16:47:00 INFO  ce[AVht0dNXFcyiYejytc3m][o.s.s.c.t.CeWorkerCallableImpl] Executed task | project=org.sonarqube:example-java-maven | type=REPORT |

|4|5|6|
|--------------------|----------------------|-------------|
| id=AVht0dNXFcyiYejytc3m | submitter=admin | time=1699ms|

**1**: timestamp. Format is YYYY.MM.DD HH:MM:SS  
   YYYY: year on 4 digits  
   MM: month on 2 digits  
   DD: day on 2 digits  
   HH: hour of day on 2 digits in 24 hours format  
   MM: minutes on 2 digits  
   SS: seconds on 2 digits  
   
**2**: log level.  
   Possible values (in order of descending criticality): ERROR, WARN, INFO, DEBUG and TRACE  
   
**3**: process identifier. Possible values: app (main), web (Web Server), ce (Compute Engine) and es (Elasticsearch)  

**4**: SQ thread identifier. Can be empty. In the Web Server, if present, it will be the HTTP request ID. In the Compute Engine, if present, it will be the task ID.
   
**5**: logger name. Usually a class canonical name. Package names are truncated to keep the whole field to 20 characters max
   
**6**: log payload. Content of this field does not follow any specific format, can vary in length and include line returns. Some logs, however, will follow the convention to provide data in payload in the format "| key=value"  Especially, log of profiled pieces of code will end with "| time=XXXXms".  

**`SONAR_LOG_LEVEL=INFO`**  
Global level of logs (applies to all 4 processes). Supported values are INFO (default), DEBUG and TRACE

**`SONAR_LOG_LEVEL_APP=INFO`**  
**`SONAR_LOG_LEVEL_WEB=INFO`**  
**`SONAR_LOG_LEVEL_CE=INFO`**  
**`SONAR_LOG_LEVEL_ES=INFO`**  
Level of logs of each process can be controlled individually with their respective properties. When specified, they overwrite the level defined at global level. Supported values are INFO, DEBUG and TRACE

**`SONAR_PATH_LOGS=logs`**  
Path to log files. Can be absolute or relative to installation directory. Default is <installation home>/logs

**`SONAR_LOG_ROLLINGPOLICY=time:yyyy-MM-dd`**  
Rolling policy of log files:
* based on time if value starts with "time:", for example by day ("time:yyyy-MM-dd") or by month ("time:yyyy-MM")  
* based on size if value starts with "size:", for example "size:10MB"  
* disabled if value is "none".  That needs logs to be managed by an external system like logrotate.  

**`SONAR_LOG_MAXFILES=7`**  
Maximum number of files to keep if a rolling policy is enabled.
* maximum value is 20 on size rolling policy  
* unlimited on time rolling policy. Set to zero to disable old file purging.

**`SONAR_WEB_ACCESSLOGS_ENABLE=true`**
Access log is the list of all the HTTP requests received by server. If enabled, it is stored
in the file {`SONAR_PATH_LOGS`}/access.log. This file follows the same rolling policy as other log file
(see `SONAR_LOG_ROLLINGPOLICY` and `SONAR_LOG_MAXFILES`).

**`SONAR_WEB_ACCESSLOGS_PATTERN=%i{X-Forwarded-For} %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}" "%reqAttribute{ID}"`**  
Format of access log. It is ignored if `SONAR_WEB_ACCESSLOGS_ENABLE=false`.   

Possible values are:
   - "common" is the Common Log Format, shortcut to: %h %l %u %user %date "%r" %s %b
   - "combined" is another format widely recognized, shortcut to: %h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}"
   - else a custom pattern. See http://logback.qos.ch/manual/layouts.html#AccessPatternLayout.
   
The login of authenticated user is not implemented with "%u" but with "%reqAttribute{LOGIN}" (since version 6.1).  
The value displayed for anonymous users is "-".  

The token name used for requests will be added to the access log if the "%reqAttribute{TOKEN_NAME}" is added (since version 9.5).

The SonarQube's HTTP request ID can be added to the pattern with "%reqAttribute{ID}" (since version 6.2).  

If SonarQube is behind a reverse proxy, then the following value allows to display the correct remote IP address:

Default value (which was "combined" before version 6.2) is equivalent to "combined + SQ HTTP request ID":
`SONAR_WEB_ACCESSLOGS_PATTERN=%h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}" "%reqAttribute{ID}"`

## DataCenter Edition

**`SONAR_CLUSTER_NAME=sonarqube`**

The name of the cluster. Required if multiple clusters are present on the same network. For example, this prevents mixing Production and Preproduction clusters. This will be the name stored in the Hazelcast cluster and used as the name of the Elasticsearch cluster.

**`SONAR_CLUSTER_SEARCH_HOSTS`**

Comma-delimited list of search hosts in the cluster. The list can contain either the host or the host and port, but not both. The item format is `ip/hostname` for host only or`ip/hostname:port` for host and port. `ip/hostname` can also be set to the service name of the search containers .

**`SONAR_CLUSTER_SEARCH_PASSWORD`**

Password for Elasticsearch built-in user (elastic) which will be used on the client site. If provided, it enables authentication. This property needs to be set to the same value throughout the cluster.

### Search Nodes Only

**`SONAR_CLUSTER_ES_HOSTS`**

Comma-delimited list of search hosts in the cluster. The list can contain either the host or the host and port but not both. The item format is `ip/hostname` for host only or`ip/hostname:port` for host and port, while `ip/hostname` can also be set to the service name of the search containers.

**`SONAR_CLUSTER_NODE_NAME`**

The name of the node that is used on Elasticsearch and stored in Hazelcast member attribute (NODE_NAME)

**`SONAR_CLUSTER_ES_SSL_KEYSTORE`**

File path to a keystore in PKCS#12 format. Can be the same PKCS#12 container as the `SONAR_CLUSTER_ES_SSL_TRUSTSTORE`. The user running SonarQube must have READ permission to that file. Required if password provided.

**`SONAR_CLUSTER_ES_SSL_KEYSTOREPASSWORD`**

Password to the keystore.

**`SONAR_CLUSTER_ES_SSL_TRUSTSTORE`**

File path to a truststore in PKCS#12 format. Can be the same PKCS#12 container as the `SONAR_CLUSTER_ES_SSL_KEYSTORE`. The user running SonarQube must have READ permission to that file. Required if password provided.	

**`SONAR_CLUSTER_ES_SSL_TRUSTSTOREPASSWORD`**

Password to the truststore.

### Application Nodes Only

**`SONAR_CLUSTER_HOSTS`**

Comma-delimited list of all **application** hosts in the cluster. This value must contain **only application hosts**. Each item in the list must contain the port if the default `SONAR_CLUSTER_NODE_PORT` value is not used. Item format is `ip/hostname`, `ip/hostname:port`. `ip/hostname` can also be set to the service name of the application containers.

**`SONAR_CLUSTER_NODE_PORT`**

The Hazelcast port for communication with each application member of the cluster. Default: `9003`

## Others

**`SONAR_NOTIFICATIONS_DELAY=60`**  
Delay in seconds between processing of notification queue. Default is 60 seconds.

**`SONAR_PATH_DATA=data`**  
**`SONAR_PATH_TEMP=temp`**  
Paths to persistent data files (embedded database and search index) and temporary files. Can be absolute or relative to installation directory. Defaults are respectively <installation home>/data and <installation home>/temp

**`SONAR_TELEMETRY_ENABLE=true`**
Telemetry - Share anonymous SonarQube statistics. By sharing anonymous SonarQube statistics, you help us understand how SonarQube is used so we can improve the product to work even better for you. We don't collect source code or IP addresses. And we don't share the data with anyone else. To see an example of the data shared: login as a global administrator, call the WS api/system/info and check the Statistics field.

## Development – only for developers
[[warning]]
| The following properties MUST NOT be used in production environments.

**`SONAR_SEARCH_HTTPPORT=-1`**  
Elasticsearch HTTP connector
