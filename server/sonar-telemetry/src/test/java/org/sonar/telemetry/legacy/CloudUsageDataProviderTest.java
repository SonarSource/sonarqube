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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.util.Paths2;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.telemetry.legacy.CloudUsageDataProvider.DOCKER_RUNNING;
import static org.sonar.telemetry.legacy.CloudUsageDataProvider.KUBERNETES_SERVICE_HOST;
import static org.sonar.telemetry.legacy.CloudUsageDataProvider.KUBERNETES_SERVICE_PORT;
import static org.sonar.telemetry.legacy.CloudUsageDataProvider.SONAR_HELM_CHART_VERSION;

public class CloudUsageDataProviderTest {

  private final System2 system2 = mock(System2.class);
  private final Paths2 paths2 = mock(Paths2.class);
  private final OkHttpClient httpClient = mock(OkHttpClient.class);
  private final ContainerSupport containerSupport = mock(ContainerSupport.class);
  private final ProcessBuilder processBuilder = mock(ProcessBuilder.class);
  private final CloudUsageDataProvider underTest = new CloudUsageDataProvider(containerSupport, system2, paths2, () -> processBuilder,
    httpClient);

  @Before
  public void setUp() throws Exception {
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn("localhost");
    when(system2.envVariable(KUBERNETES_SERVICE_PORT)).thenReturn("443");

    mockHttpClientCall(200, "OK", ResponseBody.create("""
      {
          "major": "1",
          "minor": "25",
          "gitVersion": "v1.25.3",
          "gitCommit": "434bfd82814af038ad94d62ebe59b133fcb50506",
          "gitTreeState": "clean",
          "buildDate": "2022-11-02T03:24:50Z",
          "goVersion": "go1.19.2",
          "compiler": "gc",
          "platform": "linux/arm64"
      }
      """, MediaType.parse(MediaTypes.JSON)));
  }

  private void mockHttpClientCall(int code, String message, @Nullable ResponseBody body) throws IOException {
    Call callMock = mock(Call.class);
    when(callMock.execute()).thenReturn(new Response.Builder()
      .request(new Request.Builder().url("http://any.test/").build())
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(message)
      .body(body)
      .build());
    when(httpClient.newCall(any())).thenReturn(callMock);
  }

  @Test
  public void containerRuntime_whenContainerSupportContextExists_shouldNotBeNull() {
    when(containerSupport.getContainerContext()).thenReturn("docker");
    assertThat(underTest.getCloudUsage().containerRuntime()).isEqualTo("docker");
  }

  @Test
  public void containerRuntime_whenContainerSupportContextMissing_shouldBeNull() {
    when(containerSupport.getContainerContext()).thenReturn(null);
    assertThat(underTest.getCloudUsage().containerRuntime()).isNull();
  }

  @Test
  public void kubernetes_whenEnvVarExists_shouldReturnTrue() {
    assertThat(underTest.getCloudUsage().kubernetes()).isTrue();
  }

  @Test
  public void kubernetes_whenEnvVarDoesNotExist_shouldReturnFalse() {
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn(null);
    assertThat(underTest.getCloudUsage().kubernetes()).isFalse();
  }

  @Test
  public void kubernetesVersion_whenOnKubernetes_shouldReturnValue() {
    assertThat(underTest.getCloudUsage().kubernetesVersion()).isEqualTo("1.25");
  }

  @Test
  public void kubernetesVersion_whenNotOnKubernetes_shouldReturnNull() {
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn(null);
    assertThat(underTest.getCloudUsage().kubernetesVersion()).isNull();
  }

  @Test
  public void kubernetesVersion_whenApiCallFails_shouldReturnNull() throws IOException {
    mockHttpClientCall(404, "not found", null);
    assertThat(underTest.getCloudUsage().kubernetesVersion()).isNull();
  }

  @Test
  public void kubernetesPlatform_whenOnKubernetes_shouldReturnValue() {
    assertThat(underTest.getCloudUsage().kubernetesPlatform()).isEqualTo("linux/arm64");
  }

