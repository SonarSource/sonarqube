/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.platform;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.scanner.http.DefaultScannerWsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerTest {

  @Test
  public void shouldLoadServerProperties() {
    MapSettings settings = new MapSettings();
    settings.setProperty(CoreProperties.SERVER_ID, "123");
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "2010-05-18T17:59:00+0000");
    DefaultScannerWsClient client = mock(DefaultScannerWsClient.class);
    when(client.baseUrl()).thenReturn("http://foo.com");

    DefaultServer metadata = new DefaultServer((settings).asConfig(), client, new SonarQubeVersion(Version.parse("2.2")));

    assertThat(metadata.getId()).isEqualTo("123");
    assertThat(metadata.getVersion()).isEqualTo("2.2");
    assertThat(metadata.getStartedAt()).isNotNull();
    assertThat(metadata.getPublicRootUrl()).isEqualTo("http://foo.com");

    assertThat(metadata.getContextPath()).isNull();
  }

  @Test
  public void publicRootUrl() {
    MapSettings settings = new MapSettings();
    DefaultScannerWsClient client = mock(DefaultScannerWsClient.class);
    when(client.baseUrl()).thenReturn("http://foo.com/");
    DefaultServer metadata = new DefaultServer(settings.asConfig(), client, null);

    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://server.com/");
    assertThat(metadata.getPublicRootUrl()).isEqualTo("http://server.com");

    settings.removeProperty(CoreProperties.SERVER_BASE_URL);
    assertThat(metadata.getPublicRootUrl()).isEqualTo("http://foo.com");
  }

  @Test(expected = RuntimeException.class)
  public void invalid_startup_date_throws_exception() {
    MapSettings settings = new MapSettings();
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "invalid");
    DefaultScannerWsClient client = mock(DefaultScannerWsClient.class);
    DefaultServer metadata = new DefaultServer(settings.asConfig(), client, null);
    metadata.getStartedAt();
  }
}
