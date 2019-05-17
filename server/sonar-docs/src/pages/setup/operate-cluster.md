---
title: Configure & Operate a Cluster
url: /setup/operate-cluster/
---

_High Availability and cluster scalability are features of the [Data Center Edition](https://redirect.sonarsource.com/editions/datacenter.html)._

Once the [SonarQube cluster is installed](/setup/install-cluster/), you have a High Availability configuration that allows your SonarQube instance to stay up and running even if there is a crash or failure in one of the cluster's nodes. Your SonarQube cluster is also scalable, and you can add application nodes to increase your computing capabilities.

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

### Adding an Application Node
To add an Application Node:

1. Configure your new application node in sonar.properties. The following is an example of the configuration to be added to sonar.properties for a sixth application node (server6, ip6) in a cluster with the default five servers:

	**server6**
	```
	...
	sonar.cluster.enabled=true
	sonar.cluster.hosts=ip1,ip2,ip6
	sonar.cluster.search.hosts=ip3,ip4,ip5
	sonar.cluster.node.type=application
	sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
	...
	```
2. Update the configuration of the preexisting nodes to include your new node. 

	While you don't need to restart the cluster after adding a node, you should ensure the configuration is up to date on all of your nodes to avoid issues when you eventually do need to restart.

### Removing an Application Node
When you remove an application node, make sure to update the configuration of the remaining nodes. Much like adding a node, while you don't need to restart the cluster after removing a node, you should ensure the configuration is up to date on all of your nodes to avoid issues when you eventually do need to restart.

## Monitoring
CPU and RAM usage on each node have to be monitored separately with an APM. 

In addition, we provide a Web API _api/system/health_ you can use to validate that all of the nodes in your cluster are operational.  

* GREEN: SonarQube is fully operational
* YELLOW: SonarQube is usable, but it needs attention in order to be fully operational
* RED: SonarQube is not operational

To call it from a monitoring system without having to give admin credentials, it is possible to setup a System Passcode through the property `sonar.web.systemPasscode`. This must be configured in _$SONARQUBE-HOME/conf/sonar.properties_.

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
Start with the [default configuration](/setup/install-cluster/); it's good in most cases. The details below are only needed in specific cases.

[Hazelcast](https://hazelcast.org/) is used to manage the communication between the nodes of the cluster. You don't need to install it yourself, it's provided out of the box.

The following properties may be defined in the _$SONARQUBE-HOME/conf/sonar.properties_ file of each node in a cluster. When defining a property that contains a list of hosts (`*.hosts`) the port is not required if the default port was not overridden in the configuration.

### All nodes
Property | Description | Default | Required | 
---|---|---|---|
`sonar.cluster.enabled`|Set to `true` in each node to activate the cluster mode|`false`|yes
`sonar.cluster.name`|The name of the cluster. **Required if multiple clusters are present on the same network.** For example this prevents mixing Production and Preproduction clusters. This will be the name stored in the Hazelcast cluster and used as the name of the Elasticsearch cluster.|`sonarqube`|no
`sonar.cluster.search.hosts`|Comma-delimited list of search hosts in the cluster. Each item in the list must contain the port if the default `sonar.search.port` value is not used. Item format is `sonar.search.host` or `sonar.search.host:sonar.search.port`.| |yes
`sonar.cluster.node.name`|The name of the node that is used on Elasticsearch and stored in Hazelcast member attribute (NODE_NAME) for sonar-application|`sonarqube-{UUID}`|no
`sonar.cluster.node.type`|Type of node: either `application` or `search`| |yes
`sonar.cluster.node.host`|IP address of the network card that will be used by Hazelcast to communicate with the members of the cluster. If not specified, the first interface will be chosen (note that loopback interfaces won't be selected)| |no


### Application nodes
Property  | Description | Required 
---|---|---|---
`sonar.cluster.hosts`|Comma-delimited list of all **application** hosts in the cluster. This value must contain **only application hosts**. Each item in the list must contain the port if the default `sonar.cluster.node.port` value is not used. Item format is `sonar.cluster.node.host` or `sonar.cluster.node.host:sonar.cluster.node.port`.|yes
`sonar.cluster.node.port`|The Hazelcast port for communication with each application member of the cluster. Default: `9003`|no|
`sonar.cluster.node.web.port`|Hazelcast port for communication with the ComputeEngine process. Port must be accessible to all other search and application nodes. If not specified, a dynamic port will be chosen and all ports must be open among the nodes.|no
`sonar.cluster.node.ce.port`|Hazelcast port for communication with the WebServer process. Port must be accessible to all other search and application nodes. If not specified, a dynamic port will be chosen and all ports must be open among the nodes.|no
`sonar.auth.jwtBase64Hs256Secret`|Required for authentication with multiple web servers. It is used to keep user sessions opened when they are redirected from one web server to another by the load balancer. See _$SONARQUBE-HOME/conf/sonar.properties_) for details about how to generate this secret key.| yes

### Search nodes
Property  | Description | Default | Required 
---|---|---|---
`sonar.search.host`|Listening IP. IP must be accessible to all other search and application nodes.|`127.0.0.1`|yes
`sonar.search.port`|Listening port. Port must be accessible to all other search and application nodes.|`9001`|yes
`sonar.search.initialStateTimeout`|The timeout for the Elasticsearch nodes to elect a master node. The default value will be fine in most cases, but in a situation where startup is failing because of a timeout, this may need to be adjusted. The value must be set in the format: `{integer}{timeunit}`. Valid `{timeunit}` values are: `ms` (milliseconds); `s` (seconds); `m` (minutes); `h` (hours); `d` (days); `w` (weeks)|cluster: 120s; standalone: 30s|no

## Limitations
* Cluster downtime is required for SonarQube upgrades or plugin installations.
* All application nodes must be stopped when installing, uninstalling or upgrading a plugin.
* Plugins are not shared, it means if you install/uninstall/upgrade a given plugin in one application node, you need to perform the same actions on the other application node.
* There is no way to perform actions on the cluster from a central app - all operations must be done manually on each node of the cluster.


## Frequently Asked Questions
### Does Elasticsearch discover automatically other ES nodes? 
No. Multicast is disabled. All hosts (IP+port) must be listed.
### Can different nodes run on the same machine? 
Yes, but the best is to have 5 machines to be really resilient to failures.
### Can the members of a cluster be discovered automatically? 
No, all nodes must be configured in _$SONARQUBE-HOME/conf/sonar.properties_
