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
package org.sonar.server.webhook;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import okhttp3.Dns;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.server.network.NetworkInterfaceProvider;

import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.SONAR_VALIDATE_WEBHOOKS_PROPERTY;

@ServerSide
@ComputeEngineSide
public class WebhookCustomDns implements Dns {

  private final Configuration configuration;
  private final NetworkInterfaceProvider networkInterfaceProvider;

  public WebhookCustomDns(Configuration configuration, NetworkInterfaceProvider networkInterfaceProvider) {
    this.configuration = configuration;
    this.networkInterfaceProvider = networkInterfaceProvider;
  }

  @NotNull
  @Override
  public List<InetAddress> lookup(@NotNull String host) throws UnknownHostException {
    InetAddress address = InetAddress.getByName(host);
    if (configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS_PROPERTY).orElse(SONAR_VALIDATE_WEBHOOKS_DEFAULT_VALUE)
      && (address.isLoopbackAddress() || address.isAnyLocalAddress() || isLocalAddress(address))) {
      throw new IllegalArgumentException("Invalid URL: loopback and wildcard addresses are not allowed for webhooks.");
    }
    return Collections.singletonList(address);
  }

  private boolean isLocalAddress(InetAddress address)  {
    try {
      return networkInterfaceProvider.getNetworkInterfaceAddresses().stream()
        .anyMatch(a -> a != null && a.equals(address));
    } catch (SocketException e) {
      throw new IllegalArgumentException("Network interfaces could not be fetched.");
    }
  }

}
