/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client;

import org.junit.Test;
import org.sonar.api.server.ws.LocalConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WsClientFactoriesTest {
  @Test
  public void create_http_client() {
    HttpConnector connector = HttpConnector.newBuilder().url("http://localhost:9000").build();
    WsClient client = WsClientFactories.getDefault().newClient(connector);

    assertThat(client).isInstanceOf(DefaultWsClient.class);
    assertThat(client.wsConnector()).isSameAs(connector);
  }

  @Test
  public void create_local_client() {
    LocalConnector connector = mock(LocalConnector.class);
    WsClient client = WsClientFactories.getLocal().newClient(connector);

    assertThat(client).isInstanceOf(DefaultWsClient.class);
    assertThat(client.wsConnector()).isInstanceOf(LocalWsConnector.class);
    assertThat(((LocalWsConnector) client.wsConnector()).localConnector()).isSameAs(connector);
  }
}
