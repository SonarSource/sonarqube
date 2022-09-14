---
title: Monitoring
url: /instance-administration/monitoring/
---

Monitoring your SonarQube instance is key to keeping it healthy and having happy users.

As a start, you can use this Web API to get an overview of the health of your SonarQube installation:

* [api/system/health](/#sonarqube-admin#/api/system/health)

## Java Process Memory

The SonarQube application server consists of three main Java processes:

* Compute Engine
* Elasticsearch
* Web (including embedded web server)

Each of these Java processes has its own memory settings that can be configured in the _$SONARQUBE_HOME/conf/sonar.properties_ file. The default memory settings that ship with SonarQube are fine for most instances. If you are supporting a large SonarQube instance (more than 100 users or more than 5,000,000 lines of code) or an instance that is part of your Continuous Integration pipeline, you should monitor the memory and CPU usage of all three key Java processes on your instance, along with overall disk space. Monitoring will allow you to see if any of the processes is running short of resources and take action ahead of resource shortages. There are numerous monitoring tools available, both open source and commercial, to help you with this task. SonarSource does not recommend or endorse any particular tool.

## Memory settings

You may need to increase your memory settings if you see the following symptoms:

* Your monitoring tools show one or more of the SonarQube processes is reaching its memory limit
* Any of the SonarQube processes crashes and/or generates an out-of-memory error in the sonar.log file
* A SonarQube background task fails with an out-of-memory error in the background task log
* The store size of the Issues index of your Elasticsearch instance (visible in the System Info) is greater than or equal to the memory allocated to the Elasticsearch Java process

You can increase the maximum memory allocated to the appropriate process by increasing the  -Xmx memory setting for the corresponding Java process in your _$SONARQUBE_HOME/conf/sonar.properties_ file:

Java Process | SonarQube Property | Notes
--- | --- | ---
Compute Engine | sonar.ce.javaOpts
Elasticsearch | sonar.search.javaOpts | It is recommended to set the min and max memory to the same value to prevent the heap from resizing at runtime, which diverts JVM resources and can greatly increase response times of in-flight requests.
Web | sonar.web.javaOpts

The -Xmx parameter accepts numbers in both megabytes (e.g. -Xmx2048m) and gigabytes (e.g. -Xmx2G). The metric suffix is case-insensitive.

## Exposed JMX MBeans

The SonarQube Server offers visibility about what happens internally through the exposure of JMX MBeans.

In addition to the classical Java MBeans providing information about the ClassLoader, OS, Memory, and Threads you have access to three more MBeans in the SonarQube Server:

* ComputeEngine
* Database
* SonarQube

All these MBeans are read-only. It's not possible to modify or reset their values in real time.

[[collapse]]
| ## ComputeEngineTasks MBean
| Attribute Name | Description
| ---|---
| ProcessingTime | Measure the time (in ms) spent to process Background Tasks since the last restart of SonarQube. Its value will always increase and will be reset by a restart of SonarQube.  This measure is very powerful when combined with SuccessCount and ErrorCount measures to get the average time to handle a Background Task, or when used to understand how much time the SonarQube Server is spending during a day to handle Background Tasks. It gives you an indication of the load on your server.
| ErrorCount | Number of Background Tasks which failed since the last restart of SonarQube
| PendingCount | Number of Background Tasks waiting to be processed since the last restart of SonarQube. This measure is the same for all Compute Engine workers since Background Tasks are waiting in a common queue.
| InProgressCount | Number of Background Tasks currently under processing. Its value is either 1 or 0, since SonarQube can process only one task at a time.
| SuccessCount | Number of Background Tasks successfully processed since the last restart of SonarQube
| WorkerCount | Number of Background Tasks that can be processed at the same time
| PendingTime | Pending time (in ms) of the oldest Background Task waiting to be processed. This measure, together with PendingCount, helps you know if analyses are stacking and taking too long to start processing. This helps you evaluate if it might be worth configuring additional Compute Engine workers (Enterprise Edition) or additional nodes (Data Center Edition) to improve SonarQube performance.
|
| Note:
| * the total number of Background Tasks handled since the last restart of SonarQube is equal to SuccessCount + ErrorCount
| * all values reset to their default values after restarting SonarQube

[[collapse]]
| ## Database MBean
| ### Same attributes are available for both ComputeEngineServer and WebServer.
| Attribute Name | Description
| ---|---
| MigrationStatus | Possible values are: UP_TO_DATE, REQUIRES_UPGRADE, REQUIRES_DOWNGRADE, FRESH_INSTALL (only available for WebServer).
| PoolActiveConnections | Number of active database connections
| PoolIdleConnections | Number of database connections waiting to be used
| PoolMaxConnections | Maximum number of active database connections
| PoolTotalConnections | Total number of connections currently in the pool
| PoolMaxWaitMillis | Maximum number of milliseconds that a client will wait for a connection from the pool

[[collapse]]
| ## SonarQube MBean
| Attribute Name | Description
| ---|---
| LogLevel | Log Level: INFO, DEBUG, TRACE
| ServerId | SonarQube Server ID
| Version | SonarQube Version

## How do I Activate JMX?

### Local Access

There is nothing to activate to view SonarQube MBeans if your tool is running on the same server as the SonarQube Server.

### Remote Access

Here are examples of configuration to activate remote access to JMX MBeans.

* For the WebServer:
```
# JMX WEB - 10443/10444
sonar.web.javaAdditionalOpts=-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=true -Dcom.sun.management.jmxremote.port=10443 -Dcom.sun.management.jmxremote.rmi.port=10444 -Dcom.sun.management.jmxremote.password.file=/opt/sonarsource/sonar/conf/jmxremote.password -Dcom.sun.management.jmxremote.access.file=/opt/sonarsource/sonar/conf/jmxremote.access
```

* For the ComputeEngine:

There is no specific javaAdditionalOpts entry, simply amend sonar.ce.javaOpts.

Example of `jmxremote.access`:

```
#
# JMX Access Control file
#
reader readonly
admin  readwrite \
    create javax.management.monitor.*,javax.management.timer.*,com.sun.management.*,com.oracle.jrockit.* \
    unregister
```

Example of `jmxremote.password`:

```
#
# JMX Access Password file
#
reader readerpassword
admin  adminpassword
```

Note: on `jmxremote.password`, you should apply `chmod 600` or `400` for security reasons.

## Prometheus Monitoring
You can monitor your SonarQube instance using SonarQube's native integration with Prometheus. Through this integration, you can ensure your instance is running properly and know if you need to take action to prevent future issues. 

Prometheus monitors your SonarQube instance by collecting metrics from the `/api/monitoring/metrics` endpoint. Results are returned in OpenMetrics text format. See Prometheus' documentation on [Exposition Formats](https://prometheus.io/docs/instrumenting/exposition_formats/) for more information on the OpenMetrics text format. 

Monitoring through this endpoint requires authentication. You can access the endpoint following ways:

- **`Authorization:Bearer xxxx` header:** You can use a bearer token during database upgrade and when SonarQube is fully operational. Define the bearer token in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **`X-Sonar-Passcode: xxxxx` header:** You can use `X-Sonar-passcode` during database upgrade and when SonarQube is fully operational. Define `X-Sonar-passcode` in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **username:password and JWT token:** When SonarQube is fully operational, system admins logged in with local or delegated authentication can access the endpoint.  

<!-- static -->

For more information on deploying SonarQube on Kubernetes:

- For Community, Developer, and Enterprise Edition, see [Deploy SonarQube on Kubernetes](/setup/sonarqube-on-kubernetes/). 
- For Data Center Edition, see [Deploy a SonarQube Cluster on Kubernetes](/setup/sonarqube-cluster-on-kubernetes/).

<!-- /static -->

