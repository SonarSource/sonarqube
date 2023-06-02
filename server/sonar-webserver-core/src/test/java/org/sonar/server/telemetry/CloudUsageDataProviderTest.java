/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.telemetry;

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
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.server.util.Paths2;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.telemetry.CloudUsageDataProvider.KUBERNETES_SERVICE_HOST;
import static org.sonar.server.telemetry.CloudUsageDataProvider.KUBERNETES_SERVICE_PORT;

public class CloudUsageDataProviderTest {

  private final System2 system2 = Mockito.mock(System2.class);
  private final Paths2 paths2 = Mockito.mock(Paths2.class);
  private final OkHttpClient httpClient = Mockito.mock(OkHttpClient.class);
  private final CloudUsageDataProvider underTest = new CloudUsageDataProvider(system2, paths2, httpClient);

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
  public void kubernetesProvider_shouldReturnValue() {
    assertThat(underTest.getCloudUsage().kubernetesProvider()).isNotBlank();
  }

  @Test
  public void initHttpClient_whenValidCertificate_shouldCreateClient() throws URISyntaxException {
    when(paths2.get(anyString())).thenReturn(Paths.get(requireNonNull(getClass().getResource("dummy.crt")).toURI()));

    CloudUsageDataProvider provider = new CloudUsageDataProvider(system2, paths2);
    assertThat(provider.getHttpClient()).isNotNull();
  }

  @Test
  public void initHttpClient_whenNotOnKubernetes_shouldNotCreateClient() throws URISyntaxException {
    when(paths2.get(anyString())).thenReturn(Paths.get(requireNonNull(getClass().getResource("dummy.crt")).toURI()));
    when(system2.envVariable(KUBERNETES_SERVICE_HOST)).thenReturn(null);

    CloudUsageDataProvider provider = new CloudUsageDataProvider(system2, paths2);
    assertThat(provider.getHttpClient()).isNull();
  }

  @Test
  public void initHttpClient_whenCertificateNotFound_shouldFail() {
    when(paths2.get(any())).thenReturn(Paths.get("dummy.crt"));

    CloudUsageDataProvider provider = new CloudUsageDataProvider(system2, paths2);
    assertThat(provider.getHttpClient()).isNull();
  }
}
