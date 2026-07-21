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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@ComputeEngineSide
@ServerSide
@Configuration
@Lazy
public class BitbucketCloudRestClientConfiguration {

  private final TimeoutConfiguration timeoutConfiguration;
  private final OkHttpClient okHttpClient;

  @Autowired
  public BitbucketCloudRestClientConfiguration(TimeoutConfiguration timeoutConfiguration, @Qualifier("okHttpClient") OkHttpClient okHttpClient) {
    this.timeoutConfiguration = timeoutConfiguration;
    this.okHttpClient = okHttpClient;
  }

  @Bean
  public OkHttpClient bitbucketCloudHttpClient() {
    return okHttpClient.newBuilder()
      .connectTimeout(timeoutConfiguration.getConnectTimeout(), TimeUnit.MILLISECONDS)
      .readTimeout(timeoutConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS)
      .followRedirects(false)
      .followSslRedirects(false)
      .build();
  }

  @Bean
  public BitbucketCloudRestClient bitbucketCloudRestClient() {
    return new BitbucketCloudRestClient(bitbucketCloudHttpClient());
  }
}
