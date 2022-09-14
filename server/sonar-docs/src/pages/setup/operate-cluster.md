---
title: Configure & Operate a Cluster
url: /setup/operate-cluster/
---

_High availability and cluster scalability are features of the [Data Center Edition](https://redirect.sonarsource.com/editions/datacenter.html)._

Once the [SonarQube cluster is installed](/setup/install-cluster/), you have a high availability configuration that allows your SonarQube instance to stay up and running even if there is a crash or failure in one of the cluster's nodes. Your SonarQube cluster is also scalable, and you can add application nodes to increase your computing capabilities.

## Start, Stop, or Upgrade the Cluster

### Start the Cluster
To start a cluster, you need to follow these steps in order:

1. Start the search nodes
1. Start the application nodes

### Stop the Cluster
To stop a cluster, you need to follow these steps in order:

1. Stop the application nodes
1. Stop the search nodes

### Upgrade SonarQube
1. Stop the cluster.
1. Upgrade SonarQube on all nodes (application part, plugins, JDBC driver if required) following the usual upgrade procedure but without triggering the /setup phase.
1. Once all nodes have the same binaries: restart the cluster.
1. At this point, only one of the application nodes is up. Try to access `node_ip:port/setup` on each application node, and trigger the setup operation on the one that responds.

## Start or Stop a Node
You can start or stop a single node in the same way as starting and stopping an instance using a single server. By default, it's a graceful shutdown where no new analysis report processing can start, but the tasks in progress are allowed to finish.

## Install or Upgrade a Plugin
1. Stop the application nodes.
1. Install or upgrade the plugin on the application nodes.
	* If upgrading, remove the old version.
	* You don't need to install plugins on search nodes.
1. Restart the application nodes.

## Scalability
You have the option of adding application nodes (up to 10 total application nodes) to your cluster to increase computing capabilities. 

### Scaling in a Traditional Environment

#### **Adding an Application Node**
To add an Application Node:

1. Configure your new application node in sonar.properties. The following is an example of the configuration to be added to sonar.properties for a sixth application node (server6, ip6) in a cluster with the default five servers:

	**server6**
	```
	...
	sonar.cluster.enabled=true
	sonar.cluster.node.type=application
	sonar.cluster.node.host=ip6 
	sonar.cluster.node.port=9003
	sonar.cluster.hosts=ip1,ip2,ip6
	sonar.cluster.search.hosts=ip3:9001,ip4:9001,ip5:9001
	sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
	...
	```
2. Update the configuration of the preexisting nodes to include your new node. 

	While you don't need to restart the cluster after adding a node, you should ensure the configuration is up to date on all of your nodes to avoid issues when you eventually do need to restart.

#### **Removing an Application Node**
When you remove an application node, make sure to update the configuration of the remaining nodes. Much like adding a node, while you don't need to restart the cluster after removing a node, you should ensure the configuration is up to date on all of your nodes to avoid issues when you eventually do need to restart.

### Scaling in a Docker Environment

#### **Adding Application Nodes**

If you're using docker-compose, you can scale the application nodes using the following command:

`docker-compose up -d --scale sonarqube=3`

#### Removing Application Nodes
You can reduce the number of application nodes with the same command used to add application nodes by lowering the number.

## Monitoring
CPU and RAM usage on each node have to be monitored separately with an APM. 

In addition, we provide a Web API _api/system/health_ you can use to validate that all of the nodes in your cluster are operational.  

* GREEN: SonarQube is fully operational
* YELLOW: SonarQube is usable, but it needs attention in order to be fully operational
* RED: SonarQube is not operational

To call it from a monitoring system without having to give admin credentials, it is possible to setup a system passcode. You can configure this through the `sonar.web.systemPasscode` property in _$SONARQUBE_HOME/conf/sonar.properties_ if you're using a traditional environment or through the corresponding environment variable if you're using a Docker environment.

### Cluster Status
On the System Info page at **Administration > System**, you can check whether your cluster is running safely (green) or has some nodes with problems (orange or red).

### Maximum Pending Time for Tasks
On the global Background Tasks page at **Administration > Projects > Background Tasks**, you can see the number of **pending** tasks as well as the maximum **pending time** for the tasks in the queue. This shows the pending time of the oldest background task waiting to be processed. You can use this to evaluate if it might be worth configuring additional Compute Engine workers (Enterprise Edition) or additional nodes (Data Center Edition) to improve SonarQube performance. 

## Compute Engine Workers
If you change the number of [Compute Engine workers](/instance-administration/compute-engine-performance/) in the UI, you must restart each application node to have the change take effect.

## Project Move
When the [Project Move](/instance-administration/project-move/) feature is used in a DC installation:

* Projects are exported on only one of the application nodes 
* The archive of the exported projects must be copied to all the applications nodes in the target server

## Configuration details
There are three TCP networks to configure: 

- the network of application nodes that relies on Hazelcast.
- the network used for Elasticsearch internal communication between search nodes (`es` properties).
- the network between application nodes and search nodes (`search` properties).

[Hazelcast](https://hazelcast.org/) is used to manage the communication between the cluster's application nodes. You don't need to install it yourself, it's provided out of the box.

## Docker Environment Configuration
In a Docker environment, your properties are configured using [Environment Variables](/setup/environment-variables/).

## Traditional Environment Configuration
The following properties may be defined in the _$SONARQUBE_HOME/conf/sonar.properties_ file of each node in a cluster. When defining a property that contains a list of hosts (`*.hosts`) the port is not required if the default port was not overridden in the configuration.

[[warning]]
| Ports can be unintentionally exposed. We recommend only giving external access to the application nodes and to main port (`sonar.web.port`).

### All nodes
Property | Description | Default | Required | 
---|---|---|---|
`sonar.cluster.enabled`|Set to `true` in each node to activate the cluster mode|`false`|yes
`sonar.cluster.name`|The name of the cluster. **Required if multiple clusters are present on the same network.** For example this prevents mixing Production and Preproduction clusters. This will be the name stored in the Hazelcast cluster and used as the name of the Elasticsearch cluster.|`sonarqube`|no
`sonar.cluster.node.name`|The name of the node that is used on Elasticsearch and stored in Hazelcast member attribute (NODE_NAME) for sonar-application|`sonarqube-{UUID}`|no
`sonar.cluster.node.type`|Type of node: either `application` or `search`| |yes

### Application nodes
Property  | Description | Required 
---|---|---
`sonar.cluster.hosts`|Comma-delimited list of all **application** hosts in the cluster. This value must contain **only application hosts**. Each item in the list must contain the port if the default `sonar.cluster.node.port` value is not used. Item format is `sonar.cluster.node.host` or `sonar.cluster.node.host:sonar.cluster.node.port`.|yes
`sonar.cluster.node.host`|IP address of the network card that will be used by Hazelcast to communicate with the members of the cluster.|yes
`sonar.cluster.node.port`|The Hazelcast port for communication with each application member of the cluster. Default: `9003`|no
`sonar.cluster.node.web.port`|The Hazelcast port for communication with the WebServer process. Port must be accessible to all other application nodes. If not specified, a dynamic port will be chosen and all ports must be open among the nodes.|no
`sonar.cluster.node.ce.port`|The Hazelcast port for communication with the ComputeEngine process. Port must be accessible to all other application nodes. If not specified, a dynamic port will be chosen and all ports must be open among the nodes.|no
`sonar.cluster.search.hosts`|Comma-delimited list of search hosts in the cluster. The list can contain either the host or the host and port, but not both. The item format is `sonar.cluster.node.search.host` for host only or`sonar.cluster.node.search.host:sonar.cluster.node.search.port` for host and port.| yes
`sonar.auth.jwtBase64Hs256Secret`|Required for authentication with multiple web servers. It is used to keep user sessions opened when they are redirected from one web server to another by the load balancer. See _$SONARQUBE_HOME/conf/sonar.properties_) for details about how to generate this secret key.| yes

### Search nodes
Property  | Description | Default | Required 
---|---|---|---
`sonar.cluster.node.search.host`|Elasticsearch host of the current node used for HTTP communication between search and application nodes. IP must be accessible to all application nodes.|`127.0.0.1`|yes
`sonar.cluster.node.search.port`|Elasticsearch port of the current node used for HTTP communication between search and application nodes. Port must be accessible to all application nodes.|`9001`|yes
`sonar.cluster.es.hosts`|Comma-delimited list of search hosts in the cluster. The list can contain either the host or the host and port but not both. The item format is `sonar.cluster.node.es.host` for host only or`sonar.cluster.node.es.host:sonar.cluster.node.es.port` for host and port.| |yes
`sonar.cluster.node.es.host`|Elasticsearch host of the current node used by Elasticsearch internal communication to form a cluster (TCP transport).|localhost|yes
`sonar.cluster.node.es.port`|Elasticsearch port of the current node used by Elasticsearch internal communication to form a cluster (TCP transport). Port must be accessible to all other search nodes|9002| yes
`sonar.search.initialStateTimeout`|The timeout for the Elasticsearch nodes to elect a primary node. The default value will be fine in most cases, but in a situation where startup is failing because of a timeout, this may need to be adjusted. The value must be set in the format: `{integer}{timeunit}`. Valid `{timeunit}` values are: `ms` (milliseconds); `s` (seconds); `m` (minutes); `h` (hours); `d` (days); `w` (weeks)|cluster: 120s; standalone: 30s|no

### Elasticsearch authentication

[[info]]
| This configuration is optional. To secure access to your setup, you may want to first limit access to the nodes in your network. Elasticsearch authentication just adds another layer of security.

[[warning]]
| When creating the PKCS#12 container, make sure it is created with an algorithm that is readable by Java 11.

For Elasticsearch authentication, the following properties need to be configured on specific nodes:

#### Application nodes
Property  | Description | Default | Required
---|---|---|---
`sonar.cluster.search.password`|Password for Elasticsearch built-in user (elastic) which will be used on the client site. If provided, it enables authentication. If this property is set, `sonar.cluster.search.password` on the search nodes must also be set to exact same value.| |no

#### Search nodes
Property  | Description | Default | Required
---|---|---|---
`sonar.cluster.search.password`|Password for Elasticsearch built-in user (elastic) which will be set in ES. If provided, it enables authentication, and the instance will require additional properties to be set. If this property is set, `sonar.cluster.search.password` on the application nodes must also be set to exact same value.| |no
`sonar.cluster.es.ssl.keystore`|File path to a keystore in PKCS#12 format. The user running SonarQube must have READ permission to that file. Required if password provided.| |no
`sonar.cluster.es.ssl.truststore`|File path to a truststore in PKCS#12 format. The user running SonarQube must have READ permission to that file. Required if password provided.| |no
`sonar.cluster.es.ssl.keystorePassword`|Password to the keystore.| |no
`sonar.cluster.es.ssl.truststorePassword`|Password to the truststore.| | no

When you're using the SonarSource Docker images, the truststore/keystore should be provided as volumes. 
On Kubernetes, you need to create a new Secret from the truststore/keystore and provide the name to the Helm chart.

## Secure your Network

To further lock down the communication in between the nodes in your SonarQube Cluster, you can define the following network rules:

Protocol | Source | Destination | Port | default
---|---|---|---|---
TCP | Reverse Proxy | App Node | `sonar.web.port` | 9000
TCP | App Node | Search Node | `sonar.cluster.node.search` | 9001
TCP | Search Node | Search Node | `sonar.cluster.node.es.port` | 9002
TCP | App Node | App Node | `sonar.cluster.node.port` | 9003

you can further segrement your network configuration if you specify a frontend, a backend and a search network.  

Network | Parameter | Description
---|---|---
Frontend | `sonar.web.host` 				| Frontend HTTP Network
Backend  | `sonar.cluster.node.host` 		| Backend App to App Network
Backend  | `sonar.cluster.search.hosts`     | Backend App to Search Network
Search   | `sonar.cluster.node.search.host` | Backend Search to Search Network

## Limitations
* Cluster downtime is required for SonarQube upgrades or plugin installations.
* All application nodes must be stopped when installing, uninstalling, or upgrading a plugin.
* Plugins are not shared, meaning if you install/uninstall/upgrade a given plugin on one application node, you need to perform the same actions on the other application node.
* There is no way to perform actions on the cluster from a central app - all operations must be done manually on each node of the cluster.

## Frequently Asked Questions

### Does Elasticsearch discover automatically other ES nodes? 
No. Multicast is disabled. All hosts (IP+port) must be listed.

### Can different nodes run on the same machine? 
Yes, but it's best to have one machine for each node to be resilient to failures. To maintain an even higher level of availability, each of your three search nodes can be located in a separate availability zone *within the same region*.

### Can the members of a cluster be discovered automatically? 
No, all nodes must be configured in _$SONARQUBE_HOME/conf/sonar.properties_

### My keystore/truststore cannot be read by SonarQube
Make sure that the keystore/truststore in question was generated with an algorithm that is known to Java 11. See [JDK-8267599](https://bugs.openjdk.java.net/browse/JDK-8267599) for reference