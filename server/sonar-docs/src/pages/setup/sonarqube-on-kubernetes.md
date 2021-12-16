---
title: Deploy SonarQube on Kubernetes
url: /setup/sonarqube-on-kubernetes/
---

_This part of the Documentation is only valid for Community, Developer, and Enterprise Editions. For information on deploying the Data Center Edition of SonarQube on Kubernetes, see [this](/setup/sonarqube-cluster-on-kubernetes/) documentation._

# Overview 

You can find the SonarQube Helm chart on [GitHub](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube).

Your feedback is welcome at [our community forum](https://community.sonarsource.com/).

## Kubernetes Environment Recommendations

When you want to operate SonarQube on Kubernetes, consider the following recommendations.

### Prerequisites

#### SonarQube Helm Chart

| Kubernetes Version  | Helm Chart Version | SonarQube Version |
| -------- | ----------------------------- | ----------------- |
| 1.19 | 1.1 | 9.1 |
| 1.20 | 1.1 | 9.1 |
| 1.21 | 1.1 | 9.1 |


### Pod Security Policies

The following widely-used Pod Security Policies cannot be used in combination with SonarQube:
* **[Privileged](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privileged)** - The SonarQube images are currently intended to start as root in order to provision the PVC and drop to lower privileges after that.
* **[ReadOnlyFileSystem](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)** - SonarQube is doing some filesystem operations to the container filesystem in order to deploy the correct language analyzers and community plugins.
* **[MustRunAsNonRoot](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#example-policies)** - There is a init container that needs to run privileged to ensure that the [Elasticsearch requirements](/requirements/requirements/) to the specific node are fulfilled.

### Taints and Tolerations

We recommend binding SonarQube to a specific node and reserving this node for SonarQube. It greatly increases the stability of the service.
The following sections detail creating a taint on a specific node and letting the SonarQube deployment ignore this taint using a flag in the `values.yaml` of the Helm Chart.

#### Creating a taint

In order to create a taint, you need to select a node that you want to reserve for SonarQube. Use the following command to get a list of all nodes attached to your Kubernetes Cluster:

```bash
kubectl get nodes
```

Select a node from the output of this command, and create a custom taint using the following command: 

```bash
kubectl taint nodes <node> sonarqube=true:NoSchedule
```

This taint ensures that no additional pods are scheduled on this node.

#### Ignoring this Taint for SonarQube

To let the SonarQube deployment ignore the previously created taint, add the following section to the `values.yaml`:

```yaml
tolerations: 
  - key: "sonarqube"
    operator: "Equal"
    value: "true"
    effect: "NoSchedule"
```
Depending on your taint's name, you may need to adjust the key accordingly.

### Node Labels

As described in the **Taints and Tolerations** section above, for stability, we recommend binding the SonarQube deployment to one node in your cluster. With one node now reserved for SonarQube, you need to label this node to be selected by the Kube-scheduler in the pod assignment.

#### Label a Node

Label the node for which you previously defined a taint with the following command:

```bash
kubectl label node <node> sonarqube=true
```

#### Bind Deployment to Label

To only let SonarQube be scheduled on nodes with this specific label, add the following section to the `values.yaml`:

```yaml
nodeSelector: 
  sonarqube: "true"
```

By combining node selection with taints and tolerations, SonarQube can run alone on one specific node independently from the rest of your software in your Kubernetes cluster. This results in better stability and performance of SonarQube. 
For more information, see the official [Kubernetes documentation](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/).

### Affinity

Node affinity and anti-affinity can be used in a way similar to node selectors but with more operators to choose from. However, we generally don’t recommend using this in combination with SonarQube as it can lead to recurring rescheduling of the SonarQube pod. 
If you still want to use affinity and anti-affinity, see the official [Kubernetes documentation](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity). 

## Helm chart specifics

We try to provide a good default with the Helm chart, but there are some points to consider while working with SonarQube on Kubernetes. Please read the following sections carefully to make the correct decisions for your environment.

### Installation

Currently only helm3 is supported.

To install the Helm Chart from our Helm Repository, you can use the following commands:

```bash 
helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
helm repo update
kubectl create namespace sonarqube
helm upgrade --install -n sonarqube sonarqube sonarqube/sonarqube
```

### Persistency 

SonarQube comes with a bundled Elasticsearch and, as Elasticsearch is stateful, so is SonarQube. There is an option to persist the Elasticsearch indexes in a Persistent Volume, but with regular killing operations by the Kubernetes Cluster, these indexes can be corrupted. By default, persistency is disabled in the Helm chart.  
Enabling persistency decreases the startup time of the SonarQube Pod significantly, but you are risking corrupting your Elasticsearch index. You can enable persistency by adding the following to the `values.yaml`:

```yaml
persistence:
  enabled: true
```

Leaving persistency disabled results in a longer startup time until SonarQube is fully available, but you won't lose any data as SonarQube will persist all data in the database.

### Custom Certificate

When you're working with your own CA or in an environment that uses self-signed certificates for your code repository platform, you can create a secret containing this certificate and add this certificate to the java truststore inside the SonarQube deployment directly during the deployment.

To enable this behavior, add the following to your `value.yaml` file:

```yaml
caCerts:
  secret: <secret name>
```

#### Get Certificate via openssl

If you already have a running installation of your code repository platform, you can extract the certificate with the following snippet using `openssl`

```bash
echo -n | openssl s_client -connect <server url>:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > cert.pem
```

This certificate needs to be Base64 encoded in order to be added as secret data.  

```bash
Create base64 string
cat cert.pem | base64 | tr -d "\n"
```

Note that you can also use `string-data` here if you don't want to encode your certificate.

#### Create secret

The Base64 encoded certificate can be added to the secret's data:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: <secret name>
  namespace: <sonarqube namespace>
data:
  cert: <base64 string>
```

Then, create the secret in your Kubernetes cluster with the following command:

```bash
kubectl apply -f secret.yaml
```

### Ingress Creation

To make the SonarQube service accessible from outside of your cluster, you most likely need an ingress. Creating a new ingress is also covered by the Helm chart. See the following section for help with creating one.

#### Ingress Class

The SonarSource Helm chart has an optional dependency to the [NGINX-ingress helm chart](https://kubernetes.github.io/ingress-nginx). If you already have NGINX-ingress present in your cluster, you can use it. 

If you want to install NGINX as well, add the following to your `values.yaml`.

```yaml
nginx:
  enabled: true
```

We recommend using the `ingress-class` NGINX with a body size of at least 8MB. This can be achieved with the following changes to your `values.yaml`:

```yaml
ingress:
  enabled: true
  # Used to create an Ingress record.
  hosts:
    - name: <Your Sonarqube FQDN>
      # Different clouds or configurations might need /* as the default path
      path: /
      # For additional control over serviceName and servicePort
      # serviceName: someService
      # servicePort: somePort
  annotations: 
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/proxy-body-size: "8m"
```

### Monitoring
You can monitor your SonarQube instance using SonarQube's native integration with Prometheus. Through this integration, you can ensure your instance is running properly and know if you need to take action to prevent future issues. 

Prometheus monitors your SonarQube instance by collecting metrics from the `/api/monitoring/metrics` endpoint. Results are returned in OpenMetrics text format. See Prometheus' documentation on [Exposition Formats](https://prometheus.io/docs/instrumenting/exposition_formats/) for more information on the OpenMetrics text format. 

Monitoring through this endpoint requires authentication. You can access the endpoint following ways:

- **`Authorization:Bearer xxxx` header:** You can use a bearer token during database upgrade and when SonarQube is fully operational. Define the bearer token in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **`X-Sonar-Passcode: xxxxx` header:** You can use `X-Sonar-passcode` during database upgrade and when SonarQube is fully operational. Define `X-Sonar-passcode` in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **username:password and JWT token:** When SonarQube is fully operational, system admins logged in with local or delegated authentication can access the endpoint. 

#### **JMX Exporter**
You can also expose the JMX metrics to Prometheus using the Prometheus JMX exporter.

To use this option, set the following values in your `values.yaml` file:

```yaml
prometheusExporter:
  enabled: true
  config:
    rules:
      - pattern: ".*"
```

This downloads the Prometheus JMX exporter agent and adds it to the startup options of SonarQube. With this default configuration, the JMX metrics will be exposed on /metrics for Prometheus to scrape.

The config scope here defines a configuration that is understandable by the Prometheus JMX exporter. For more information, please Prometheus' documentation on the [JMX Exporter](https://github.com/prometheus/jmx_exporter).

#### **PodMonitor**

You can collect metrics on using PodMonitor for Prometheus by defining PodMonitor as follows:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: sonarqube
  namespace: monitoring
spec:
  namespaceSelector:
    matchNames:
    - sonarqube
  podMetricsEndpoints:
  - interval: 30s
    path: /
    scheme: http
    targetPort: monitoring-ce
  - interval: 30s
    path: /
    scheme: http
    targetPort: monitoring-web
  selector:
    matchLabels:
      app: sonarqube
```

### Other Configuration Options

While we only document the most pressing Helm chart customizations in this documentation, there are other possibilities for you to choose to [Customize the Chart Before Installing](https://helm.sh/docs/intro/using_helm/#customizing-the-chart-before-installing). Please see the Helm chart [README](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube) file for more information on these.

## Known Limitations

As SonarQube is intended to be run anywhere, there are some drawbacks that are currently known when operating in Kubernetes. This list is not comprehensive, but something to keep in mind and points for us to improve on.

### Readiness and Startup delays

When persistence is disabled, SonarQube startup takes significantly longer as the Elasticsearch indexes need to be rebuilt. As this delay depends on the amount of data in your SonarQube instance, the values for the startup/readiness and liveness probes need to be adjusted to your environment. 
We also recommend taking a look at the default limits for the SonarQube deployment as the amount of CPU available to SonarQube also impacts the startup time.

### Problems with Azure Fileshare PVC

Currently, there is a known limitation when working on AKS that resonates around the use of Azure Fileshare. We recommend using another storage class for persistency on AKS.

