---
title: Install the Server as a Cluster
url: /setup/install-cluster/
---

<!-- sonarqube -->

_Running SonarQube as a Cluster is only possible with a [Data Center Edition](https://www.sonarsource.com/plans-and-pricing/data-center/)_.

The Data Center Edition allows SonarQube to run in a clustered configuration to make it resilient to failures.

## Overview

The default configuration for the Data Center Edition comprises five servers, a load balancer, and a database server:

- Two application nodes responsible for handling web requests from users (WebServer process) and handling analysis reports (ComputeEngine process). You can add application nodes to increase computing capabilities.
- Three search nodes that host the Elasticsearch process that will store data indices. SSDs perform significantly better than HDDs for these nodes.
- A reverse proxy / load balancer to load balance traffic between the two application nodes. The installing organization must supply this hardware or software component.
- PostgreSQL, Oracle, or Microsoft SQL Server database server. This software must be supplied by the installing organization.


With this configuration, one application node and one search node can be lost without impacting users.  Here is a diagram of the default topology:

![DCE Cluster Machines Topology.](/images/cluster-dce.png)

## Requirements

### Network

All servers, including the database server, must be co-located (geographical redundancy is not supported) and have static IP addresses (reference via hostname is not supported).  Network traffic should not be restricted between application and search nodes.

### Servers

You need a minimum of five servers (two application nodes and three search nodes) to form a SonarQube application cluster. You can add application nodes to increase computing capabilities. Servers can be virtual machines; it is not necessary to use physical machines.

The operating system requirements for servers are available on the [Requirements](/requirements/requirements/) page.  All application nodes should be identical in terms of hardware and software. Similarly, all search nodes should be identical to each other. Application and search nodes, however, can differ from one another. Generally, search nodes are configured with more CPU and RAM than application nodes.

Here are the machines we used to perform our validation with a 200M issues database. You can use this as a minimum recommendation to build your cluster.

- App Node made of [Amazon EC2 m4.xlarge](https://aws.amazon.com/ec2/instance-types/): 4 vCPUs, 16GB RAM
- Search Node made of [Amazon EC2 m4.2xlarge](https://aws.amazon.com/ec2/instance-types/): 8 vCPUs, 32GB RAM - 16GB allocated to Elasticsearch. SSDs perform significantly better than HDDs for these nodes.

### Database Server

Supported database systems are available on the [Requirements](/requirements/requirements/) page.

### Load Balancer

SonarSource does not provide specific recommendations for reverse proxy / load balancer or solution-specific configuration.  The general requirements to use with SonarQube Data Center Edition are:

- Ability to balance HTTP requests (load) between the application nodes configured in the SonarQube cluster.
- If terminating HTTPS, meets the requirements set out in [Securing SonarQube Behind a Proxy](/setup/operate-server/).
- No requirement to preserve or sticky sessions; this is handled by the built-in JWT mechanism.

### License

You need a dedicated license to activate the Data Center Edition. If you don't have one yet, please contact the SonarSource Sales Team.

### Support

Don't start this journey alone!  As a Data Center Edition subscriber, SonarSource will assist with the setup and configuration of your cluster. Get in touch with [SonarSource Support](https://support.sonarsource.com) for help.

## Configuration

Additional parameters are required to activate clustering capabilities and specialize each node. These parameters are in addition to standard configuration properties used in a single-node configuration.

The **sonar.properties** file on each node will be edited to configure the node's specialization. A list of all cluster-specific configuration parameters is available in the [Operate the Cluster](/setup/operate-cluster/) documentation.

Prior to configuration, you will need to generate a value for the `sonar.auth.jwtBase64Hs256Secret` property for the application nodes.  The value is a HS256 key encoded with base64 and will be the same for both nodes.  The following is an example on how to generate this value on a Unix system:

```
echo -n "your_secret" | openssl dgst -sha256 -hmac "your_key" -binary | base64
```

### Sample Configuration

The following example represents the minimal parameters required to configure a SonarQube cluster.  The example assumes:

- The VMs having IP addresses ip1 and ip2 (server1, server2) are application nodes
- The VMs having IP addresses ip3, ip4, and ip5 (server3, server4 and server5) are search nodes

The configuration to be added to sonar.properties for each node is the following:

#### Application Nodes

**server1**
```
...
sonar.cluster.enabled=true
sonar.cluster.hosts=ip1,ip2
sonar.cluster.search.hosts=ip3,ip4,ip5
sonar.cluster.node.type=application
sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
...
```

**server2**
```
...
sonar.cluster.enabled=true
sonar.cluster.hosts=ip1,ip2
sonar.cluster.search.hosts=ip3,ip4,ip5
sonar.cluster.node.type=application
sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
...
```

#### Search Nodes

**server3**
```
...
sonar.cluster.enabled=true
sonar.cluster.search.hosts=ip3,ip4,ip5
sonar.cluster.node.type=search
sonar.search.host=ip3
...
```

**server4**
```
...
sonar.cluster.enabled=true
sonar.cluster.search.hosts=ip3,ip4,ip5
sonar.cluster.node.type=search
sonar.search.host=ip4
...
```

**server5**
```
...
sonar.cluster.enabled=true
sonar.cluster.search.hosts=ip3,ip4,ip5
sonar.cluster.node.type=search
sonar.search.host=ip5
...
```

## Sample Installation Process

The following is an example of the default SonarQube cluster installation process. You need to tailor your installation to the specifics of the target installation environment and the operational requirements of the hosting organization.

**Prepare the cluster environment:**

1. Prepare the cluster environment by setting up the network and provisioning the nodes and load balancer. 
2. Follow the [Installing the Server](/setup/install-server/) documentation to configure the database server.

**Prepare a personalized SonarQube package:**

1. On a single application node of the cluster, download and install SonarQube Data Center Edition, following the usual [Installing the Server](/setup/install-server/) documentation.
2. Add cluster-related parameters to `$SONARQUBE_HOME/conf/sonar.properties`.
3. As the Marketplace is not available in SonarQube Data Center Edition, this is a good opportunity to install additional plugins. Download and place a copy of each plugin JAR in `$SONARQUBE_HOME/extensions/plugins`.  Be sure to check compatibility with your SonarQube version using the [Plugin Version Matrix](https://docs.sonarqube.org/display/PLUG/Plugin+Version+Matrix).
4. Zip the directory `$SONARQUBE_HOME`. This archive is a customized SonarQube Data Center Edition package that can be copied to other nodes.

**Test configuration on a single node:**

1. On the application node where you created your Zip package, comment out all cluster-related parameters in `$SONARQUBE_HOME/conf/sonar.properties`.
2. Configure the load balancer to proxy with single application node.
3. Start server and test access through load balancer.
4. Request license from SonarSource Sales Team.
5. After applying license, you will have a full-featured SonarQube system operating on a single node.

**Deploy SonarQube package on other nodes:**

1. Unzip SonarQube package on the other four nodes.
2. Configure node-specific parameters on all five nodes in `$SONARQUBE_HOME/conf/sonar.properties` and ensure application node-specific and search node-specific parameters are properly set.
3. Start all search nodes.
4. After all search nodes are running, start all application nodes.
5. Configure the load balancer to proxy with both application nodes.

Congratulations, you have a fully-functional SonarQube cluster.  Once these steps are complete, take a break and a coffee, then you can [Operate your Cluster](/setup/operate-cluster/).

<!-- /sonarqube -->
