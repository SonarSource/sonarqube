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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerImplTest {
  private final MapSettings settings = new MapSettings();
  private final StartupMetadata state = mock(StartupMetadata.class);
  private final UrlSettings urlSettings = mock(UrlSettings.class);
  private final SonarQubeVersion sonarQubeVersion = mock(SonarQubeVersion.class);
  private final ServerImpl underTest = new ServerImpl(settings.asConfig(), state, urlSettings, sonarQubeVersion);

  @Test
  public void test_url_information() {
    when(urlSettings.getContextPath()).thenReturn("/foo");
    when(urlSettings.getBaseUrl()).thenReturn("http://localhost:9000/foo");
    when(urlSettings.isSecured()).thenReturn(false);

    assertThat(underTest.getContextPath()).isEqualTo("/foo");
    assertThat(underTest.getPublicRootUrl()).isEqualTo("http://localhost:9000/foo");
  }

  @Test
  public void test_startup_information() {
    long time = 123_456_789L;
    when(state.getStartedAt()).thenReturn(time);

    assertThat(underTest.getStartedAt().getTime()).isEqualTo(time);
  }

  @Test
  public void test_id() {
    settings.setProperty(CoreProperties.SERVER_ID, "foo");

    assertThat(underTest.getId()).isEqualTo("foo");
  }

  @Test
  public void test_getVersion() {
    Version version = Version.create(6, 1);
    when(sonarQubeVersion.get()).thenReturn(version);

    assertThat(underTest.getVersion()).isEqualTo(version.toString());
  }
}
