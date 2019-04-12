---
title: Prerequisites and Overview
url: /requirements/requirements/
---
## Prerequisite
The only prerequisite for running SonarQube is to have Java (Oracle JRE 8 or OpenJDK 8) installed on your machine.

![](/images/exclamation.svg) **Note:** _On Mac OS X it is highly recommended to install Oracle JDK 8 instead of the corresponding Oracle JRE since the JRE installation does not fully set up your Java environment properly. See [this post](http://stackoverflow.com/questions/15624667/mac-osx-java-terminal-version-incorrect) for more information._

## Hardware Requirements
1. A small-scale (individual or small team) instance of the SonarQube server requires at least 2GB of RAM to run efficiently and 1GB of free RAM for the OS. If you are installing an instance for a large teams or Enterprise, please consider the additional recommendations below.
2. The amount of disk space you need will depend on how much code you analyze with SonarQube. As an example, [SonarCloud](https://sonarcloud.io) the public instance of SonarQube, has more than 350 million lines of code under analysis with 5 years of history. SonarCloud is currently running on clustered [Amazon EC2 m5.large](http://aws.amazon.com/ec2/instance-types/) instances with allocations of 50 Gb of drive space per node. It handles 19,000+ projects with roughly 14M open issues. SonarCloud runs on PostgreSQL 9.5 and it is using about 250Gb of disk space for the database.
3. SonarQube must be installed on hard drives that have excellent read & write performance. Most importantly, the "data" folder houses the Elasticsearch indices on which a huge amount of I/O will be done when the server is up and running. Great read & write hard drive performance will therefore have a great impact on the overall SonarQube server performance.
4. SonarQube does not support 32-bit systems on the server side. SonarQube does, however, support 32-bit systems on the scanner side.

### Enterprise Hardware Recommendations
For large teams or Enterprise-scale installations of SonarQube, additional hardware is required. At the Enterprise level, monitoring your SonarQube instance/instance-administration/java-process-memory is essential and should guide further hardware upgrades as your instance grows. A starting configuration should include at least:

* 8 cores, to allow the main SonarQube platform to run with multiple Compute Engine workers
* 16GB of RAM
For additional requirements and recommendations relating to database and ElasticSearch, see Hardware Recommendations/requirements/hardware-recommendations.

## Supported Platforms
### Java
The SonarQube Java analyzer is able to analyze any kind of Java source files regardless of the version of Java they comply to. But SonarQube analysis and the SonarQube Server require specific versions of the JVM.

We recommend using the Critical Path Update (CPU) releases.

| Java           |                          |
| -------------- | ------------------------ |
| Oracle JRE     | ![](/images/cross.svg) 9 |
|                | ![](/images/check.svg) 8 |
|                | ![](/images/cross.svg) 7 |
| Open JDK       | ![](/images/cross.svg) 9 |
|                | ![](/images/check.svg) 8 |
|                | ![](/images/cross.svg) 7 |
| IBM JRE        | ![](/images/cross.svg)   |
| GCJ            | ![](/images/cross.svg)   |
| Oracle JRockit | ![](/images/cross.svg)   |

| Database                                                    |                                                                                                                                                                                                                                                                   |
| ----------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [PostgreSQL](http://www.postgresql.org/)                    | ![](/images/check.svg) 9.3 - 9.6                                                                                                                                                                                                                                  |
|                                                             | ![](/images/check.svg) 10                                                                                                                                                                                                                                         |
|                                                             | ![](/images/exclamation.svg) Must be configured to use UTF-8 charset                                                                                                                                                                                              |
| [Microsoft SQL Server](http://www.microsoft.com/sqlserver/) | ![](/images/cross.svg) 2012 (MSSQL Server 11.0)                                                                                                                                                                                                                   |
|                                                             | ![](/images/check.svg) 2014 (MSSQL Server 12.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/check.svg) 2016 (MSSQL Server 13.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/check.svg) 2017 (MSSQL Server 14.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/exclamation.svg) Collation must be case-sensitive (CS) and accent-sensitive (AS) (example: Latin1_General_CS_AS)                                                                                                                                      |
|                                                             | ![](/images/exclamation.svg) READ_COMMITTED_SNAPSHOT must be set on the SonarQube database to avoid potential deadlocks under heavy load                                                                                                                          |
|                                                             | ![](/images/info.svg) Both Windows authentication (“Integrated Security”) and SQL Server authentication are supported. See the Microsoft SQL Server section in Installing/installation/installing-the-server page for instructions on configuring authentication. |
| [Oracle] (http://www.oracle.com/database/)                  | ![](/images/cross.svg) 10G                                                                                                                                                                                                                                        |
|                                                             | ![](/images/check.svg) 11G with backward-compatible [Oracle 12.2.x](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-ucp-122-3110062.html) drivers                                                                                                   |
|                                                             | ![](/images/check.svg) 12C with [Oracle 12.2.x](http://www.oracle.com/technetwork/database/features/jdbc/jdbc-ucp-122-3110062.html) drivers                                                                                                                       |
|                                                             | ![](/images/check.svg) XE Editions are supported
|                                                             | ![](/images/exclamation.svg) Must be configured to use a UTF8-family charset (see NLS_CHARACTERSET)                                                                                                                                                               |
|                                                             | ![](/images/exclamation.svg) The driver ojdbc14.jar is not supported                                                                                                                                                                                              |
|                                                             | ![](/images/exclamation.svg) Only the thin mode is supported, not OCI                                                                                                                                                                                             |
| [MySQL](http://www.mysql.com/)                              | Not recommended for large instances                                                                                                                                                                                                                               |
|                                                             | ![](/images/check.svg) 5.6                                                                                                                                                                                                                                       |
|                                                             | ![](/images/check.svg) 5.7                                                                                                                                                                                                                                       |
|                                                             | ![](/images/exclamation.svg) Must be configured to use UTF8 charset and a case-sensitive (CS) collation                                                                                                                                                           |
|                                                             | ![](/images/exclamation.svg) Only InnoDB storage engine is supported, but not MyISAM                                                                                                                                                                              |
|                                                             | ![](/images/exclamation.svg)Only the bundled mysql-connector-java jar is supported                                                                                                                                                                                |

### Web Browser
To get the full experience SonarQube has to offer, you must enable JavaScript in your browser.

|                             |                                         |
| --------------------------- | --------------------------------------- |
| Microsoft Internet Explorer | ![](/images/cross.svg) IE 9             |
|                             | ![](/images/cross.svg) IE 10            |
|                             | ![](/images/check.svg) IE 11            |
| Microsoft Edge              | ![](/images/check.svg) Latest           |
| Mozilla Firefox             | ![](/images/check.svg) Latest           |
| Google Chrome               | ![](/images/check.svg) Latest           |
| Opera                       | ![](/images/exclamation.svg) Not tested |
| Safari                      | ![](/images/check.svg) Latest           |

<!-- sonarqube -->
## GitHub Enterprise Integration
To add Pull Request analysis to Checks in GitHub Enterprise, you must be running GitHub Enterprise version 2.14+.

## Bitbucket Server Integration
To add Pull Request analysis to Code Insights in Bitbucket Server, you must be running Bitbucket Server version 5.15+.
<!-- /sonarqube -->

## Platform notes
### Linux
If you're running on Linux, you must ensure that:

* `vm.max_map_count` is greater or equals to 262144
* `fs.file-max` is greater or equals to 65536
* the user running SonarQube can open at least 65536 file descriptors
* the user running SonarQube can open at least 2048 threads

You can see the values with the following commands:
```
sysctl vm.max_map_count
sysctl fs.file-max
ulimit -n
ulimit -u
```

You can set them dynamically for the current session by running  the following commands as `root`:
```
sysctl -w vm.max_map_count=262144
sysctl -w fs.file-max=65536
ulimit -n 65536
ulimit -u 2048
```

To set these values more permanently, you must update either _/etc/sysctl.d/99-sonarqube.conf_ (or _/etc/sysctl.conf_ as you wish) to reflect these values.

If the user running SonarQube (`sonarqube` in this example) does not have the permission to have at least 65536 open descriptors, you must insert this line in _/etc/security/limits.d/99-sonarqube.conf_ (or _/etc/security/limits.conf_ as you wish):
```
sonarqube   -   nofile   65536
sonarqube   -   nproc    2048
```

You can get more detail in the [Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/system-config.html).

If you are using `systemd` to start SonarQube, you must specify those limits inside your unit file in the section \[service\] :
```
[Service]
...
LimitNOFILE=65536
LimitNPROC=2048
...
```

### seccomp filter
By default, Elasticsearch uses [seccomp filter](https://www.kernel.org/doc/Documentation/prctl/seccomp_filter.txt). On most distribution this feature is activated in the kernel, however on distributions like Red Hat Linux 6 this feature is deactivated. If you are using a distribution without this feature and you cannot upgrade to a newer version with seccomp activated, you have to explicitly deactivate this security layer by updating `sonar.search.javaAdditionalOpts` in _$SONARQUBE_HOME/conf/sonar.properties_:
```
sonar.search.javaAdditionalOpts=-Dbootstrap.system_call_filter=false
```

You can check if seccomp is available on your kernel with:
```
$ grep SECCOMP /boot/config-$(uname -r)
```

If your kernel has seccomp, you will see:
```
CONFIG_HAVE_ARCH_SECCOMP_FILTER=y
CONFIG_SECCOMP_FILTER=y
CONFIG_SECCOMP=y
```
For more detail, see the [Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/breaking-changes-5.6.html).
