---
title: Install the Server as a Cluster
url: /setup/install-cluster/
---

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

All servers, including the database server, must be located within the same region. 

All application and search nodes should have static IP addresses (reference via hostname is not supported). Network traffic should not be restricted between application and search nodes.

### Servers

You need a minimum of five servers (two application nodes and three search nodes) to form a SonarQube application cluster. Servers can be virtual machines; it is not necessary to use physical machines. You can also add application nodes to increase computing capabilities. 

The operating system requirements for servers are available on the [Requirements](/requirements/requirements/) page. 

All application nodes should be identical in terms of hardware and software. Similarly, all search nodes should be identical to each other. Application and search nodes, however, can differ from one another. Generally, search nodes are configured with more CPU and RAM than application nodes.

Search nodes can be located in different availability zones, but they must be in the same region. In this case, each search node should be located in a separate availability zone to maintain availability in the event of a failure in one zone.

#### **Example Machines**
Here are the machines we used to perform our validation with a 200M issues database. You can use this as a minimum recommendation to build your cluster.

- App Node made of [Amazon EC2 m4.xlarge](https://aws.amazon.com/ec2/instance-types/): 4 vCPUs, 16GB RAM
- Search Node made of [Amazon EC2 m4.2xlarge](https://aws.amazon.com/ec2/instance-types/): 8 vCPUs, 32GB RAM - 16GB allocated to Elasticsearch. SSDs perform significantly better than HDDs for these nodes.

### Database Server

Supported database systems are available on the [Requirements](/requirements/requirements/) page.

### Load Balancer

SonarSource does not provide specific recommendations for reverse proxy / load balancer or solution-specific configuration. The general requirements for SonarQube Data Center Edition are:

- Ability to balance HTTP requests (load) between the application nodes configured in the SonarQube cluster.
- If terminating HTTPS, meets the requirements set out in [Securing SonarQube Behind a Proxy](/setup/operate-server/).
- No requirement to preserve or sticky sessions; this is handled by the built-in JWT mechanism.
- Ability to check for node health for routing

#### Example with HAProxy

```
frontend http-in
    bind *:80
    bind *:443 ssl crt /etc/ssl/private/<server_certificate>
    http-request redirect scheme https unless { ssl_fc }
    default_backend sonarqube_server
backend sonarqube_server
    balance roundrobin
    http-request set-header X-Forwarded-Proto https
    option httpchk GET /api/system/status
    http-check expect rstring UP|DB_MIGRATION_NEEDED|DB_MIGRATION_RUNNING
    default-server check maxconn 200
    server node1 <server_endpoint_1>
    server node2 <server_endpoint_2> 
```

### License

You need a dedicated license to activate the Data Center Edition. If you don't have one yet, please contact the SonarSource Sales Team.

### Support

Don't start this journey alone!  As a Data Center Edition subscriber, SonarSource will assist with the setup and configuration of your cluster. Get in touch with [SonarSource Support](https://support.sonarsource.com) for help.

## Installing SonarQube from the ZIP file

Additional parameters are required to activate clustering capabilities and specialize each node. These parameters are in addition to standard configuration properties used in a single-node configuration.

The **sonar.properties** file on each node will be edited to configure the node's specialization. A list of all cluster-specific configuration parameters is available in the [Operate the Cluster](/setup/operate-cluster/) documentation.

Prior to configuration, you will need to generate a value for the `sonar.auth.jwtBase64Hs256Secret` property for the application nodes.  The value is a HS256 key encoded with base64 and will be the same for both nodes.  The following is an example on how to generate this value on a Unix system:

```
echo -n "your_secret" | openssl dgst -sha256 -hmac "your_key" -binary | base64
```

### Sample Configuration

The following example represents a sample configuration of a SonarQube cluster.  The example assumes:

- The VMs having IP addresses ip1 and ip2 (server1, server2) are application nodes
- The VMs having IP addresses ip3, ip4, and ip5 (server3, server4 and server5) are search nodes

The configuration to be added to sonar.properties for each node is the following:

#### Application Nodes

**server1**
```
...
sonar.cluster.enabled=true
sonar.cluster.node.type=application
sonar.cluster.node.host=ip1
sonar.cluster.node.port=9003
sonar.cluster.hosts=ip1,ip2
sonar.cluster.search.hosts=ip3:9001,ip4:9001,ip5:9001
sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
...
```

**server2**
```
...
sonar.cluster.enabled=true
sonar.cluster.node.type=application
sonar.cluster.node.host=ip2
sonar.cluster.node.port=9003
sonar.cluster.hosts=ip1,ip2
sonar.cluster.search.hosts=ip3:9001,ip4:9001,ip5:9001
sonar.auth.jwtBase64Hs256Secret=YOURGENERATEDSECRET
...
```

#### Search Nodes

**server3**
```
...
sonar.cluster.enabled=true
sonar.cluster.node.type=search
sonar.cluster.node.search.host=ip3
sonar.cluster.node.search.port=9001
sonar.cluster.node.es.host=ip3
sonar.cluster.node.es.port=9002
sonar.cluster.es.hosts=ip3:9002,ip4:9002,ip5:9002
...
```

**server4**
```
...
sonar.cluster.enabled=true
sonar.cluster.node.type=search
sonar.cluster.node.search.host=ip4
sonar.cluster.node.search.port=9001
sonar.cluster.node.es.host=ip4
sonar.cluster.node.es.port=9002
sonar.cluster.es.hosts=ip3:9002,ip4:9002,ip5:9002
...
```

**server5**
```
...
sonar.cluster.enabled=true
sonar.cluster.node.type=search
sonar.cluster.node.search.host=ip5
sonar.cluster.node.search.port=9001
sonar.cluster.node.es.host=ip5
sonar.cluster.node.es.port=9002
sonar.cluster.es.hosts=ip3:9002,ip4:9002,ip5:9002
...
```

### Sample Installation Process

The following is an example of the default SonarQube cluster installation process. You need to tailor your installation to the specifics of the target installation environment and the operational requirements of the hosting organization.

**Prepare the cluster environment:**

1. Prepare the cluster environment by setting up the network and provisioning the nodes and load balancer. 
2. Follow the [Installing the Server](/setup/install-server/) documentation to configure the database server.

**Prepare a personalized SonarQube package:**

1. On a single application node of the cluster, download and install SonarQube Data Center Edition, following the usual [Installing the Server](/setup/install-server/) documentation.
2. Add cluster-related parameters to `$SONARQUBE_HOME/conf/sonar.properties`.
3. This is also a good opportunity to install plugins. Download and place a copy of each plugin JAR in `$SONARQUBE_HOME/extensions/plugins`.  Be sure to check compatibility with your SonarQube version using the [Plugin Version Matrix](/instance-administration/plugin-version-matrix/).
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

## Installing SonarQube from the Docker Image

You can also install a cluster using our docker images. The general setup is the same but is shifted to a docker specific terminology.

## Requirements

### Network

All containers should be in the same network. This includes search and application nodes.
For the best performance, it is advised to check for low latency between the database and the cluster nodes.

### Limits

The limits of each container depend on the workload that each container has. A good starting point would be:

* cpus: 0.5  
* mem_limit: 4096M  
* mem_reservation: 1024M  

The 4Gb mem_limit should not be lower as this is the minimal value for Elasticsearch.

### Scalability

Application nodes can be scaled using replicas. This is not the case for the Search nodes as Elasticsearch will not become ready. See the [Configure and Operate a Cluster](/setup/operate-cluster/) for more information.

### Volumes
You'll use the following volumes in your configuration:

- `sonarqube_data` – In the Docker Compose configuration example in the following section, volumes are shared between replicas in the application nodes, so you don't need a `sonarqube_data` volume on your application nodes. In the search nodes, the `sonarqube_data` volume contains the Elasticsearch data and helps reduce startup time, so we recommend having a `sonarqube_data` volume on each search node.
- `sonarqube_extensions` – For application nodes, we recommend sharing a common `sonarqube_extensions` volume which contains any plugins you install and the Oracle JDBC driver if necessary.
- `sonarqube_logs` – For both application and search nodes, we recommend sharing a common `sonarqube_logs` volume which contains SonarQube logs. The volume will be populated with a new folder depending on the container's hostname and all logs of this container will be put into this folder. This behavior also happens when a custom log path is specified via the [Docker Environment Variables](/setup/environment-variables/).

## Example Docker Compose configuration

Click the heading below to expand the docker-compose.yml file example. 

[[info]]
| The example below will use the latest version of the SonarQube Docker image. If want to use the LTS version of SonarQube, you need to update the example with the `sonarqube:lts-datacenter-app` and `sonarqube:lts-datacenter-search` image tags.

[[collapse]]
| ## docker-compose.yml file example
|
| ```yaml
|version: "3"
|
|services:
|  sonarqube:
|    image: sonarqube:datacenter-app
|    depends_on:
|      - db
|      - search-1
|      - search-2
|      - search-3
|    networks:
|      - sonar-network
|    deploy:
|      replicas: 2
|    environment:
|      SONAR_JDBC_URL: jdbc:postgresql://db:5432/sonar
|      SONAR_JDBC_USERNAME: sonar
|      SONAR_JDBC_PASSWORD: sonar
|      SONAR_WEB_PORT: 9000
|      SONAR_CLUSTER_SEARCH_HOSTS: "search-1,search-2,search-3"
|      SONAR_CLUSTER_HOSTS: "sonarqube"
|      SONAR_AUTH_JWTBASE64HS256SECRET: "dZ0EB0KxnF++nr5+4vfTCaun/eWbv6gOoXodiAMqcFo="
|      VIRTUAL_HOST: sonarqube.dev.local
|      VIRTUAL_PORT: 9000
|    volumes:
|      - sonarqube_extensions:/opt/sonarqube/extensions
|      - sonarqube_logs:/opt/sonarqube/logs
|  search-1:
|    image: sonarqube:datacenter-search
|    hostname: "search-1"
|    depends_on:
|      - db
|    networks:
|      - sonar-network
|    environment:
|      SONAR_JDBC_URL: jdbc:postgresql://db:5432/sonar
|      SONAR_JDBC_USERNAME: sonar
|      SONAR_JDBC_PASSWORD: sonar
|      SONAR_CLUSTER_ES_HOSTS: "search-1,search-2,search-3"
|      SONAR_CLUSTER_NODE_NAME: "search-1"
|    volumes:
|      - search-data-1:/opt/sonarqube/data
|  search-2:
|    image: sonarqube:datacenter-search
|    hostname: "search-2"
|    depends_on:
|      - db
|    networks:
|      - sonar-network
|    environment:
|      SONAR_JDBC_URL: jdbc:postgresql://db:5432/sonar
|      SONAR_JDBC_USERNAME: sonar
|      SONAR_JDBC_PASSWORD: sonar
|      SONAR_CLUSTER_ES_HOSTS: "search-1,search-2,search-3"
|      SONAR_CLUSTER_NODE_NAME: "search-2"
|    volumes:
|      - search-data-2:/opt/sonarqube/data
|  search-3:
|    image: sonarqube:datacenter-search
|    hostname: "search-3"
|    depends_on:
|      - db
|    networks:
|      - sonar-network
|    environment:
|      SONAR_JDBC_URL: jdbc:postgresql://db:5432/sonar
|      SONAR_JDBC_USERNAME: sonar
|      SONAR_JDBC_PASSWORD: sonar
|      SONAR_CLUSTER_ES_HOSTS: "search-1,search-2,search-3"
|      SONAR_CLUSTER_NODE_NAME: "search-3"
|    volumes:
|      - search-data-3:/opt/sonarqube/data
|  db:
|    image: postgres:12
|    networks:
|      - sonar-network
|    environment:
|      POSTGRES_USER: sonar
|      POSTGRES_PASSWORD: sonar
|    volumes:
|      - postgresql:/var/lib/postgresql
|      - postgresql_data:/var/lib/postgresql/data
|  proxy:
|    image: jwilder/nginx-proxy
|    ports:
|      - "80:80"
|    volumes:
|      - /var/run/docker.sock:/tmp/docker.sock:ro
|    networks:
|      - sonar-network
|      - sonar-public
|
|networks:
|  sonar-network:
|    ipam:
|      driver: default
|      config:
|        - subnet: 172.28.2.0/24
|  sonar-public:
|    driver: bridge
|
|volumes:
|  sonarqube_extensions:
|  sonarqube_logs:
|  search-data-1:
|  search-data-2:
|  search-data-3:
|  postgresql:
|  postgresql_data:
| ```

## Next Steps
Once you've complete these steps, check out the [Operate your Cluster](/setup/operate-cluster/) documentation.
