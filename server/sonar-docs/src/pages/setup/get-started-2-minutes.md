---
title: Get Started in Two Minutes Guide
url: /setup/get-started-2-minutes/
---

[[info]]
| This guides shows you how to install a demo instance. When you are ready to move to production, take some time to read the [Install the Server](/setup/install-server/) documentation.

## Installing from a zip file

1. [Download](https://www.sonarqube.org/downloads/) the SonarQube Community Edition.

2. As a **non-`root` user**, unzip it, let's say in _C:\sonarqube_ or _/opt/sonarqube_.

3. As a **non-`root` user**, start the SonarQube Server:

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
Images of the Community, Developer, and Enterprise Editions are available on [Docker Hub](https://hub.docker.com/_/sonarqube/).

Start the server by running:

```console
$ docker run -d --name sonarqube -p 9000:9000 <image_name>
```

By default you can login as `admin` with password `admin`, see the [authentication documentation](https://docs.sonarqube.org/latest/instance-administration/security/).

To analyze a Maven project:

```console
# On Linux:
$ mvn sonar:sonar

# With boot2docker:
$ mvn sonar:sonar -Dsonar.host.url=http://$(boot2docker ip):9000
```

To analyze other types of projects and for more details see [Analyzing Source Code documentation](https://redirect.sonarsource.com/doc/analyzing-source-code.html).
