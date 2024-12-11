/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.telemetry.legacy;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Scanner;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.util.Paths2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@ServerSide
public class CloudUsageDataProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CloudUsageDataProvider.class);

  private static final String SERVICEACCOUNT_CA_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
  static final String KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
  static final String KUBERNETES_SERVICE_PORT = "KUBERNETES_SERVICE_PORT";
  static final String SONAR_HELM_CHART_VERSION = "SONAR_HELM_CHART_VERSION";
  static final String DOCKER_RUNNING = "DOCKER_RUNNING";
  private static final String[] KUBERNETES_PROVIDER_COMMAND = {"bash", "-c", "uname -r"};
  private static final int KUBERNETES_PROVIDER_MAX_SIZE = 100;
  private final ContainerSupport containerSupport;
  private final System2 system2;
  private final Paths2 paths2;
  private final Supplier<ProcessBuilder> processBuilderSupplier;
  private OkHttpClient httpClient;
  private TelemetryData.CloudUsage cloudUsageData;

  @Inject
  public CloudUsageDataProvider(ContainerSupport containerSupport, System2 system2, Paths2 paths2) {
    this(containerSupport, system2, paths2, ProcessBuilder::new, null);
    if (isOnKubernetes()) {
      initHttpClient();
    }
  }

  @VisibleForTesting
  CloudUsageDataProvider(ContainerSupport containerSupport, System2 system2, Paths2 paths2, Supplier<ProcessBuilder> processBuilderSupplier,
                         @Nullable OkHttpClient httpClient) {
    this.containerSupport = containerSupport;
    this.system2 = system2;
    this.paths2 = paths2;
    this.processBuilderSupplier = processBuilderSupplier;
    this.httpClient = httpClient;
  }

  public TelemetryData.CloudUsage getCloudUsage() {
    if (cloudUsageData != null) {
      return cloudUsageData;
    }

    String kubernetesVersion = null;
    String kubernetesPlatform = null;

    if (isOnKubernetes()) {
      VersionInfo versionInfo = getVersionInfo();
      if (versionInfo != null) {
        kubernetesVersion = versionInfo.major() + "." + versionInfo.minor();
        kubernetesPlatform = versionInfo.platform();
      }
    }

    cloudUsageData = new TelemetryData.CloudUsage(
      isOnKubernetes(),
      kubernetesVersion,
      kubernetesPlatform,
      getKubernetesProvider(),
      getOfficialHelmChartVersion(),
      containerSupport.isRunningOnHelmOpenshift(),
      containerSupport.isHelmAutoscalingEnabled(),
      containerSupport.getContainerContext(),
      isOfficialImageUsed());

    return cloudUsageData;
  }

  private boolean isOnKubernetes() {
    return StringUtils.isNotBlank(system2.envVariable(KUBERNETES_SERVICE_HOST));
  }

  @CheckForNull
  private String getOfficialHelmChartVersion() {
    return system2.envVariable(SONAR_HELM_CHART_VERSION);
  }

  private boolean isOfficialImageUsed() {
    return Boolean.parseBoolean(system2.envVariable(DOCKER_RUNNING));
  }

  /**
   * Create an http client to call the Kubernetes API.
   * This is based on the client creation in the official Kubernetes Java client.
   */
  private void initHttpClient() {
    try {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(getKeyStore());
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, new SecureRandom());

      httpClient = new OkHttpClient.Builder()
        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
        .hostnameVerifier(OkHostnameVerifier.INSTANCE)
        .build();
    } catch (Exception e) {
      LOG.debug("Failed to create http client for Kubernetes API", e);
    }
  }

  private KeyStore getKeyStore() throws GeneralSecurityException, IOException {
    KeyStore caKeyStore = newEmptyKeyStore();

    try (FileInputStream fis = new FileInputStream(paths2.get(SERVICEACCOUNT_CA_PATH).toFile())) {
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(fis);

      int index = 0;
      for (Certificate certificate : certificates) {
        String certificateAlias = "ca" + index;
        caKeyStore.setCertificateEntry(certificateAlias, certificate);
        index++;
      }
    }

    return caKeyStore;
  }

  private static KeyStore newEmptyKeyStore() throws GeneralSecurityException, IOException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);
    return keyStore;
  }

  record VersionInfo(String major, String minor, String platform) {
  }

  private VersionInfo getVersionInfo() {
    try {
      Request request = buildRequest();
      try (Response response = httpClient.newCall(request).execute()) {
        ResponseBody responseBody = requireNonNull(response.body(), "Response body is null");
        return new Gson().fromJson(responseBody.string(), VersionInfo.class);
      }
    } catch (Exception e) {
      LOG.debug("Failed to get Kubernetes version info", e);
      return null;
    }
  }

  private Request buildRequest() throws URISyntaxException {
    String host = system2.envVariable(KUBERNETES_SERVICE_HOST);
    String port = system2.envVariable(KUBERNETES_SERVICE_PORT);
    if (host == null || port == null) {
      throw new IllegalStateException("Kubernetes environment variables are not set");
    }

    URI uri = new URI("https", null, host, Integer.parseInt(port), "/version", null, null);

    return new Request.Builder()
      .get()
      .url(uri.toString())
      .build();
  }

  @CheckForNull
  private String getKubernetesProvider() {
    try {
      Process process = processBuilderSupplier.get().command(KUBERNETES_PROVIDER_COMMAND).start();
      try (Scanner scanner = new Scanner(process.getInputStream(), UTF_8)) {
        scanner.useDelimiter("\n");
        // Null characters can be present in the output on Windows
        String output = scanner.next().replace("\u0000", "");
        return StringUtils.abbreviate(output, KUBERNETES_PROVIDER_MAX_SIZE);
      } finally {
        process.destroy();
      }
    } catch (Exception e) {
      LOG.debug("Failed to get Kubernetes provider", e);
      return null;
    }
  }

  @VisibleForTesting
  OkHttpClient getHttpClient() {
    return httpClient;
  }
}
