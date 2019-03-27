---
title: Security Statement
url: /security/
---

We know that your code is very important to you and your business. We also know that no one wants proven bugs or vulnerabilities found on their source code to be unveiled to third-parties. This is why we take security extremely seriously.

## Hosting

SonarCloud is hosted on Amazon AWS in Frankfurt and Ireland. 

## System security

SonarCloud uses its own Virtual Private Cloud (AWS VPC). Accesses to the infrastructure are restricted through firewalls, allowing accesses from SonarSource networks only. Secure protocols are required for accesses and SSL keys are used for authentication. Access to the infrastructure, inclusive of storage, is restricted to the Technology Operations team.


## Data security

At the infrastructure level, access to data is controlled by virtue of being hosted in a network zone which only the Technology Operations have access to. 

To insure data availability, a replica of the SonarCloud database is maintained real time in a different availability zone. In addition, the database is fully backed up everyday with a 7 days retention. 

At the software level, SonarCloud ensures private source code is accessible to organization members only, in addition to SonarSource Technology Operations team for support purposes only.



## Software security

SonarCloud UI and APIs regularly pass penetration testing conducted by a an external company, specialized in cyber and application security, certified in accordance to ISO-27001 and which is also member of the OWASP.

## Communications

All communications are done over TLS 1.2:
* Navigating in the Web application
* Using WS APIs
* Running analysis (by the scanners) from CI services and pushing analysis reports to SonarCloud

## SonarCloud Webhook IPs

SonarCloud performs webhook calls from the following list of IPs:
```
3.120.158.225
3.121.87.141 
52.59.209.17
52.59.246.1
54.93.180.144
18.184.94.137
18.184.195.184 
18.185.94.218 
18.194.44.125
18.194.206.183
18.194.244.158
18.195.64.198
```

## Authentication

Primary authentication on the system is available through the SonarCloud GitHub application, through OAuth authentication with Bitbucket Cloud and Microsoft Azure DevOps. As a consequence, users don’t have a password on SonarCloud, and are as protected as what they expect (especially with 2FA activated on those systems). 
 
For WS API calls or source code analysis triggered from CI services, only revocable user tokens are accepted.

## Payment

When you subscribe to the paid plan on SonarCloud, your credit card information never transit through our system nor it gets stored on the server. It's handed off to [Braintree Payment Solutions](https://www.braintreepayments.com), a company dedicated to storing your sensitive data on [PCI-Compliant](http://en.wikipedia.org/wiki/Payment_Card_Industry_Data_Security_Standard) servers.
