/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import okhttp3.Dns;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

import static org.sonar.process.ProcessProperties.Property.SONAR_VALIDATE_WEBHOOKS;

@ServerSide
@ComputeEngineSide
public class WebhookCustomDns implements Dns {

  private final Configuration configuration;

  public WebhookCustomDns(Configuration configuration) {
    this.configuration = configuration;
  }

  @NotNull
  @Override
  public List<InetAddress> lookup(@NotNull String host) throws UnknownHostException {
    InetAddress address = InetAddress.getByName(host);
    if (configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey()).orElse(true)
      && (address.isLoopbackAddress() || address.isAnyLocalAddress())) {
      throw new IllegalArgumentException("Invalid URL: loopback and wildcard addresses are not allowed for webhooks.");
    }
    return Collections.singletonList(address);
  }

}
