/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServerClientTest {

  @Test
  public void shouldExtractId() throws Exception {
    ServerClient server = new ServerClient(new Settings(), mock(EnvironmentInformation.class));
    assertThat(server.extractServerId("{\"id\":\"123456\",\"version\":\"3.1\",\"status\":\"UP\"}")).isEqualTo("123456");
  }

  @Test
  public void shouldRemoveUrlEndingSlash() throws Exception {
    Settings settings = new Settings();
    settings.setProperty("sonar.host.url", "http://localhost:8080/sonar/");
    ServerClient server = new ServerClient(settings, new EnvironmentInformation("Junit", "4"));

    assertThat(server.getURL()).isEqualTo("http://localhost:8080/sonar");
  }

  @Test
  public void shouldLoadServerProperties() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.SERVER_ID, "123");
    settings.setProperty(CoreProperties.SERVER_VERSION, "2.2");
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "2010-05-18T17:59:00+0000");
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, "abcde");
    settings.setProperty("sonar.host.url", "http://foo.com");

    ServerClient server = new ServerClient(settings, mock(EnvironmentInformation.class));

    assertThat(server.getId()).isEqualTo("123");
    assertThat(server.getVersion()).isEqualTo("2.2");
    assertThat(server.getStartedAt().getDate()).isEqualTo(18);
    assertThat(server.getURL()).isEqualTo("http://foo.com");
    assertThat(server.getPermanentServerId()).isEqualTo("abcde");
  }
}
