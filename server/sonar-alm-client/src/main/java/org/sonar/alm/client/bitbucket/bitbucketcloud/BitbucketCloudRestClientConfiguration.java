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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import okhttp3.OkHttpClient;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonarqube.ws.client.OkHttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@ComputeEngineSide
@ServerSide
@Configuration
@Lazy
public class BitbucketCloudRestClientConfiguration {

  private final TimeoutConfiguration timeoutConfiguration;

  @Autowired
  public BitbucketCloudRestClientConfiguration(TimeoutConfiguration timeoutConfiguration) {
    this.timeoutConfiguration = timeoutConfiguration;
  }

  @Bean
  public OkHttpClient bitbucketCloudHttpClient() {
    OkHttpClientBuilder builder = new OkHttpClientBuilder();
    builder.setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout());
    builder.setReadTimeoutMs(timeoutConfiguration.getReadTimeout());
    builder.setFollowRedirects(false);
    return builder.build();
  }

  @Bean
  public BitbucketCloudRestClient bitbucketCloudRestClient() {
    return new BitbucketCloudRestClient(bitbucketCloudHttpClient());
  }
}
