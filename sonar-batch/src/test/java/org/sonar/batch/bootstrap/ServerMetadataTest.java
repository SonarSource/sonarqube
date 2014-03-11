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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerMetadataTest {

  @Test
  public void shouldExtractId() throws Exception {
    ServerMetadata metadata = new ServerMetadata(new Settings(), mock(ServerClient.class));
    assertThat(metadata.extractServerId("{\"id\":\"123456\",\"version\":\"3.1\",\"status\":\"UP\"}")).isEqualTo("123456");
  }

  @Test
  public void shouldLoadServerProperties() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.SERVER_ID, "123");
    settings.setProperty(CoreProperties.SERVER_VERSION, "2.2");
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "2010-05-18T17:59:00+0000");
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, "abcde");
    ServerClient client = mock(ServerClient.class);
    when(client.getURL()).thenReturn("http://foo.com");

    ServerMetadata metadata = new ServerMetadata(settings, client);

    assertThat(metadata.getId()).isEqualTo("123");
    assertThat(metadata.getVersion()).isEqualTo("2.2");
    assertThat(metadata.getStartedAt().getDate()).isEqualTo(18);
    assertThat(metadata.getURL()).isEqualTo("http://foo.com");
    assertThat(metadata.getPermanentServerId()).isEqualTo("abcde");
  }
}
