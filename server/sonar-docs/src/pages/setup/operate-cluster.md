---
title: Configure & Operate a Cluster
url: /setup/operate-cluster/
---

_High Availability is a feature of the [Data Center Edition](https://redirect.sonarsource.com/editions/datacenter.html)._



Once the the [SonarQube cluster is installed](/setup/install-cluster/), you have a High Availability configuration that will allow your SonarQube instance to stay up and running even if there is a crash or failure in one of the nodes of the cluster.

## Start/Stop/Upgrade the Cluster
### Start the Cluster
1. Start the search nodes
1. Start the application nodes

### Stop the Cluster
1. Stop the application nodes
1. Stop the search nodes

### Upgrade SonarQube
1. Stop the cluster
1. Upgrade SonarQube on all nodes (app part, plugins, JDBC driver if required) following the usual Upgrade procedure but without triggering the /setup phase
1. Once all nodes have the same binaries: start the cluster 
1. At this point only one of the application nodes is up. Try to access `node_ip:port/setup` on each server, and trigger the setup operation on the one that responds.

## Install/Upgrade a Plugin
1. Stop the cluster
1. Upgrade the plugin on all nodes
Start the cluster

## Monitoring 
CPU and RAM usage on each node have to be monitored separately with an APM. 

In addition, we provide a Web API _api/system/health_ you can use to validate that all of the nodes in your cluster are operational.  

* GREEN: SonarQube is fully operational
* YELLOW: SonarQube is usable, but it needs attention in order to be fully operational
* RED: SonarQube is not operational

To call it from a monitoring system without having to give admin credentials, it is possible to setup a System Passcode through the property `sonar.web.systemPasscodez. This must be configured in _$SONARQUBE-HOME/conf/sonar.properties_.

### Manually Check the Status of your SQ Cluster from the UI
In the System Info page, you can check whether your cluster is running safely (green) or has some nodes with problems (orange or red).

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
`sonar.cluster.hosts`|Comma-delimited list of all **application** hosts in the cluster. This value must contain **only application hosts**. Each item in the list must contain the port if the default `sonar.cluster.node.port` value is not used. Item format is `sonar.cluster.node.host` or `sonar.cluster.node.host:sonar.cluster.node.port`.| |yes
`sonar.cluster.search.hosts`|Comma-delimited list of search hosts in the cluster. Each item in the list must contain the port if the default `sonar.search.port` value is not used. Item format is `sonar.search.host` or `sonar.search.host:sonar.search.port`.| |yes
`sonar.cluster.node.name`|The name of the node that is used on Elasticsearch and stored in Hazelcast member attribute (NODE_NAME) for sonar-application|`sonarqube-{UUID}`|no
`sonar.cluster.node.type`|Type of node: either `application` or `search`| |yes
`sonar.cluster.node.host`|IP address of the network card that will be used by Hazelcast to communicate with the members of the cluster. If not specified, the first interface will be chosen (note that loopback interfaces won't be selected)| |no


### Application nodes
Property  | Description | Required 
---|---|---|---
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

