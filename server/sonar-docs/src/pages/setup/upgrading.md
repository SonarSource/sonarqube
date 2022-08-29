---
title: Upgrade Guide
url: /setup/upgrading/
---
This is a generic guide for upgrading across versions of SonarQube. Carefully read the [Release Upgrade Notes](/setup/upgrade-notes/) of your target version and of any intermediate version(s).

Before upgrading, we recommend practicing your upgrade on a staging environment that's as similar to your production environment as possible. For more on this and other important upgrading concepts, read through the [Before You Upgrade](/setup/before-you-upgrade/) page. 

[[warning]]
| Before upgrading, back up your SonarQube database. Upgrade problems are rare, but you'll want the backup if anything does happen.

## Database disk usage recommendations
During your upgrade, tables may be duplicated to speed up the migration process. This could cause your database disk usage to temporarily increase to as much as double the normal usage. Because of this, we recommend that your database disk usage is below 50% before starting a migration.

## Upgrading Instructions
You can upgrade your SonarQube instance using the ZIP file, Docker image, or Helm Chart. To expand the upgrading instructions, click the option below that corresponds to your set up.

[[info]]
|If your upgrade requires the rebuild of Elasticsearch indexes, your projects and applications will become available as they are reindexed. Portfolios won't be available until all projects are indexed.

[[collapse]]
| ## Upgrading from the ZIP file
| 
| 1. Download and unzip the SonarQube distribution of your edition in a fresh directory, let's say `$NEW_SONAR_HOME`
| 2. If you're using third-party plugins, Manually install plugins that are compatible with your version of SonarQube. Use the [Plugin Version Matrix](/instance-administration/plugin-version-matrix/) to ensure that the versions you install are compatible with your server version. Simply copying plugins from the old server to the new is not recommended; incompatible or duplicate plugins could cause startup errors. Analysis of all languages provided by your edition is available by default without plugins.
| 3. Update the contents of `sonar.properties` file (in `$NEW_SONAR_HOME/conf`) with the settings in the `$OLD_SONAR_HOME/conf` directory (web server URL, database, ldap settings, etc.). Do not copy-paste the old files.
| If you are using the Oracle DB, copy its JDBC driver into `$NEW_SONAR_HOME/extensions/jdbc-driver/oracle`
| 4. Stop your old SonarQube Server
| 5. Start your new SonarQube Server
| 6. Browse to `http://yourSonarQubeServerURL/setup` and follow the setup instructions
| 7. Reanalyze your projects to get fresh data

[[collapse]]
| ## Upgrading from the Docker image
| 
| [[info]]
| | If you're upgrading with an Oracle database or you're using plugins, you can reuse your extensions volume from the previous version to avoid moving plugins or drivers. Use the [Plugin Version Matrix](/instance-administration/plugin-version-matrix/) to ensure that your plugins are compatible with your version. Analysis of all languages provided by your edition is available by default without plugins.
| 
| To upgrade SonarQube using the Docker image:
| 
| 1. Stop and remove the existing SonarQube container (a restart from the UI is not enough as the environment variables are only evaluated during the first run, not during a restart):
|     
| 	```console
|     $ docker stop <container_id>
|     $ docker rm <container_id>
| 	```
| 
| 2. Run docker:
| 
| 	```bash
| 	$> docker run -d --name sonarqube \
| 		-p 9000:9000 \
| 		-e SONAR_JDBC_URL=... \
| 		-e SONAR_JDBC_USERNAME=... \
| 		-e SONAR_JDBC_PASSWORD=... \
| 		-v sonarqube_data:/opt/sonarqube/data \
| 		-v sonarqube_extensions:/opt/sonarqube/extensions \
| 		-v sonarqube_logs:/opt/sonarqube/logs \
| 		<image_name>
| 	```
| 
| 3. Go to `http://yourSonarQubeServerURL/setup` and follow the setup instructions.
| 
| 4. Reanalyze your projects to get fresh data.
| 
| ### **From 8.9.x LTS to another 8.9.x LTS**
| 
| No specific Docker operations are needed, just use the new tag.

