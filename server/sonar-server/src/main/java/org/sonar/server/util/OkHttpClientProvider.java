/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.util;

import okhttp3.OkHttpClient;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.process.ProcessProperties;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static java.lang.String.format;

/**
 * Provide a unique instance of {@link OkHttpClient} which configuration:
 * <ul>
 *   <li>supports HTTPS</li>
 *   <li>supports proxy, including authentication, as defined by the properties like "http.proxyHost" in
 *   conf/sonar.properties</li>
 *   <li>has connect and read timeouts of 10 seconds each</li>
 *   <li>sends automatically the HTTP header "User-Agent" with value "SonarQube/{version}", for instance "SonarQube/6.2"</li>
 * </ul>
 */
@ServerSide
@ComputeEngineSide
public class OkHttpClientProvider extends ProviderAdapter {

  private static final int DEFAULT_CONNECT_TIMEOUT_IN_MS = 10_000;
  private static final int DEFAULT_READ_TIMEOUT_IN_MS = 10_000;

  private okhttp3.OkHttpClient okHttpClient;

  /**
   * @return a {@link OkHttpClient} singleton
   */
  public OkHttpClient provide(Settings settings, SonarRuntime runtime) {
    if (okHttpClient == null) {
      OkHttpClientBuilder builder = new OkHttpClientBuilder();
      builder.setConnectTimeoutMs(DEFAULT_CONNECT_TIMEOUT_IN_MS);
      builder.setReadTimeoutMs(DEFAULT_READ_TIMEOUT_IN_MS);
      // no need to define proxy URL as system-wide proxy is used and properly
      // configured by bootstrap process.
      builder.setProxyLogin(settings.getString(ProcessProperties.HTTP_PROXY_USER));
      builder.setProxyPassword(settings.getString(ProcessProperties.HTTP_PROXY_PASSWORD));
      builder.setUserAgent(format("SonarQube/%s", runtime.getApiVersion().toString()));
      okHttpClient = builder.build();
    }
    return okHttpClient;
  }
}
