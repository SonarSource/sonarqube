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
package org.sonar.scanner.repository.settings;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProjectSettingsLoaderTest {

  private DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private ScannerProperties properties = mock(ScannerProperties.class);
  private DefaultProjectSettingsLoader underTest = new DefaultProjectSettingsLoader(wsClient, properties);

  @Test
  public void loadProjectSettings() throws IOException {
    WsResponse response = mock(WsResponse.class);
    PipedOutputStream out = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(out);
    Settings.ValuesWsResponse.newBuilder()
      .addSettings(Settings.Setting.newBuilder()
        .setKey("abc").setValue("def")
        .build())
      .addSettings(Settings.Setting.newBuilder()
        .setKey("123").setValue("456")
        .build())
      .build()
      .writeTo(out);
    out.close();
    when(response.contentStream()).thenReturn(in);
    when(wsClient.call(any())).thenReturn(response);
    when(properties.getProjectKey()).thenReturn("project_key");

    Map<String, String> result = underTest.loadProjectSettings();

    ArgumentCaptor<GetRequest> argumentCaptor = ArgumentCaptor.forClass(GetRequest.class);
    verify(wsClient, times(1)).call(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getPath()).isEqualTo("api/settings/values.protobuf?component=project_key");
    assertThat(result)
      .isNotNull()
      .hasSize(2)
      .containsEntry("abc", "def")
      .containsEntry("123", "456");
  }
}
