---
title: Hardware Recommendations
url: /requirements/hardware-recommendations/
---
## Database
We recommend that for large instances, the database used by SonarQube is hosted on a machine that is physically separate from SonarQube Server but close to it on the network.

### Oracle
In case your SonarQube Server is running on Linux and you are using Oracle, the Oracle JDBC Driver may be blocked due to /dev/random. See [this Oracle article](http://www.usn-it.de/index.php/2009/02/20/oracle-11g-jdbc-driver-hangs-blocked-by-devrandom-entropy-pool-empty/) for more details about this problem.

To avoid it, you may want to add this JVM parameter to your SonarQube Web Server (`sonar.web.javaOpts`) configuration :
 ```
 -Djava.security.egd=file:///dev/urandom
 ```

## Elasticsearch (ES)
* [Elasticsearch](https://www.elastic.co/) is used by SonarQube in the background in the SearchServer process. To ensure good performance of your SonarQube, you need to follow these recommendations that are linked to ES usage.

### Disk
* Free disk space is an absolute requirement. ES implements a safety mechanism to prevent the disk from being flooded with index data that locks all indices in read-only mode when a 95% disk usage watermark is reached. For information on recovering from ES read-only indices, see the [Troubleshooting](/setup/troubleshooting/) page.
* Disk can easily become the bottleneck of ES. If you can afford SSDs, they are by far superior to any spinning media. SSD-backed nodes see boosts in both query and indexing performance. If you use spinning media, try to obtain the fastest disks possible (high-performance server disks 15k RPM drives).
* Using RAID 0 is an effective way to increase disk speed, for both spinning disks and SSD. There is no need to use mirroring or parity variants of RAID because of Elasticsearch replicas and database primary storage.
* Do not use remote-mounted storage, such as NFS, SMB/CIFS or network-attached storage (NAS). They are often slower, display larger latencies with a wider deviation in average latency, and are a single point of failure.

**Advanced**
* If you are using SSD, make sure your OS I/O Scheduler is configured correctly. When you write data to disk, the I/O Scheduler decides when that data is actually sent to the disk. The default under most *nix distributions is a scheduler called cfq (Completely Fair Queuing). This scheduler allocates "time slices" to each process, and then optimizes the delivery of these various queues to the disk. It is optimized for spinning media: the nature of rotating platters means it is more efficient to write data to disk based on physical layout. This is very inefficient for SSD, however, since there are no spinning platters involved. Instead, deadline or noop should be used. The deadline scheduler optimizes based on how long writes have been pending, while noop is just a simple FIFO queue. This simple change can have dramatic impacts.
* If SQ home directory is located on a slow disk, then the property `sonar.path.data` can be used to move data to a faster disk (RAID 0 local SSD for instance).

### Memory
* Machine available memory for OS must be at least the Elasticsearch heap size. The reason is that Lucene (used by ES) is designed to leverage the underlying OS for caching in-memory data structures. That means that by default OS must have at least 1Gb of available memory.
* Don't allocate more than 32GB. See the following Elasticsearch articles for more details:
  * [Elasticsearch Guide: Heap Sizing](https://www.elastic.co/guide/en/elasticsearch/guide/current/heap-sizing.html)
  * [A Heap of Trouble](https://www.elastic.co/blog/a-heap-of-trouble)
  * [Elasticsearch Reference: JVM heap size](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/advanced-configuration.html#set-jvm-heap-size)

### CPU
* If you need to choose between faster CPUs or more cores, then choose more cores. The extra concurrency that multiple cores offer will far outweigh a slightly faster clock speed.
* By nature, data is distributed on multiple nodes, so execution time depends on the slowest node. It's better to have multiple medium boxes than one fast and one slow.
