---
title: Architecture and Integration
url: /architecture/architecture-integration/
---
## Overview
The SonarQube Platform is made of 4 components:  
![SonarQube Platform.](/images/architecture-scanning.png)

1. One SonarQube Server starting 3 main processes:
    * Web Server for developers, managers to browse quality snapshots and configure the SonarQube instance
    * Search Server based on Elasticsearch to back searches from the UI
    * Compute Engine Server in charge of processing code analysis reports and saving them in the SonarQube Database
2. One SonarQube Database to store:
    * the configuration of the SonarQube instance (security, plugins settings, etc.)
    * the quality snapshots of projects, views, etc.
3. Multiple SonarQube Plugins installed on the server, possibly including language, SCM, integration, authentication, and governance plugins
4. One or more SonarScanners running on your Build / Continuous Integration Servers to analyze projects

## Integration
The following schema shows how SonarQube integrates with other ALM tools and where the various components of SonarQube are used.  
![SonarQube Integration.](/images/architecture-integrate.png)

1. Developers code in their IDEs and use [SonarLint](https://sonarlint.org) to run local analysis.
2. Developers push their code into their favourite SCM : git, SVN, TFVC, ...
3. The Continuous Integration Server triggers an automatic build, and the execution of the SonarScanner required to run the SonarQube analysis.
4. The analysis report is sent to the SonarQube Server for processing.
5. SonarQube Server processes and stores the analysis report results in the SonarQube Database, and displays the results in the UI.
6. Developers review, comment, challenge their Issues to manage and reduce their Technical Debt through the SonarQube UI.
7. Managers receive Reports from the analysis.
Ops use APIs to automate configuration and extract data from SonarQube.
Ops use JMX to monitor SonarQube Server.

## About Machines and Locations
* The SonarQube Platform cannot have more than one SonarQube Server and one SonarQube Database.
* For optimal performance, each component (server, database, scanners) should be installed on a separate machine, and the server machine should be dedicated.
* SonarScanners scale by adding machines.
* All machines must be time synchronized.
* The SonarQube Server and the SonarQube Database must be located in the same network
* SonarScanners don't need to be on the same network as the SonarQube Server.
* There is **no communication** between **SonarScanners** and the **SonarQube Database**.
