---
title: Configure & Operate the Server
url: /setup/operate-server/
---

<!-- sonarqube -->

## Running SonarQube as a Service on Windows

### Install or Uninstall NT Service (may have to run these files via Run As Administrator):

```
%SONARQUBE_HOME%/bin/windows-x86-64/InstallNTService.bat
%SONARQUBE_HOME%/bin/windows-x86-64/UninstallNTService.bat
```

### Start or Stop the Service:

```
%SONARQUBE_HOME%/bin/windows-x86-64/StartNTService.bat
%SONARQUBE_HOME%/bin/windows-x86-64/StopNTService.bat
```
**Note:** `%SONARQUBE_HOME%/bin/windows-x86-64/StopNTService.bat` does a graceful shutdown where no new analysis report processing can start, but the tasks in progress are allowed to finish. The time a stop will take depends on the processing time of the tasks in progress. You'll need to kill all SonarQube processes manually to force a stop.

## Running SonarQube Manually on Linux

### Start or Stop the Instance

```
Start:
$SONAR_HOME/bin/linux-x86-64/sonar.sh start

Graceful shutdown:
$SONAR_HOME/bin/linux-x86-64/sonar.sh stop

Hard stop:
$SONAR_HOME/bin/linux-x86-64/sonar.sh force-stop
```
**Note:** Stop does a graceful shutdown where no new analysis report processing can start, but the tasks in progress are allowed to finish. The time a stop will take depends on the processing time of the tasks in progress. Use force stop for a hard stop. 

## Running SonarQube as a Service on Linux with SystemD

On Unix system using SystemD, you can install SonarQube as a service. You cannot run SonarQube as `root` in 'nix systems. Ideally, you will created a new account dedicated to the purpose of running SonarQube.
Let's suppose:

* The user used to start the service is `sonarqube`
* The group used to start the service is `sonarqube`
* The Java Virtual Machine is installed in `/opt/java/`
* SonarQube has been unzipped into `/opt/sonarqube/`

Then create the file `/etc/systemd/system/sonarqube.service` _based on_ the following 

```
[Unit]
Description=SonarQube service
After=syslog.target network.target

[Service]
Type=simple
User=sonarqube
Group=sonarqube
PermissionsStartOnly=true
ExecStart=/bin/nohup /opt/java/bin/java -Xms32m -Xmx32m -Djava.net.preferIPv4Stack=true -jar /opt/sonarqube/lib/sonar-application-7.4.jar
StandardOutput=syslog
LimitNOFILE=65536
LimitNPROC=8192
TimeoutStartSec=5
Restart=always
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```
**Note**
* Because the sonar-application jar name ends with the version of SonarQube, you will need to adjust the `ExecStart` command accordingly on install and at each upgrade.
* The SonarQube data directory, `/opt/sonarqube/data`, and the extensions directory, `/opt/sonarqube/extensions` should be owned by the `sonarqube` user. As a good practice, the rest should be owned by `root`

Once your `sonarqube.service` file is created and properly configured, run:
```
sudo systemctl enable sonarqube.service
sudo systemctl start sonarqube.service
```

## Running SonarQube as a Service on Linux with initd

The following has been tested on Ubuntu 8.10 and CentOS 6.2.

Create the file /etc/init.d/sonar with this content:

```
#!/bin/sh
#
# rc file for SonarQube
#
# chkconfig: 345 96 10
# description: SonarQube system (www.sonarsource.org)
#
### BEGIN INIT INFO
# Provides: sonar
# Required-Start: $network
# Required-Stop: $network
# Default-Start: 3 4 5
# Default-Stop: 0 1 2 6
# Short-Description: SonarQube system (www.sonarsource.org)
# Description: SonarQube system (www.sonarsource.org)
### END INIT INFO
 
/usr/bin/sonar $*
```

Register SonarQube at boot time (RedHat, CentOS, 64 bit):

```
sudo ln -s $SONAR_HOME/bin/linux-x86-64/sonar.sh /usr/bin/sonar
sudo chmod 755 /etc/init.d/sonar
sudo chkconfig --add sonar
```
Once registration is done, run:
```
sudo service sonar start
```

## Securing the Server Behind a Proxy

This section helps you configure the SonarQube Server if you want to run it behind a proxy. This can be done for security concerns or to consolidate multiple disparate applications.

### Server Configuration

To run the SonarQube server over HTTPS, you must build a standard reverse proxy infrastructure.

The reverse proxy must be configured to set the value `X_FORWARDED_PROTO: https` in each HTTP request header. Without this property, redirection initiated by the SonarQube server will fall back on HTTP.

### Using an Apache Proxy

We assume that you've already installed Apache 2 with module mod_proxy, that SonarQube is running and available on `http://private_sonar_host:sonar_port/` and that you want to configure a Virtual Host for `www.public_sonar.com`.

At this point, edit the HTTPd configuration file for the `www.public_sonar.com` virtual host. Include the following to expose SonarQube via `mod_proxy` at `http://www.public_sonar.com/`:

```
ProxyRequests Off
ProxyPreserveHost On
<VirtualHost *:80>
  ServerName www.public_sonar.com
  ServerAdmin admin@somecompany.com
  ProxyPass / http://private_sonar_host:sonar_port/
  ProxyPassReverse / http://www.public_sonar.com/
  ErrorLog logs/somecompany/sonar/error.log
  CustomLog logs/somecompany/sonar/access.log common
</VirtualHost>
```

Apache configuration is going to vary based on your own application's requirements and the way you intend to expose SonarQube to the outside world. If you need more details about Apache HTTPd and mod_proxy, please see [http://httpd.apache.org](http://httpd.apache.org).

### Using Nginx

We assume that you've already installed Nginx, that you are using a Virtual Host for www.somecompany.com and that SonarQube is running and available on `http://sonarhost:sonarport/`.

At this point, edit the Nginx configuration file. Include the following to expose SonarQube at http://www.somecompany.com/:

```
# the server directive is nginx's virtual host directive
server {
  # port to listen on. Can also be set to an IP:PORT
  listen 80;
 
  # sets the domain[s] that this vhost server requests for
  server_name www.somecompany.com;
 
  location / {
    proxy_pass http://sonarhost:sonarport;
  }
}
```

Nginx configuration will vary based on your own application's requirements and the way you intend to expose SonarQube to the outside world. If you need more details about Nginx, please see [https://www.nginx.com/resources/admin-guide/reverse-proxy/](https://www.nginx.com/resources/admin-guide/reverse-proxy/).

Note that you may need to increase the max URL length since SonarQube requests can have URLs longer than 2048.

### Using IIS

Please see: [http://blog.jessehouwing.nl/2016/02/configure-ssl-for-sonarqube-on-windows.html](http://blog.jessehouwing.nl/2016/02/configure-ssl-for-sonarqube-on-windows.html)

Note that the setup described in this blog post is not approprite for SAML through IIS.

<!-- /sonarqube -->
