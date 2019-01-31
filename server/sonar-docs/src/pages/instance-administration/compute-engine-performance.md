---
title: Compute Engine Performance
url: /instance-administration/compute-engine-performance/
---

_The ability to manage Compute Engine performance is available as part of [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._


### How can I get analyses through the Compute Engine Queue faster?
If analyses are taking too long to process, it may be that you need to increase the number of Compute Engine (CE) workers (**[Administration > Projects > Background Tasks > Number of Workers](/#sonarqube-admin#/admin/background_tasks)**). 

There are two cases to consider:

1. slowness comes from the fact that the queue is often full of pending tasks
1. individual tasks take a long time to process

In the first case, increasing the number of workers could help. The second case should be carefully evaluated. In either case, when considering increasing the number of CE workers, two questions should be answered.

* does my infrastructure allow me to increase the number of workers?
* to what extent should I increase the number of workers? I.E. What number should I configure?

Increasing the number of workers will increase the stress on the resources consumed by the Compute Engine. Those resources are:

* the DB
* disk I/O
* network
* heap
* CPU

Of those, only the last two are internal to the CE.

If slowness comes from any of the external resources (DB, disk I/O, network), then increasing the number of workers could actually slow the processing of individual reports (think of two people trying to go through a door at  the same time). However, if your slowness is caused by large individual analysis reports hogging the CE worker for extended periods of time, then enabling parallel processing by adding another worker could help. But if you do, you need to take a look at the internal resources.

CE workers are not CPU-intensive and memory use depends entirely on the project that was analyzed. Some need a lot of memory, others don't. But with multiple CE workers, you should increase CE heap size by a multiple of the number of workers. The same logic applies to CPU: if running with one worker consumes up to Y% of CPU, then you should plan for Z workers requiring Y*Z% of CPU.

To accurately diagnose your situation, monitor network latency, the I/O of the SonarQube instance, and the database CPU and memory usage to evaluate whether slowness is mainly/mostly/only related to external resources. 
