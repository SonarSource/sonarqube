---
title: Get Started in Two Minutes Guide
url: /setup/get-started-2-minutes/
---

## Installing from a zip file

1. [Download](https://www.sonarqube.org/downloads/) the SonarQube Community Edition

2. Unzip it, let's say in _C:\sonarqube_ or _/opt/sonarqube_

3. Start the SonarQube Server:

```
# On Windows, execute:
C:\sonarqube\bin\windows-x86-xx\StartSonar.bat

# On other operating systems, as a non-root user execute:
/opt/sonarqube/bin/[OS]/sonar.sh console
```

4. Log in to [http://localhost:9000](http://localhost:9000) with System Administrator credentials (admin/admin) and follow the embedded tutorial to analyze your first project.

![](/images/exclamation.svg) This play instance is suitable for demonstration purposes, when you are ready to move to production, take some time to read the [Install the Server](/setup/install-server/) documentation.

## Using Docker

A Docker image of the Community Edition is available on [Docker Hub](https://hub.docker.com/_/sonarqube/), see usage and configuration examples there.

![](/images/exclamation.svg) This instance is suitable for demonstration or testing purposes only.
