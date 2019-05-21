---
title: Webhooks
url: /project-administration/webhooks/
---

Webhooks notify external services when a project analysis is complete. An HTTP POST request including a JSON payload is sent to each URL. URLs may be specified at both the project and global levels. Project-level specification does not replace global-level webhooks. All hooks at both levels are called.

The HTTP(S) call:

* is made regardless of the status of the Background Task
* includes a JSON document as payload, using the POST method
* has a content type of "application/json", with UTF-8 encoding

## Configuration

You can configure up to 10 webhooks in in **Administration > Webhooks**.

An additional set of 10 webhooks can be configured at the global level in **Administration > Configuration > Webhooks**.

If configured, all 20 will be executed.

## Delivery and Payload

### Delivery

The Webhook administration console shows the result and timestamp of the most recent delivery of each webhook with the payload available via the list icon. Results and payloads of earlier deliveries are available from the tools menu to the right of each webhook.

Response records are purged after 30 days.

The URL must respond within 10 seconds or the delivery is marked as failed.

### Payload

An HTTP header "X-SonarQube-Project" with the project key is sent to allow quick identification of the project involved.

The Payload is a JSON document which includes:

* when the analysis was performed: see "analysedAt"
* the identification of the project analyzed: see "project"
* each Quality Gate criterion checked and its status: see "qualityGate"
* the Quality Gate status of the project: see "qualityGate.status"
* the status and the identifier of the Background Task : see "status" and "taskId"
* user-specified properties: see "properties"

#### Example

```
{
    "serverUrl": "http://localhost:9000",
    "taskId": "AVh21JS2JepAEhwQ-b3u",
    "status": "SUCCESS",
    "analysedAt": "2016-11-18T10:46:28+0100",
    "revision": "c739069ec7105e01303e8b3065a81141aad9f129",
    "project": {
        "key": "myproject",
        "name": "My Project",
        "url": "https://mycompany.com/sonarqube/dashboard?id=myproject"
    },
    "properties": {
    },
    "qualityGate": {
        "conditions": [
            {
                "errorThreshold": "1",
                "metric": "new_security_rating",
                "onLeakPeriod": true,
                "operator": "GREATER_THAN",
                "status": "OK",
                "value": "1"
            },
            {
                "errorThreshold": "1",
                "metric": "new_reliability_rating",
                "onLeakPeriod": true,
                "operator": "GREATER_THAN",
                "status": "OK",
                "value": "1"
            },
            {
                "errorThreshold": "1",
                "metric": "new_maintainability_rating",
                "onLeakPeriod": true,
                "operator": "GREATER_THAN",
                "status": "OK",
                "value": "1"
            },
            {
                "errorThreshold": "80",
                "metric": "new_coverage",
                "onLeakPeriod": true,
                "operator": "LESS_THAN",
                "status": "NO_VALUE"
            }
        ],
        "name": "SonarQube way",
        "status": "OK"
    }
}
```

## Securing your webhooks

After you've configured your server to receive payloads, you want to be sure that the payloads you receive are initiated by {instance} and not by hackers. You can do this by validating a hash signature that ensures that requests originate from {instance}. 

### Setting your secret

To set your secret in {instance}:

1. From the project or organization where you're securing your webhooks, navigate to the webhooks settings at **Administration > webhooks**
1. You can either click **Create** to create a new webhook or click an existing webhook's settings drop-down and click **Update**.
1. Enter a random string in the **Secret** text box. This is used as the key to generate the HMAC hex digest value in the `X-Sonar-Webhook-HMAC-SHA256` header.
1. Click **Update**. 

### Validating {instance} Payloads

After setting your secret, it's used by {instance} to create a hash signature with each payload that's passed using the `X-Sonar-Webhook-HMAC-SHA256` HTTP header. The header value needs to match the signature you are expecting to receive. {instance} uses a HMAC lower-case SHA256 digest to compute the signature of the request body. Here's some sample Java code for your server:

```
private static boolean isValidSignature(YourHttpRequest request) {
  String receivedSignature = request.getHeader("X-Sonar-Webhook-HMAC-SHA256");
  // See Apache commons-codec
  String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, "your_secret").hmacHex(request.getBody())
  return Objects.equals(expectedSignature, receivedSignature);  
}
```

If the signatures don't match, then the payload should be ignored.

## Additional parameters

A basic authentication mechanism is supported by providing user/password in the URL of the Webhook such as `https://myLogin:myPassword@my_server/foo`.

If you provide additional properties to your SonarScanner using the pattern `sonar.analysis.*`, these properties will be automatically added to the section "properties" of the payload.

For example these additional parameters:

```
sonar-scanner -Dsonar.analysis.buildNumber=12345
```

Would add this to the payload:

```
"properties": {
  "sonar.analysis.buildNumber": "12345"
}
```
