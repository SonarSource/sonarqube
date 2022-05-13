---
title: Deploy a SonarQube Cluster on Kubernetes
url: /setup/sonarqube-cluster-on-kubernetes/
---

_This page applies to deploying SonarQube Data Center Edition on Kubernetes. For information on deploying Community, Developer, and Enterprise editions of SonarQube on Kubernetes, see [this](/setup/sonarqube-on-kubernetes/) documentation._

# Overview 

You can find the SonarQube DCE Helm chart on [GitHub](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube-dce).

Your feedback is welcome at [our community forum](https://community.sonarsource.com/).

## Kubernetes Environment Recommendations

When you want to operate SonarQube on Kubernetes, consider the following recommendations.

### Supported Versions

The SonarQube helm chart should only be used with the latest version of SonarQube and a supported version of Kubernetes. There is a dedicated helm chart for the LTS version of SonarQube that follows the same patch policy as the application, while also being compatible with the supported versions of Kubernetes. 

### Pod Security Policies

The following widely-used Pod Security Policies cannot be used in combination with SonarQube:
* **[Privileged](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privileged)** - The SonarQube images are currently intended to start as root in order to provision the PVC and drop to lower privileges after that.
* **[ReadOnlyFileSystem](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)** - SonarQube is doing some filesystem operations to the container filesystem in order to deploy the correct language analyzers and community plugins.
* **[MustRunAsNonRoot](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#example-policies)** - There is a init container that needs to run privileged to ensure that the [Elasticsearch requirements](/requirements/requirements/) to the specific node are fulfilled.

## Helm chart specifics

We try to provide a good default with the Helm chart, but there are some points to consider while working with SonarQube on Kubernetes. Please read the following sections carefully to make the correct decisions for your environment.

### Installation

Currently only helm3 is supported.

To install the Helm Chart from Helm Repository, you can use the following commands:

```bash 
helm repo add sonarqube https://SonarSource.github.io/helm-chart-sonarqube
helm repo update
kubectl create namespace sonarqube-dce
export JWT_SECRET=$(echo -n "your_secret" | openssl dgst -sha256 -hmac "your_key" -binary | base64)
helm upgrade --install -n sonarqube-dce sonarqube-dce --set ApplicationNodes.jwtSecret=$JWT_SECRET sonarqube/sonarqube-dce
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

You can monitor your SonarQube cluster using SonarQube's native integration with Prometheus. Through this integration, you can ensure your cluster is running properly and know if you need to take action to prevent future issues. 

Prometheus monitors your SonarQube cluster by collecting metrics from the `/api/monitoring/metrics` endpoint. Results are returned in OpenMetrics text format. See Prometheus' documentation on [Exposition Formats](https://prometheus.io/docs/instrumenting/exposition_formats/) for more information on the OpenMetrics text format. 

Monitoring through this endpoint requires authentication. You can access the endpoint following ways:

- **`Authorization:Bearer xxxx` header:** You can use a bearer token during database upgrade and when SonarQube is fully operational. Define the bearer token in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **`X-Sonar-Passcode: xxxxx` header:** You can use `X-Sonar-passcode` during database upgrade and when SonarQube is fully operational. Define `X-Sonar-passcode` in the `sonar.properties` file using the `sonar.web.systemPasscode property`.
- **username:password and JWT token:** When SonarQube is fully operational, system admins logged in with local or delegated authentication can access the endpoint. 

#### **JMX Exporter**
You can also expose the JMX metrics to Prometheus with the help of the Prometheus JMX exporter.

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

You can collect metrics on application nodes using PodMonitor for Prometheus. Search node monitoring is not currently supported. To monitor applications nodes, define PodMonitor as follows:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: sonarqube
  namespace: monitoring
spec:
  namespaceSelector:
    matchNames:
    - sonarqube-dce
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
      app: sonarqube-dce
```

### Log Format

SonarQube prints all logs in plain-text to stdout/stderr. It can print logs as JSON-String if the variable `logging.jsonOutput` is set to `true`. This will enable log collection tools like [Loki](https://grafana.com/oss/loki/) to do post processing on the information that are provided by the application.

#### LogQL Example

With JSON Logging enabled, you can define a LogQL Query like this to filter only logs with the severity "ERROR" and display the Name of the Pod as well as the Message:

```
{namespace="sonarqube-dce", app="sonarqube-dce"}| json | severity="ERROR" | line_format "{{.nodename}} {{.message}}"
```

### ES Cluster Authentication

Since SonarQube 8.9, you can enable basic security for the Search Cluster in SonarQube. To benefit from this additional layer of security on Kubernetes as well, you need to provide a PKCS#11 Container with the required certificates to our Helm chart.
The required secret can be created like this: 

```bash
kubectl create secret generic <NAME OF THE SECRET> --from-file=/PATH/TO/YOUR/PKCS12.container=elastic-stack-ca.p12 -n <NAMESPACE>
```

### Other Configuration Options

This documentation only contains the most important Helm chart customizations. See the [Customize the Chart Before Installing](https://helm.sh/docs/intro/using_helm/#customizing-the-chart-before-installing) documentation and the Helm chart [README](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube-dce) for more possibilities on customizing the Helm chart. 
## Known Limitations

### Problems with Azure Fileshare PVC

Currently, there is a known limitation when working on AKS that resonates around the use of Azure Fileshare. We recommend using another storage class for persistency on AKS.
