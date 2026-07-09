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

import java.util.Set;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.alm.setting.ALM;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.MessageSerializer;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.BaseMessage;
import org.sonar.telemetry.core.schema.InstallationMetric;
import org.sonar.telemetry.core.schema.Metric;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

/**
 * Sends ADHOC telemetry when a DevOps platform (ALM) integration or GitHub authentication is configured,
 * so we can measure adoption of the manual path vs. the automatic GitHub App Manifest path.
 * <p>
 * SonarQube telemetry has no "event with properties" concept: every datum is a flat {@link Metric}
 * (key/value/type/granularity). Each conceptual event is therefore emitted as one {@link BaseMessage}
 * whose metric key(s) encode the event name and its properties, using the snake_case key convention.
 */
@ServerSide
public class DevOpsConfigurationTelemetry {

  public static final String KEY_DEVOPS_CONFIG_MANUAL_ALM = "devops_config_manual_alm";
  public static final String KEY_DEVOPS_CONFIG_AUTO_ALM = "devops_config_auto_alm";
  public static final String KEY_DEVOPS_CONFIG_AUTO_AUTH_SETUP = "devops_config_auto_auth_setup";
  public static final String KEY_AUTH_CONFIG_MANUAL_ALM = "auth_config_manual_alm";
  public static final String KEY_AUTH_CONFIG_AUTO_ALM = "auth_config_auto_alm";

  private final TelemetryClient telemetryClient;
  private final Server server;
  private final UuidFactory uuidFactory;
  private final Configuration configuration;

  public DevOpsConfigurationTelemetry(TelemetryClient telemetryClient, Server server, UuidFactory uuidFactory, Configuration configuration) {
    this.telemetryClient = telemetryClient;
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.configuration = configuration;
  }

  /**
   * A DevOps platform integration was configured manually (through one of the {@code create_*} ALM setting
   * web services).
   */
  public void sendManualDevOpsConfig(ALM alm) {
    if (!isTelemetryEnabled()) {
      return;
    }
    send(Set.of(metric(KEY_DEVOPS_CONFIG_MANUAL_ALM, alm.getId(), TelemetryDataType.STRING)));
  }

  /**
   * A GitHub DevOps platform integration was configured automatically through the GitHub App Manifest flow.
   *
   * @param authSetup whether GitHub authentication was also set up as part of the same flow
   */
  public void sendAutoDevOpsConfig(boolean authSetup) {
    if (!isTelemetryEnabled()) {
      return;
    }
    send(Set.of(
      metric(KEY_DEVOPS_CONFIG_AUTO_ALM, ALM.GITHUB.getId(), TelemetryDataType.STRING),
      metric(KEY_DEVOPS_CONFIG_AUTO_AUTH_SETUP, authSetup, TelemetryDataType.BOOLEAN)));
  }

  /**
   * GitHub authentication was configured manually (through the GitHub configuration web service).
   */
  public void sendManualAuthConfig() {
    if (!isTelemetryEnabled()) {
      return;
    }
    send(Set.of(metric(KEY_AUTH_CONFIG_MANUAL_ALM, ALM.GITHUB.getId(), TelemetryDataType.STRING)));
  }

  /**
   * GitHub authentication was configured automatically through the GitHub App Manifest flow.
   */
  public void sendAutoAuthConfig() {
    if (!isTelemetryEnabled()) {
      return;
    }
    send(Set.of(metric(KEY_AUTH_CONFIG_AUTO_ALM, ALM.GITHUB.getId(), TelemetryDataType.STRING)));
  }

  private void send(Set<Metric> metrics) {
    BaseMessage message = new BaseMessage.Builder()
      .setMessageUuid(uuidFactory.create())
      .setInstallationId(server.getId())
      .setDimension(Dimension.INSTALLATION)
      .setMetrics(metrics)
      .build();
    telemetryClient.uploadMetricAsync(MessageSerializer.serialize(message));
  }

  private static InstallationMetric metric(String key, Object value, TelemetryDataType type) {
    return new InstallationMetric(key, value, type, Granularity.ADHOC);
  }

  private boolean isTelemetryEnabled() {
    return configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false);
  }
}