  @Test
  public void kubernetesPlatform_whenNotOnKubernetes_shouldReturnNull() {
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn(null);
    assertThat(underTest.getCloudUsage().kubernetesPlatform()).isNull();
  }

  @Test
  public void kubernetesPlatform_whenApiCallFails_shouldReturnNull() throws IOException {
    mockHttpClientCall(404, "not found", null);
    assertThat(underTest.getCloudUsage().kubernetesPlatform()).isNull();
  }

  @Test
  public void kubernetesProvider_shouldReturnValue() throws IOException {
    Process processMock = mock(Process.class);
    when(processMock.getInputStream()).thenReturn(new ByteArrayInputStream("some-provider".getBytes()));
    when(processBuilder.command(any(String[].class))).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(processMock);

    assertThat(underTest.getCloudUsage().kubernetesProvider()).isEqualTo("some-provider");
  }

  @Test
  public void kubernetesProvider_whenValueContainsNullChars_shouldReturnValueWithoutNullChars() throws IOException {
    Process processMock = mock(Process.class);
    when(processMock.getInputStream()).thenReturn(new ByteArrayInputStream("so\u0000me-prov\u0000ider".getBytes()));
    when(processBuilder.command(any(String[].class))).thenReturn(processBuilder);
    when(processBuilder.start()).thenReturn(processMock);

    assertThat(underTest.getCloudUsage().kubernetesProvider()).isEqualTo("some-provider");
  }

  @Test
  public void officialHelmChart_whenEnvVarExists_shouldReturnValue() {
    when(system2.envVariable(SONAR_HELM_CHART_VERSION)).thenReturn("10.1.0");
    assertThat(underTest.getCloudUsage().officialHelmChart()).isEqualTo("10.1.0");
  }

  @Test
  public void officialHelmChart_whenEnvVarDoesNotExist_shouldReturnNull() {
    when(system2.envVariable(SONAR_HELM_CHART_VERSION)).thenReturn(null);
    assertThat(underTest.getCloudUsage().officialHelmChart()).isNull();
  }

  @Test
  public void officialImage_whenEnvVarTrue_shouldReturnTrue() {
    when(system2.envVariable(DOCKER_RUNNING)).thenReturn("True");
    assertThat(underTest.getCloudUsage().officialImage()).isTrue();
  }

  @Test
  public void officialImage_whenEnvVarFalse_shouldReturnFalse() {
    when(system2.envVariable(DOCKER_RUNNING)).thenReturn("False");
    assertThat(underTest.getCloudUsage().officialImage()).isFalse();
  }

  @Test
  public void officialImage_whenEnvVarDoesNotExist_shouldReturnFalse() {
    when(system2.envVariable(DOCKER_RUNNING)).thenReturn(null);
    assertThat(underTest.getCloudUsage().officialImage()).isFalse();
  }

  @Test
  public void initHttpClient_whenValidCertificate_shouldCreateClient() throws URISyntaxException {
    when(paths2.get(anyString())).thenReturn(Paths.get(requireNonNull(getClass().getResource("dummy.crt")).toURI()));

    CloudUsageDataProvider provider = new CloudUsageDataProvider(containerSupport, system2, paths2);
    assertThat(provider.getHttpClient()).isNotNull();
  }

  @Test
  public void initHttpClient_whenNotOnKubernetes_shouldNotCreateClient() throws URISyntaxException {
    when(paths2.get(anyString())).thenReturn(Paths.get(requireNonNull(getClass().getResource("dummy.crt")).toURI()));
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn(null);

    CloudUsageDataProvider provider = new CloudUsageDataProvider(containerSupport, system2, paths2);
    assertThat(provider.getHttpClient()).isNull();
  }

  @Test
  public void initHttpClient_whenCertificateNotFound_shouldFail() {
    when(paths2.get(any())).thenReturn(Paths.get("dummy.crt"));

    CloudUsageDataProvider provider = new CloudUsageDataProvider(containerSupport, system2, paths2);
    assertThat(provider.getHttpClient()).isNull();
  }
}
