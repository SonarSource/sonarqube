---
title: Deploy DCE on Kubernetes
url: /setup/sonarqube-cluster-on-kubernetes/
---

_This page applies to deploying SonarQube Data Center Edition on Kubernetes. For information on deploying Community, Developer, and Enterprise editions of SonarQube on Kubernetes, see [this](/setup/sonarqube-on-kubernetes/) documentation._

# Overview 

[[info]]
| Deploying and operating SonarQube Data Center Edition on Kubernetes is currently in Beta status.

You can find the SonarQube DCE Helm chart on [GitHub](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube-dce).

Your feedback is welcome at [our community forum](https://community.sonarsource.com/).

## Kubernetes Environment Recommendations

When you want to operate SonarQube on Kubernetes, consider the following recommendations.

### Prerequisites

| Kubernetes Version  | Helm Chart Version | SonarQube Version |
| -------- | ----------------------------- | ----------------- |
| 1.19 | 0.1.x | 9.1 |
| 1.20 | 0.1.x | 9.1 |
| 1.21 | 0.1.x | 9.1 |

### Pod Security Policies

The following widely-used Pod Security Policies cannot be used in combination with SonarQube:
* **[Privileged](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#privileged)** - The SonarQube images are currently intended to start as root in order to provision the PVC and drop to lower privileges after that.
* **[ReadOnlyFileSystem](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#volumes-and-file-systems)** - SonarQube is doing some filesystem operations to the container filesystem in order to deploy the correct language analyzers and community plugins.
* **[MustRunAsNonRoot](https://kubernetes.io/docs/concepts/policy/pod-security-policy/#example-policies)** - There is a init container that needs to run privileged to ensure that the [Elasticsearch requirements](/requirements/requirements/) to the specific node are fulfilled.

## Helm chart specifics

We try to provide a good default with the Helm chart, but there are some points to consider while working with SonarQube on Kubernetes. Please read the following sections carefully to make the correct decisions for your environment.

### Installation

Currently only helm3 is supported.

To install the Helm Chart from the [GitHub](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube-dce) Repository, you can use the following commands:

```bash 
git clone https://github.com/SonarSource/helm-chart-sonarqube.git
cd helm-chart-sonarqube/charts/sonarqube-dce
helm dependency update
kubectl create namespace sonarqube-dce
export JWT_SECRET=$(echo -n "your_secret" | openssl dgst -sha256 -hmac "your_key" -binary | base64)
helm upgrade --install -f values.yaml -n sonarqube-dce sonarqube-dce --set ApplicationNodes.jwtSecret=$JWT_SECRET ./
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

Currently, no cloud-native monitoring solutions play nicely with SonarQube or are supported by SonarSource. It is, however, possible to expose at least the JMX metrics to Prometheus with the help of the Prometheus JMX exporter for the Application Nodes.
To use this option, set the following values in your `values.yaml` file:

```yaml
prometheusExporter:
  enabled: true
  config:
    rules:
      - pattern: ".*"
```

This downloads the Prometheus JMX exporter agent and adds it to the startup options of SonarQube. With this default configuration, the JMX metrics will be exposed on /metrics for Prometheus to scrape.

The config scope here defines a configuration that is understandable by the Prometheus JMX exporter. For more information, please see the [documentation](https://github.com/prometheus/jmx_exporter).

#### PodMonitor

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


### Other Configuration Options

This documentation only contains the most important Helm chart customizations. See the [Customize the Chart Before Installing](https://helm.sh/docs/intro/using_helm/#customizing-the-chart-before-installing) documentation and the Helm chart [README](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube-dce) for more possibilities on customizing the Helm chart. 
## Known Limitations

As SonarQube is intended to be run anywhere, there are some drawbacks that are currently known when operating in Kubernetes. This list is not comprehensive, but something to keep in mind and points for us to improve on.

### No Sidecar Support

There is currently no support for additional sidecar containers and, as a result, there is no support for log collection. SonarQube will print the main application log to stdout, but logs on the web, ce, or search component will be printed to separate file streams inside the container.
If you want to use a sidecar container with the SonarQube deployment, you have to manually alter the deployment.

### No Log Complete Collection 

As previously mentioned, there's currently no support for a log collection to make SonarQube observable. Logs are printed to separate file streams as plaintext.
If you still want to scrape these logs, you will need to manually alter the deployment to read these 4 file streams and send them to your log collection solution manually.

### Problems with Azure Fileshare PVC

Currently, there is a known limitation when working on AKS that resonates around the use of Azure Fileshare. We recommend using another storage class for persistency on AKS.
