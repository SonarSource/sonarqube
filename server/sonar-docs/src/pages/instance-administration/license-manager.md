---
title: License Administration
url: /instance-administration/license-manager/
---

## License Manager
_Starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can access the License Manager at **Administration > Configuration> License Manager**._

You can use the License Manager to retrieve your server ID (required for obtaining a License Key) and configure your SonarSource-provided License key.

[[warning]]
|The server ID is specific to the current database system. Make sure to configure an external database for long-term use prior to requesting your license with this server ID.

### Retrieving the server ID
The server ID is always available on the License Manager page, as well as in the **System Info** section at **Administration > System**.

[[info]]
|If the database connection string is updated, the server ID will be re-generated and you'll need a new license. When it is generated, the server ID is unique. The same server ID will never be generated twice, even on the same SonarQube instance.

### Setting a license
By clicking the **Set new license** button, you can set a new license to enable or disable features in SonarQube or to update your license.

### Lines of Code consumption
Under **Lines of Code**, the gauge shows how many lines of code (LOC) you are currently scanning and how close you are to your limit. If you're near your limit, you may need to purchase additional LOCs.

For a given project, the lines of code that count towards License Usage are those of the largest branch (or pull request). Lines of test code do not count towards your License Usage.

For example:
- If a project has 100 lines of code on its main branch and 200 on a secondary branch, then the number of LOCs counted for the project is 200
- If a project has 0 lines of code on its main branch (provisioned but never analyzed) and 200 on a secondary branch, then the number of LOCs counted is 200
- If a project has 200 lines of code on its main branch and 100 on a secondary branch, then the number of LOCs counted is 200

### Global Administrator notifications
The License Manager has two built-in notification mechanisms (notifications are sent to Global Administrators). Global administrators will get notifications when:

- **the license is about to expire** – a reminder is sent two months and again one month before your license expires.
- **the configurable LOC threshold is exceeded** – you can change this threshold using the indicator on the LOC gauge. 
	- A background job runs at server startup and then every 24 hours to check the LOC threshold and decide if a notification should be sent. 
	- The background job does not check the validity of SMTP server settings and whether the global administrator email addresses are set up. For global administrators to receive notifications, these settings need to be correctly configured before the LOC threshold is reached or exceeded.

### Features Included

This section of the License Manager page lists the commercial features that are enabled by the current license.

## Staging licenses
_Staging licenses are only available in Enterprise Editions, Data Center Edition, or with commercial support_

Your commercial license may include one or more staging licenses. You can use these licenses for non-production instances to test new features, upgrades, new integrations, etc.

Our license mechanism supports a regular synchronization between your production instance and staging instances. To set up synchronization:

- First Staging setup:
  1. Create a staging database and copy the production database in it.
  1. Connect your SonarQube staging instance to it.
  1. Start SonarQube and retrieve the generated server ID.
  1. Request your Staging license key for this server ID.
  1. Set it up in the Administration panel.
  
- Synchronization on a regular basis:
  1. Empty the staging database and copy the production database in it
  1. Start SonarQube
  1. The server ID will be the same as generated the first time, so you can reuse the same license key

## Actions that will invalidate your license key

Certain actions will regenerate your server ID and invalidate your license key. The following are some of the most common of these actions:

- Moving, upgrading, or changing your database server to another host, available with a different IP or DNS name.
- Changing the existing database server IP or DNS name.
- Changing the database/schema name on the database server.
- Restoring the database content from another SonarQube instance (except for production/staging synchronization).
- Reinstalling SonarQube on an empty database.
- Using DBCopy or MySQL Migrator to copy your old database into a new one.

If you plan on going through one of these scenarios and you have commercial support, please open a support ticket beforehand to confirm the plan or to explore alternatives.

In all cases, follow the steps below in **Requesting a new license** if your license key had been invalidated.

## Requesting a new license
If your license key isn't working:
1. Send an email to contact@sonarsource.com that includes the following information:
	- Server ID - Found under **System Info** at **Administration > System**
	- SonarQube version - Found under **System Info** at **Administration > System**
	- SonarQube edition
1. Clarify what current license (production/staging) and server ID this is replacing.
1. Confirm the status of the existing license.

A new license key will be issued within 1 business day once we receive an email with the needed information at contact@sonarsource.com.

## Support

### Access to SonarSource Support
If your license entitles you to SonarSource Support, A **Support** tab will appear at **[Administration > Support](/#sonarqube-admin#/admin/extension/license/support)** to guide you through interactions with SonarSource Support.

This page also allows you to collect the Support Information File of your instance. Make sure to provide this file for any interaction with SonarSource Support.

![Support Information File.](/images/support-information-file.png)

