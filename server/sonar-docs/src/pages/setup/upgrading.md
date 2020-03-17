---
title: Upgrade the Server
url: /setup/upgrading/
---

Upgrading across multiple, non-LTS versions is handled automatically. However, if you have an LTS version in your migration path, you must first migrate to this LTS and then migrate to your target version.

Example 1 : 6.1 -> 8.1, migration path is 6.1 -> 6.7.7 LTS -> 7.9.x LTS -> 8.1
Example 2 : 7.2 -> 7.9, migration path is 7.2 -> 7.9.x LTS (where x is the latest patch available for 7.9 - you don't need to install all the intermediate patches, just take the latest)

## Upgrade Guide

This is a generic guide for upgrading across versions of SonarQube. Carefully read the [Release Upgrade Notes](/setup/upgrade-notes/) of your target version and of any intermediate version(s).

[[warning]]
| Before you start, back up your SonarQube Database. Upgrade problems are rare, but you'll want the backup if anything does happen.

### Upgrading from the ZIP file

1. Download and unzip the SonarQube distribution of your edition in a fresh directory, let's say `$NEW_SONARQUBE_HOME`
2. Manually install the non-default plugins that are compatible with your version of SonarQube. Use the [Compatibility Matrix](https://docs.sonarqube.org/display/PLUG/Plugin+Version+Matrix) to ensure that the versions you install are compatible with your server version. Note that the most recent versions of all SonarSource code analyzers available in your edition are installed by default. Simply copying plugins from the old server to the new is not recommended; incompatible or duplicate plugins could cause startup errors.
3. Update the contents of `sonar.properties` and `wrapper.conf` files (in `$NEW_SONARQUBE_HOME/conf`) with the settings of the related files in the `$OLD_SONARQUBE_HOME/conf` directory (web server URL, database, ldap settings, etc.). Do not copy-paste the old files.
If you are using the Oracle DB, copy its JDBC driver into `$NEW_SONARQUBE_HOME/extensions/jdbc-driver/oracle`
4. Stop your old SonarQube Server
5. Start your new SonarQube Server
6. Browse to `http://yourSonarQubeServerURL/setup` and follow the setup instructions
7. Reanalyze your projects to get fresh data

### Upgrading from the Docker image

### To 8.2+

To upgrade to SonarQube 8.2+:

1. Create a **new** `sonarqube_extensions_8_x` volume.

2. Stop and remove the existing SonarQube container (a restart from the UI is not enough as the environment variables are only evaluated during the first run, not during a restart):
    
	```console
	$ docker stop <container_id>
    $ docker rm <container_id>
	```

3. If you're using non-default plugins, they need to be manually added to the new `sonarqube_extensions_8_x` volume after the first start-up only. If you're using an Oracle database, the same applies to the JDBC driver. To do this:

	a. Start the SonarQube container with the embedded H2 database:
   
    ```
	$ docker run --rm \
		-p 9000:9000 \
		-v sonarqube_extensions_8_x:/opt/sonarqube/extensions \
		<image_name>
	```
	
	b. Exit once SonarQube has started properly. 
   
	c. Copy non-default plugins into `sonarqube_extensions_8_x/plugins` and, if needed, the Oracle driver into `sonarqube_extensions_8_x/jdbc-driver/oracle`.

4. Run docker:

	```bash
	$> docker run -d --name sonarqube \
		-p 9000:9000 \
		-e SONAR_JDBC_URL=... \
		-e SONAR_JDBC_USERNAME=... \
		-e SONAR_JDBC_PASSWORD=... \
		-v sonarqube_data:/opt/sonarqube/data \
		-v sonarqube_extensions_8_x:/opt/sonarqube/extensions \
		-v sonarqube_logs:/opt/sonarqube/logs \
		<image_name>
	```

5. Browse to `http://yourSonarQubeServerURL/setup` and follow the setup instructions.

6. Reanalyze your projects to get fresh data.

### From 7.9.x LTS to another 7.9.x LTS version

No specific Docker operations are needed, just use the new tag.


## Edition Upgrade
If you're moving to a different edition within the same version (upgrade or downgrade) the steps are exactly the same as above, without the need to browse to `http://yourSonarQubeServerURL/setup` or reanalyze your projects.

## Additional Information

### Oracle Clean-up

Starting with version 6.6, there's an additional step you may want to perform if you're using Oracle. On Oracle, the database columns to be dropped are now marked as UNUSED and are not physically dropped anymore. To reclaim disk space, Oracle administrators must drop these unused columns manually. The SQL request is `ALTER TABLE foo DROP UNUSED COLUMNS`. The relevant tables are listed in the system table `all_unused_col_tabs`.

### Additional Database Maintenance

Refreshing your database's statistics and rebuilding your database's indices are recommended once the technical upgrade is done (just before the very last step).

For PostgreSQL, that means executing `VACUUM FULL`. According to the PostgreSQL documentation:

```
In normal PostgreSQL operation, tuples that are deleted or obsoleted by an update are not physically removed from their table; they remain present until a VACUUM is done.
```

### Scanner Update

When upgrading SonarQube, you should also make sure you’re using the latest versions of the SonarQube scanners to take advantage of features and fixes on the scanner side. Please check the documentation pages of the Scanners you use for the most recent version compatible with SonarQube and your build tools.

### SonarQube as a Linux or Windows Service

If you use external configuration, such as a script or Windows Service to control your server, you'll need to update it to point to `$NEW_SONARQUBE_HOME`.

In case you used the InstallNTService.bat to install SonarQube as a Windows Service, run the $OLD_SONARQUBE_HOME/bin/.../UninstallNTService.bat before running the InstallNTService.bat of the $NEW_SONARQUBE_HOME.

## Release Upgrade Notes

Usually SonarQube releases come with some specific recommendations for upgrading from the previous version. You should read the upgrade notes for each version between your current version and the target version.
