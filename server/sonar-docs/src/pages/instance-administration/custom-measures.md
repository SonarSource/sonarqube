---
title: Custom Measures
url: /instance-administration/custom-measures/
---

SonarQube collects a maximum of measures in an automated manner but there are some measures for which this is not possible, such as when: the information is not available for collection, the measure is computed by a human, and so on. Whatever the reason, SonarQube provides a service to inject those measures manually and allow you to benefit from other services: the Manual Measures service. The manual measures entered will be picked during the next analysis of the project and thereafter treated as "normal" measures.

## Managing Custom Metrics
As with measures that are collected automatically, manual measures are the values collected in each analsis for manual metrics. Therefore, the first thing to do is create the metric you want to save your measure against. In order to do so, log in as a system administrator and go to **[Administration > Configuration > Custom Metrics](/#sonarqube-admin#/admin/custom_metrics)**, where the interface will guide you in creating the Metric you need. 

## Managing Custom Measures
Custom measures can be entered at project level. To add a measure, sign in as a project administrator, navigate to the desired project and choose **Administration > Custom Measures**, where you will find a table with the latest measure value entered for each metric. 

Values entered in this interface are "Pending", and will not be visible outside this administrative interface until the next analysis. 

