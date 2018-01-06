/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.scanner.bootstrap.ScannerWsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerTest {

  @Test
  public void shouldLoadServerProperties() {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.SERVER_ID, "123");
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "2010-05-18T17:59:00+0000");
    ScannerWsClient client = mock(ScannerWsClient.class);
    when(client.baseUrl()).thenReturn("http://foo.com");

    DefaultServer metadata = new DefaultServer(settings, client, SonarRuntimeImpl.forSonarQube(Version.parse("2.2"), SonarQubeSide.SCANNER));

    assertThat(metadata.getId()).isEqualTo("123");
    assertThat(metadata.getVersion()).isEqualTo("2.2");
    assertThat(metadata.getStartedAt()).isNotNull();
    assertThat(metadata.getURL()).isEqualTo("http://foo.com");
    assertThat(metadata.getPermanentServerId()).isEqualTo("123");

    assertThat(metadata.getRootDir()).isNull();
    assertThat(metadata.getContextPath()).isNull();
    assertThat(metadata.isDev()).isFalse();
    assertThat(metadata.isSecured()).isFalse();
  }

  @Test
  public void publicRootUrl() {
    Settings settings = new MapSettings();
    ScannerWsClient client = mock(ScannerWsClient.class);
    when(client.baseUrl()).thenReturn("http://foo.com/");
    DefaultServer metadata = new DefaultServer(settings, client, null);

    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://server.com/");
    assertThat(metadata.getPublicRootUrl()).isEqualTo("http://server.com");

    settings.removeProperty(CoreProperties.SERVER_BASE_URL);
    assertThat(metadata.getPublicRootUrl()).isEqualTo("http://foo.com");
  }

  @Test(expected = RuntimeException.class)
  public void invalid_startup_date_throws_exception() {
    Settings settings = new MapSettings();
    settings.setProperty(CoreProperties.SERVER_STARTTIME, "invalid");
    ScannerWsClient client = mock(ScannerWsClient.class);
    DefaultServer metadata = new DefaultServer(settings, client, null);
    metadata.getStartedAt();
  }
}
