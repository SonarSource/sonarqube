---
title: Prerequisites and Overview
url: /requirements/requirements/
---
## Prerequisite
You must be able to install Java (Oracle JRE or OpenJDK) on the machine where you plan to run SonarQube.

## Hardware Requirements
1. A small-scale (individual or small team) instance of the SonarQube server requires at least 2GB of RAM to run efficiently and 1GB of free RAM for the OS. If you are installing an instance for a large teams or Enterprise, please consider the additional recommendations below.
2. The amount of disk space you need will depend on how much code you analyze with SonarQube.
3. SonarQube must be installed on hard drives that have excellent read & write performance. Most importantly, the "data" folder houses the Elasticsearch indices on which a huge amount of I/O will be done when the server is up and running. Great read & write hard drive performance will therefore have a great impact on the overall SonarQube server performance.
4. SonarQube does not support 32-bit systems on the server side. SonarQube does, however, support 32-bit systems on the scanner side.

### Enterprise Hardware Recommendations
For large teams or Enterprise-scale installations of SonarQube, additional hardware is required. At the Enterprise level, [monitoring your SonarQube instance](/instance-administration/monitoring/) is essential and should guide further hardware upgrades as your instance grows. A starting configuration should include at least:

* 8 cores, to allow the main SonarQube platform to run with multiple Compute Engine workers
* 16GB of RAM
For additional requirements and recommendations relating to database and ElasticSearch, see [Hardware Recommendations](/requirements/hardware-recommendations/).

## Supported Platforms
### Java
The SonarQube server require Java version 11 and the SonarQube scanners require Java version 11 or 17. 

SonarQube is able to analyze any kind of Java source files regardless of the version of Java they comply to. 

We recommend using the Critical Patch Update (CPU) releases.

| Java           | Server                    | Scanners                  |
| -------------- |---------------------------|---------------------------|
| Oracle JRE     | ![](/images/cross.svg) 17 | ![](/images/check.svg) 17 |
|                | ![](/images/check.svg) 11 | ![](/images/check.svg) 11 |
|                | ![](/images/cross.svg) 8  | ![](/images/cross.svg) 8  |
| OpenJDK        | ![](/images/cross.svg) 17 | ![](/images/check.svg) 17 |
|                | ![](/images/check.svg) 11 | ![](/images/check.svg) 11 |
|                | ![](/images/cross.svg) 8  | ![](/images/cross.svg) 8  |

| Database                                                    |                                                                                                                                                                                                                                                                   |
| ----------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [PostgreSQL](http://www.postgresql.org/)                    | ![](/images/check.svg) 13                                                                                                                                                                                                                                         |
|                                                             | ![](/images/check.svg) 12                                                                                                                                                                                                                                         |
|                                                             | ![](/images/check.svg) 11                                                                                                                                                                                                                                         |
|                                                             | ![](/images/check.svg) 10                                                                                                                                                                                                                                         |
|                                                             | ![](/images/check.svg) 9.6                                                                                                                                                                                                                                        |
|                                                             | ![](/images/exclamation.svg) Must be configured to use UTF-8 charset                                                                                                                                                                                              |
| [Microsoft SQL Server](http://www.microsoft.com/sqlserver/) | ![](/images/check.svg) 2019 (MSSQL Server 15.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/check.svg) 2017 (MSSQL Server 14.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/check.svg) 2016 (MSSQL Server 13.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/check.svg) 2014 (MSSQL Server 12.0) with bundled Microsoft JDBC driver. Express Edition is supported.                                                                                                                                                 |
|                                                             | ![](/images/exclamation.svg) Collation must be case-sensitive (CS) and accent-sensitive (AS) (example: `Latin1_General_CS_AS`)                                                                                                                                    |
|                                                             | ![](/images/exclamation.svg) `READ_COMMITTED_SNAPSHOT` must be set on the SonarQube database to avoid potential deadlocks under heavy load                                                                                                                        |
|                                                             | ![](/images/info.svg) Both Windows authentication (“Integrated Security”) and SQL Server authentication are supported. See the Microsoft SQL Server section in Installing/installation/installing-the-server page for instructions on configuring authentication. |
| [Oracle](http://www.oracle.com/database/)                   | ![](/images/check.svg) 19C                                                                                                                                                                                                                                        |
|                                                             | ![](/images/check.svg) 18C                                                                                                                                                                                                                                        |
|                                                             | ![](/images/check.svg) 12C                                                                                                                                                                                                                                        |
|                                                             | ![](/images/check.svg) XE Editions                                                                                                                                                                                                                                |
|                                                             | ![](/images/exclamation.svg) Must be configured to use a UTF8-family charset (see `NLS_CHARACTERSET`)                                                                                                                                                             |
|                                                             | ![](/images/exclamation.svg) The driver ojdbc14.jar is not supported                                                                                                                                                                                              |
|                                                             | ![](/images/info.svg) We recommend using the latest Oracle JDBC driver                                                                                                                                                                                            |
|                                                             | ![](/images/exclamation.svg) Only the thin mode is supported, not OCI                                                                                                                                                                                             |
|                                                             | ![](/images/exclamation.svg) Only `MAX_STRING_SIZE=STANDARD` parameter is supported, not `EXTENDED`                                                                                                                                                               |

### Web Browser
To get the full experience SonarQube has to offer, you must enable JavaScript in your browser.

| Browser                     |                                         |
| --------------------------- | --------------------------------------- |
| Microsoft Edge              | ![](/images/check.svg) Latest           |
| Mozilla Firefox             | ![](/images/check.svg) Latest           |
| Google Chrome               | ![](/images/check.svg) Latest           |
| Opera                       | ![](/images/exclamation.svg) Not tested |
| Safari                      | ![](/images/check.svg) Latest           |

## Platform notes
### Linux
If you're running on Linux, you must ensure that:

* `vm.max_map_count` is greater than or equal to 524288
* `fs.file-max` is greater than or equal to 131072
* the user running SonarQube can open at least 131072 file descriptors
* the user running SonarQube can open at least 8192 threads

You can see the values with the following commands:
```
sysctl vm.max_map_count
sysctl fs.file-max
ulimit -n
ulimit -u
```

You can set them dynamically for the current session by running  the following commands as `root`:
```
sysctl -w vm.max_map_count=524288
sysctl -w fs.file-max=131072
ulimit -n 131072
ulimit -u 8192
```

To set these values more permanently, you must update either _/etc/sysctl.d/99-sonarqube.conf_ (or _/etc/sysctl.conf_ as you wish) to reflect these values.

If the user running SonarQube (`sonarqube` in this example) does not have the permission to have at least 131072 open descriptors, you must insert this line in _/etc/security/limits.d/99-sonarqube.conf_ (or _/etc/security/limits.conf_ as you wish):
```
sonarqube   -   nofile   131072
sonarqube   -   nproc    8192
```

If you are using `systemd` to start SonarQube, you must specify those limits inside your unit file in the section \[service\] :
```
[Service]
...
LimitNOFILE=131072
LimitNPROC=8192
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

### Fonts
Generating [Executive Reports](/project-administration/portfolio-pdf-configuration/) requires that fonts be installed on the server hosting SonarQube. On Windows servers, this is a given. However, this is not always the case for Linux servers.

The following should be ensured:

* [Fontconfig](https://en.wikipedia.org/wiki/Fontconfig) is installed on the server hosting SonarQube
* A package of [FreeType](https://www.freetype.org/) fonts is installed on the SonarQube server. The exact packages available will vary by distribution, but a commonly used package is `libfreetype6`

### FIPS
SonarQube will not run on Linux hosts where FIPS (Federal Information Processing Standard) is enforced.
