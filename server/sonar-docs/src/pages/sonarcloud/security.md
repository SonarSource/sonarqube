---
title: Security Statement
url: /security/
---

We know that your code is very important to you and your business. We also know that no one wants proven bugs or vulnerabilities found on their source code to be unveiled to third-parties. This is why we take security extremely seriously.

## Hosting

SonarCloud runs primarily off the AWS Frankfurt region. It also uses marginally services located in the AWS Ireland region when they are not available in Frankfurt.

## System security

SonarCloud uses its own Virtual Private Cloud (AWS VPC). Accesses to the infrastructure are restricted through firewalls, allowing accesses from SonarSource networks only. Secure protocols are required for accesses and SSL keys are used for authentication. Access to the infrastructure, inclusive of storage, is restricted to the Technology Operations team.


## Data security

At the infrastructure level, access to data is controlled by virtue of being hosted in network zones which only the Technology Operations have access to. 

To insure data availability, the SonarCloud database is replicated in quasi real time between 2 availability zones both within the Frankfurt region. In addition, the database is fully backed up every day with a 7 days retention. 

At the software level, SonarCloud ensures private source code is accessible to organization members only, in addition to SonarSource Technology Operations team for support purposes only.



## Software security

SonarCloud UI and APIs regularly pass penetration testing conducted by a an external company, specialized in cyber and application security, certified in accordance to ISO-27001 and which is also member of the OWASP.

In case you find a vulnerability, please follow our [Responsible Vulnerability Disclosure process](https://community.sonarsource.com/t/responsible-vulnerability-disclosure/9317) to report it to our Security team.

## Communications

All communications are done over TLS and support TLS 1.2:
* Navigating in the Web application
* Using WS APIs
* Running analysis (by the scanners) from CI services and pushing analysis reports to SonarCloud

## SonarCloud Webhooks 

You can use secrets to secure webhooks and ensure they are coming from SonarCloud (see the "Securing your webhooks" section of the [Webhooks](/project-administration/webhooks/#securing-your-webhooks) page for more information). 


## Authentication

Primary authentication on the system is available through the SonarCloud GitHub application, through OAuth authentication with Bitbucket Cloud and Microsoft Azure DevOps. As a consequence, users don’t have a password on SonarCloud, and are as protected as what they expect (especially with 2FA activated on those systems). 
 
For WS API calls or source code analysis triggered from CI services, only revocable user tokens are accepted.

## Payment

When you subscribe to the paid plan on SonarCloud, your credit card information never transit through our system nor it gets stored on the server. It's handed off to [Braintree Payment Solutions](https://www.braintreepayments.com), a company dedicated to storing your sensitive data on [PCI-Compliant](http://en.wikipedia.org/wiki/Payment_Card_Industry_Data_Security_Standard) servers.
