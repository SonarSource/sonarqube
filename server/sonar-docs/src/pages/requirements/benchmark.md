---
title: Benchmark
url: /requirements/benchmark/
---
## Context
The following figures have been produced on common hardware available in most mid-size companies :

| App ESXi Server Configuration                           | DB ESXi Server Configuration                            |
| ------------------------------------------------------- | ------------------------------------------------------- |
| 2* Intel(R) Xeon(R) CPU E5-2650 v2 @ 2.60GHz (32 VCPUs) | 2* Intel(R) Xeon(R) CPU E5-2650 v2 @ 2.60GHz (32 VCPUs) |
| 128GB RAM                                               | 128GB RAM                                               |
| 1.9 TB volume (RAID5 of 8 Hard Drives of 300GB SAS 15K) | 1.9TB volume (RAID5 of 8 hard drives of 300GB SAS 15k)  |
| 2*1 GB NICs                                             | 2*1 GB NICs                                             |
| ESXi 6.0                                                | ESXi 6.0                                                |

We created 2 VMs, one for SonarQube Server, one for SonarQube Database inside two ESXi 6.0 Servers :

| App VM Server Configuration                                                                        | DB VM Server Configuration                               |
| -------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| 8 VCPUs                                                                                            | 4 VCPUs                                                  |
| 16GB RAM                                                                                           | 8GB RAM                                                  |
| 20 GB storage for system + 200 GB storage for data (LVM) (RAID5 of 8 hard drives of 300GB SAS 15k) | 20 GB storage for system + 200 GB storage for data (LVM) |
| 1GB NIC                                                                                            | 1GB NIC                                                  |
| CentOS 7 64bits                                                                                    | CentOS 7 64bits                                          |
|                                                                                                    | MySQL 5.6.27 (Oracle Community Edition)                  |

## Goals
From SonarQube 5.2+ Source Lines are no longer indexed by Elasticsearch, so quantity of code under analysis analyze will not affect the performance of the SearchServer.

By running this benchmark, we wanted to validate the number of Millions of Issues a given hardware hosting SonarQube can support.
* can SonarQube digest millions of Issues and how many time does it take?
* can we still use the UI of SonarQube with these millions of Issues?

## Results
### Indexation Throughput
Issues Indexation done by the Search Server is not linear:

| Millions of issues | Indexation time (hours) |
| ------------------ | ----------------------- |
| 25                 | 1                       |
| 60                 | 2-3                     |
| 100                | 6-7                     |

### RAM to Allocate to Elasticsearch
RAM to allocate to ElastSearch is linear according to the number of Issues:

| Millions of issues | SearchServer RAM in GB |
| ------------------ | ---------------------- |
| 25                 | 4                      |
| 60                 | 7                      |
| 100                | 12                     |

*Elasticsearch's RAM can be configured in _$SONARQUBE_HOME/conf/sonar.properties_ using: `sonar.search.javaOpts*`

### CPU Allocation
With SonarQube 5.2+, Elasticsearch is configured to use 5 [ElasticSearch Primary Shards](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/glossary.html#glossary-primary-shard). This allows SonarQube to handle by default 50 Millions of Issues on a server having at least 4 CPU Cores dedicated to the SearchServer.

| Millions of issues | \#Shards | \# Cores |                       |
| ------------------ | -------- | -------- | --------------------- |
| 50                 | 5        | 4        | default configuration |
| 30                 | 3        | 2        |                       |

Shard configuration must be adjusted according to the quantity of Issues you have to manage. The rule is to have 1 Shard by block of 10M Issues. So for 100M Issues, you need 10 Shards, so at least 9 Cores.