[[collapse]]
| ## Upgrading from the Helm Chart 
| 
| [[info]]
| | If you're upgrading with an Oracle database or you're using plugins, you can reuse your extensions PVC from the previous version to avoid moving plugins or drivers. Use the [Plugin Version Matrix](/instance-administration/plugin-version-matrix/) to ensure that your plugins are compatible with your version. Analysis of all languages provided by your edition is available by default without plugins.
| 
| To upgrade SonarQube using our official Helm Chart:
| 
| 1. Change the SonarQube version on your `values.yaml`.
| 
| 2. Redeploy SonarQube with the same helm chart:
| 
| 	```bash
| 	helm upgrade --install -f values.yaml -n <your namespace> <your release name> <path to sonarqube helm chart>
| 	```
| 
| 3. Go to `http://yourSonarQubeServerURL/setup` and follow the setup instructions.
| 
| 4. Reanalyze your projects to get fresh data.
 
## Reverting to the previous version
 
If you need to revert to the previous version of SonarQube, the high-level rollback procedure for all deployments is as follows: 
 
1. Shutdown your SonarQube instance or cluster.
 
2. Roll back your database to the backup you took before starting the upgrade.
 
3. Switch back to the previous version of your SonarQube installation.

4. Start your SonarQube instance or cluster.
 
## Changing your edition
You can move to a different SonarQube edition (for example, moving from Community Edition to a commercial edition) while you're upgrading your version. Just use the appropriate edition file or Docker image tag in the upgrade instructions above.

If you want to move to a different edition without upgrading your SonarQube version, the steps are exactly the same as in the upgrading instructions above without needing to navigate to `http://yourSonarQubeServerURL/setup` or reanalyze your projects. 

## Migrating from a ZIP file instance to a Docker instance
To migrate from the ZIP file to Docker:
1. Configure your Docker instance to point to your existing database.
2. Shut down your ZIP instance.
3. Start your Docker instance.

## Additional steps and information

### Oracle clean-up

Starting with version 6.6, there's an additional step you may want to perform if you're using Oracle. On Oracle, the database columns to be dropped are now marked as UNUSED and are not physically dropped anymore. To reclaim disk space, Oracle administrators must drop these unused columns manually. The SQL request is `ALTER TABLE foo DROP UNUSED COLUMNS`. The relevant tables are listed in the system table `all_unused_col_tabs`.

### Additional database maintenance

Once you've finished a technical upgrade, you should refresh your database's statistics and rebuild your database's indices before starting SonarQube and reanalyzing your projects.

For PostgreSQL, that means executing `VACUUM FULL`. According to the PostgreSQL documentation:

```
In normal PostgreSQL operation, tuples that are deleted or obsoleted by an update are not physically removed from their table; they remain present until a VACUUM is done.
```

### Scanner update

When upgrading SonarQube, you should also make sure you’re using the latest versions of the SonarQube scanners to take advantage of features and fixes on the scanner side. Please check the documentation pages of the scanners you use for the most recent version compatible with SonarQube and your build tools.

See also this section for [what might change after a software update](/user-guide/rules/#header-9).

### SonarQube as a Linux or Windows service

If you use an external configuration, such as a script or Windows Service to control your server, you'll need to update it to point to `$NEW_SONAR_HOME`.
- For Linux it depends how you implemented the service
- For Windows you can update your service by running:
```
> sc delete SonarQube
> $NEW_SONAR_HOME\bin\windows-x86-64\SonarService.bat install
```

### Rebuilding indexes

If your upgrade requires the rebuild of Elasticsearch indexes, your projects and Applications will become available as they are reindexed. Portfolios won't be available until all projects are indexed.

## Release upgrade notes

Usually SonarQube releases come with some specific recommendations for upgrading from the previous version. You should read the upgrade notes for each version between your current version and the target version.
