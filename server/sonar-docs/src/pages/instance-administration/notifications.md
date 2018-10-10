---
title: Notifications
url: /instance-administration/notifications/
---
At the end of each analysis, notifications are computed for each subscribed user. Then, asynchronously, these notifications are sent via email.

To set the frequency with which the notification queue is processed, set `the sonar.notifications.delay` property (in seconds) in _$SONARQUBE-HOME/conf/sonar.properties_. The server must be restarted for the new value to be taken into account.

## Who gets notifications
Only users who subscribe themselves will get notifications. With only one exception, there is no admin functionality to proactively subscribe another user. If you believe a user should be receiving notifications, then it's time to practice the gentle art of persuasion.

### The exception
Notifications will automatically (without user opt-in) be sent to users with Quality Profile Administration rights when built-in quality profiles are updated. These updates can only happen via an upgrade of the relevant analyzer. This type of notification is on by default, and can be toggled globally in **[Administration > General Settings > General](/#sonarqube-admin#/admin/settings/)**.

## Email Configuration
To configure the email server, go to **[Administration > General Settings > Email](/#sonarqube-admin#/admin/settings)**.

Check also the Server base URL property at Administration > General Settings > General to make sure that links in those notification emails will redirect to the right SonarQube server URL.
