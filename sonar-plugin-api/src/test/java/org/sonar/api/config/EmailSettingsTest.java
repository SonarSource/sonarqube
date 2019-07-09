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
package org.sonar.api.config;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailSettingsTest {

  private MapSettings settings = new MapSettings();
  private Server server = mock(Server.class);
  private EmailSettings underTest = new EmailSettings(settings.asConfig(), server);

  @Test
  public void should_return_default_values() {
    assertThat(underTest.getSmtpHost()).isEqualTo("");
    assertThat(underTest.getSmtpPort()).isEqualTo(25);
    assertThat(underTest.getSmtpUsername()).isEmpty();
    assertThat(underTest.getSmtpPassword()).isEmpty();
    assertThat(underTest.getSecureConnection()).isEmpty();
    assertThat(underTest.getFrom()).isEqualTo("noreply@nowhere");
    assertThat(underTest.getFromName()).isEqualTo("SonarQube");
    assertThat(underTest.getPrefix()).isEqualTo("[SONARQUBE]");
  }

  @Test
  public void getServerBaseUrl_returns_server_getPublicRootUrl() {
    String expected = RandomStringUtils.randomAlphabetic(15);
    when(server.getPublicRootUrl()).thenReturn(expected);

    assertThat(underTest.getServerBaseURL()).isEqualTo(expected);
  }

  @Test
  public void return_definitions() {
    assertThat(EmailSettings.definitions()).hasSize(8);
  }
}
