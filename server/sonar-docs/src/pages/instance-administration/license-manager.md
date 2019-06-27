---
title: License Manager
url: /instance-administration/license-manager/
---

_The License Manager is accessible from **[Administration > Configuration> License Manager](/#sonarqube-admin#/admin/extension/license/app)** as part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html) and [above](https://www.sonarsource.com/plans-and-pricing/)._

## License Manager

The License Manager enables retrieval of your Server ID (required for obtaining a License Key) and configuration of your SonarSource-provided License key.

### Retrieving Server ID
The Server ID is always available on the License Manager page, as well as in the System section of the System Info .

Note that if the database connection string is updated, the server ID will be re-generated and a new license will be needed. When it is generated, the Server ID is unique. The same Server ID will never be generated twice, even on the same SonarQube instance.

### Setting a License
Using the "Set new license" button, you can set a new License to enable or disable features in SonarQube, or simply to update your license.

### LOCs Consumption
The gauge indicates the quantity of LOCs you are currently scanning and allows you to check if you are near the limit and if you need to purchase additional LOCs.

### Global admin notifications
The License Manager comes with two notification mechanisms built-in (notifications are sent to Global Admins):

- when the license is about to expire–sent two months before expiration, with a reminder one month before
- when the configurable LOC threshold is exceeded–this threshold can be modified via the indicator present on the LOC gauge
- the background job, that checks threshold and decide if a notification should be sent, runs at server startup, then every 24 hours
- the background job does not check validity of smtp server settings and if the emails addresses are set up for global administrators, 
therefore for notifications to be received, these settings needs to be correctly configured before the LOC threshold is reached or exceeded

### Features Included section

This sections of the License Manager page lists the commercial features that are enabled by the license currently set.

## Support

### Access to SonarSource Support
If your License entitles you to SonarSource Support, A **Support** tab will appear at **[Administration > Support](/#sonarqube-admin#/admin/extension/license/support)** to guide you through interactions with SonarSource Support.

This page also allows you to collect the Support Information File of your instance. Make sure to provide this file for any interaction with SonarSource Support.

![Support Information File.](/images/support-information-file.png)
