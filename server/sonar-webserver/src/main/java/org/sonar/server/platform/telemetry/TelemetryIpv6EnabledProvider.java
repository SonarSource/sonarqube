/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.telemetry;

import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryIpv6EnabledProvider extends AbstractTelemetryDataProvider<Integer> {

  final NetworkUtils networkUtils;

  private final Configuration configuration;

  public TelemetryIpv6EnabledProvider(NetworkUtils networkUtils, Configuration configuration) {
    super("internet_protocol_version", Dimension.INSTALLATION, Granularity.MONTHLY, TelemetryDataType.INTEGER);
    this.networkUtils = networkUtils;
    this.configuration = configuration;
  }

  @Override
  public Optional<Integer> getValue() {
    if (isIpv6Address()) {
      return Optional.of(6);
    } else {
      return Optional.of(4);
    }
  }

  private boolean isIpv6Address() {
    String ipAddress = configuration.get(ProcessProperties.Property.WEB_HOST.getKey()).orElse("");
    return networkUtils.isIpv6Address(ipAddress);
  }
}
