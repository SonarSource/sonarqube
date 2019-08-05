---
title: Get Started in Two Minutes Guide
url: /setup/get-started-2-minutes/
---

[[info]]
| This guides shows you how to install a demo instance. When you are ready to move to production, take some time to read the [Install the Server](/setup/install-server/) documentation.

## Installing from a zip file

1. [Download](https://www.sonarqube.org/downloads/) the SonarQube Community Edition.

2. Unzip it, let's say in _C:\sonarqube_ or _/opt/sonarqube_.

3. Start the SonarQube Server:

   ```
   # On Windows, execute:
   C:\sonarqube\bin\windows-x86-xx\StartSonar.bat

   # On other operating systems, as a non-root user execute:
   /opt/sonarqube/bin/[OS]/sonar.sh console
   ```

   ![](/images/info.svg) If your instance fails to start, check your [logs](/setup/troubleshooting/) to find the cause.

4. Log in to [http://localhost:9000](http://localhost:9000) with System Administrator credentials (login=admin, password=admin).

5. Click the **Create new project** button to analyze your first project.

## Using Docker

A Docker image of the Community Edition is available on [Docker Hub](https://hub.docker.com/_/sonarqube/). You can find usage and configuration examples there.