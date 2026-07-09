/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings.telemetry;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.alm.setting.ALM;
import org.sonar.telemetry.core.TelemetryClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

class DevOpsConfigurationTelemetryTest {

  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;
  private final Server server = mock(Server.class);
  private final TelemetryClient telemetryClient = mock(TelemetryClient.class);
  private final Configuration configuration = mock(Configuration.class);

  private final DevOpsConfigurationTelemetry underTest = new DevOpsConfigurationTelemetry(telemetryClient, server, uuidFactory, configuration);

  @BeforeEach
  void setUp() {
    when(server.getId()).thenReturn("serverId");
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
  }

  @Test
  void sendManualDevOpsConfig_sendsAlmMetric() {
    underTest.sendManualDevOpsConfig(ALM.AZURE_DEVOPS);

    assertJson(capturePayload()).isSimilarTo("""
      {
        "dimension": "installation",
        "metric_values": [
          {"key": "devops_config_manual_alm", "value": "azure_devops", "type": "string", "granularity": "adhoc"}
        ]
      }
      """);
  }

  @Test
  void sendAutoDevOpsConfig_sendsAlmAndAuthSetupMetrics() {
    underTest.sendAutoDevOpsConfig(true);

    assertJson(capturePayload()).isSimilarTo("""
      {
        "dimension": "installation",
        "metric_values": [
          {"key": "devops_config_auto_alm", "value": "github", "type": "string", "granularity": "adhoc"},
          {"key": "devops_config_auto_auth_setup", "value": true, "type": "boolean", "granularity": "adhoc"}
        ]
      }
      """);
  }

  @Test
  void sendManualAuthConfig_sendsGithubAlmMetric() {
    underTest.sendManualAuthConfig();

    assertJson(capturePayload()).isSimilarTo("""
      {
        "dimension": "installation",
        "metric_values": [
          {"key": "auth_config_manual_alm", "value": "github", "type": "string", "granularity": "adhoc"}
        ]
      }
      """);
  }

  @Test
  void sendAutoAuthConfig_sendsGithubAlmMetric() {
    underTest.sendAutoAuthConfig();

    assertJson(capturePayload()).isSimilarTo("""
      {
        "dimension": "installation",
        "metric_values": [
          {"key": "auth_config_auto_alm", "value": "github", "type": "string", "granularity": "adhoc"}
        ]
      }
      """);
  }

  @Test
  void send_whenTelemetryDisabled_doesNotCallClient() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));

    underTest.sendManualDevOpsConfig(ALM.GITHUB);
    underTest.sendAutoDevOpsConfig(false);
    underTest.sendManualAuthConfig();
    underTest.sendAutoAuthConfig();

    verifyNoInteractions(telemetryClient);
  }

  private String capturePayload() {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient).uploadMetricAsync(captor.capture());
    return captor.getValue();
  }
}
