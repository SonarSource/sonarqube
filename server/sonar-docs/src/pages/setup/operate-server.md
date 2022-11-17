---
title: Operating the Server
url: /setup/operate-server/
---

## Running SonarQube as a Service on Windows
### Install or Uninstall SonarQube as a Service

```
> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat install
> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat uninstall
```

### Start or Stop the Service

```
> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat start
```
**Note:** By default, the service will use the Java executable available on the Windows PATH. This setting can be changed by setting the environmental variable SONAR_JAVA_PATH. See more in [Adjusting the Java Installation](https://docs.sonarqube.org/latest/setup/install-server/).
```
> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat stop
```
**Note:** `> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat stop` does a graceful shutdown where no new analysis report processing can start, but the tasks in progress are allowed to finish. The time a stop will take depends on the processing time of the tasks in progress. You'll need to kill all SonarQube processes manually to force a stop.

### Service Status
Check if the SonarQube service is running
```
> %SONAR_HOME%\bin\windows-x86-64\SonarService.bat status
```

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

On a Unix system using SystemD, you can install SonarQube as a service. You cannot run SonarQube as `root` in 'nix systems. Ideally, you will have created a new account dedicated to the purpose of running SonarQube.
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
ExecStart=/bin/nohup /opt/java/bin/java -Xms32m -Xmx32m -Djava.net.preferIPv4Stack=true -jar /opt/sonarqube/lib/sonar-application-8.5.jar
StandardOutput=syslog
LimitNOFILE=131072
LimitNPROC=8192
TimeoutStartSec=5
Restart=always
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```
**Note**
* Because the sonar-application jar name ends with the version of SonarQube, you will need to adjust the `ExecStart` command accordingly on install and at each upgrade.
* All SonarQube directories should be owned by the `sonarqube` user.

Once your `sonarqube.service` file is created and properly configured, run:
```
sudo systemctl enable sonarqube.service
sudo systemctl start sonarqube.service
```

## Running SonarQube as a Service on Linux with initd

The following has been tested on Ubuntu 20.04 and CentOS 6.2.

You cannot run SonarQube as `root` in 'nix systems. Ideally, you will have created a new account dedicated to the purpose of running SonarQube. Let's suppose the user used to start the service is `sonarqube`. Then create the file `/etc/init.d/sonar` _based on_ the following:

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

su sonarqube -c "/usr/bin/sonar $*"
```

Register SonarQube at boot time (RedHat, CentOS, 64 bit):

```
sudo ln -s $SONAR_HOME/bin/linux-x86-64/sonar.sh /usr/bin/sonar
sudo chmod 755 /etc/init.d/sonar
sudo chkconfig --add sonar
```
Register SonarQube at boot time (Ubuntu, 64 bit):

```
sudo ln -s $SONAR_HOME/bin/linux-x86-64/sonar.sh /usr/bin/sonar
sudo chmod 755 /etc/init.d/sonar
sudo update-rc.d sonar defaults
```
Once registration is done, run:
```
sudo service sonar start
```

## Securing the Server Behind a Proxy

This section helps you configure the SonarQube Server if you want to run it behind a proxy. This can be done for security concerns or to consolidate multiple disparate applications. To run the SonarQube server over HTTPS, see the HTTPS Configuration section below.

[[warning]]
|For security reasons, we recommend only giving external access to the main port.

### Using an Apache Proxy

We assume that you've already installed Apache 2 with module mod\_proxy, that SonarQube is running and available on `http://private_sonar_host:sonar_port/`, and that you want to configure a Virtual Host for `www.public_sonar.com`.

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

Apache configuration is going to vary based on your own application's requirements and the way you intend to expose SonarQube to the outside world. If you need more details about Apache HTTPd and mod\_proxy, please see [http://httpd.apache.org](http://httpd.apache.org).

### Using Nginx

We assume that you've already installed Nginx, that you are using a Virtual Host for www.somecompany.com and that SonarQube is running and available on `http://sonarhost:sonarport/`.

At this point, edit the Nginx configuration file. Include the following to expose SonarQube at http://www.somecompany.com/:

```
# the server directive is Nginx's virtual host directive
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

### Using IIS on Windows

Using IIS on Windows, you can create a website that acts as a reverse proxy and access your SonarQube instance over SSL.

[[info]]
Info: The setup described here is not appropriate for SAML through IIS.

#### Prerequisites

Internet Information Services (IIS) enabled. In the following example, IIS is enabled on the same machine as the SonarQube instance.
The [Url Rewrite extension for IIS](https://www.iis.net/downloads/microsoft/url-rewrite)
The [Application Based Routing extension for IIS](https://www.iis.net/downloads/microsoft/application-request-routing)
[A self-signed SSL certificate, or a real one](https://learn.microsoft.com/en-us/iis/manage/configuring-security/how-to-set-up-ssl-on-iis#obtain-a-certificate)

[[info]]
To make sure the extensions are enabled, restart your IIS Manager after you install them.

#### Creating an IIS website

1. In the IIS Manager, select *Your machine* > **Sites** > **Add Website…**.
2. Under **Site name**, enter a name for your website.
3. Under **Content Directory** > **Physical path**, select a physical path for your website’s folder. Based on the default IIS website, we recommend creating a `%SystemDrive%\inetpub\wwwroot_sonarqube` folder and using it as physical path.
4. In **Binding**, select **Type** > **https**.
5. Under **SSL certificate**, select an SSL certificate.
6. Click **OK**.

#### Using your IIS website as a reverse proxy

Once you’ve created your website using the IIS Manager, you can use the URL Rewrite extension to use that website as a reverse proxy.

1. From the IIS Manager home page, select your website and open **URL Rewrite**.
2. Click **Add Rule(s)** to create a new rule.
3. Select **Reverse Proxy** from the list of templates.
4. Enter the destination server URL. It can be http://localhost:9000 or a remote server. 
5. click **OK** to create the rule.

The URL Rewrite page now displays a reverse proxy inbound rule.

#### Adding the X_FORWARDED_PROTO server variable 

Using the URL Rewrite module, you can create a server variable to handle the `X-Forwarded-Proto` header and pass it to SonarQube. See the HTTPS Configuration section on this page for more information on that server variable.

From the URL Rewrite page:

1. Click **View Server Variables**. This opens the **Allowed Server Variables** page. 
2. To add a server variable, click **Add...**, enter `X_FORWARDED_PROTO` in the field and click **OK**. The server variable is now displayed on the **Allowed Server Variables** page. 
3. Click **Back to Rules** to go to the URL Rewrite rules list.
4. Select the reverse proxy inbound rule for your website. Under **Inbound Rules**, click **Edit**.
5. Expand the **Server variables** section of the rule definition.
6. Add the `X_FORWARDED_PROTO` server variable and give it the value **https**.
7. Apply the changes. 

SonarQube can now be accessed over SSL. 

#### Check that the connection is enabled 

With your SonarQube instance and your IIS website running, open the IIS Manager and click the link under **Your website** > **Browse Website** > **Browse**, or enter the website’s URL in a browser. You should see the log-in or home page of your SonarQube instance. 

#### Next steps

You can configure your SonarQube instance to only accept traffic from your reverse proxy, by adding the following line to the  `sonar.properties` file: 

`sonar.web.host=127.0.0.1`

Another option is to use the Windows Firewall to only accept traffic from localhost.

#### Resources

The setup described here is inspired by this [Configure SSL for SonarQube on Windows](https://jessehouwing.net/sonarqube-configure-ssl-on-windows/) blog post.

### HTTPS Configuration

The reverse proxy must be configured to set the value `X_FORWARDED_PROTO: https` in each HTTP request header. Without this property, redirection initiated by the SonarQube server will fall back on HTTP.

For example, with Nginx as a reverse proxy, you can paste the following or a similar snippet into the configuration file: 

 ```
# the server directive is Nginx's virtual host directive
server { 
  # port to listen on. Can also be set to an IP:PORT	
  listen 443 ssl;
  ssl_certificate ${path_to_your_certificate_file}
  ssl_certificate_key ${path_to_your_certificate_key_file}
  location / {
    proxy_pass ${address_of_your_sonarqube_instance_behind_proxy}
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto https;
  }
}
```

### Forward SonarQube custom headers
SonarQube adds custom HTTP headers. The reverse proxy should be configured to forward the following headers:

* `SonarQube-Authentication-Token-Expiration`  
This header is added to a web service response when using [tokens](/user-guide/user-token/) to authenticate. Forwarding this header is not required for the SonarQube features to work properly.

## Secure your Network

To further lock down the communication in between the reverse proxy and SonarQube, you can define the following network rules: 

Protocol | Source | Destination | Port | default
---|---|---|---|---
TCP | Reverse Proxy | SonarQube | `sonar.web.port` | 9000
TCP | SonarQube | SonarQube | `sonar.search.port` | 9001
TCP | SonarQube | SonarQube | `sonar.es.port` | random

you can further segrement your network configuration if you specify a frontend network and keep Elasticsearch restricted to the loopback NiC.  

Network | Parameter | Description | default
---|---|---|---
Frontend       | `sonar.web.host` 			| Frontend HTTP Network | 0.0.0.0
Elasticsearch  | `sonar.search.host` 		| Elasticsearch Network | 127.0.0.1
