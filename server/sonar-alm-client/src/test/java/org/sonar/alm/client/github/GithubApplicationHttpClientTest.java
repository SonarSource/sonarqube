/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.alm.client.github;

import okhttp3.OkHttpClient;
import org.junit.Test;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.alm.client.TimeoutConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubApplicationHttpClientTest {

  @Test
  public void constructor_should_initialize_with_okhttpclient() {
    GithubHeaders githubHeaders = new GithubHeaders();
    TimeoutConfiguration timeoutConfiguration = new ConstantTimeoutConfiguration(5000);
    OkHttpClient okHttpClient = new OkHttpClient();

    GithubApplicationHttpClient client = new GithubApplicationHttpClient(githubHeaders, timeoutConfiguration, okHttpClient);

    assertThat(client).isNotNull();
  }
}
