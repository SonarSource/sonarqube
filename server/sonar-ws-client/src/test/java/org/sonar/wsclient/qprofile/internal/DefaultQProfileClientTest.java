/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.wsclient.qprofile.internal;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.qprofile.QProfileResult;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultQProfileClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void restore_default_profiles() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"infos\":[\"Some info\"],\"warnings\":[\"Some warnings\"]}");

    DefaultQProfileClient client = new DefaultQProfileClient(requestFactory);
    QProfileResult result = client.restoreDefault("java");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualityprofiles/restore_default");
    assertThat(httpServer.requestParams()).containsEntry("language", "java");
    assertThat(result).isNotNull();
    assertThat(result.infos()).containsOnly("Some info");
    assertThat(result.warnings()).containsOnly("Some warnings");
  }

}
