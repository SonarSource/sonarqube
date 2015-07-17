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
package org.sonar.server.plugins;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.UriReader;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateCenterClientTest {

  static final String BASE_URL = "http://update.sonarsource.org";
  UriReader reader;
  Settings settings;

  UpdateCenterClient underTest;

  @Before
  public void startServer() throws Exception {
    reader = mock(UriReader.class);
    settings = new Settings()
      .setProperty(UpdateCenterClient.URL_PROPERTY, BASE_URL)
      .setProperty(UpdateCenterClient.ACTIVATION_PROPERTY, true);
    underTest = new UpdateCenterClient(reader, settings);
  }

  @Test
  public void downloadUpdateCenter() throws URISyntaxException {
    when(reader.readString(new URI(BASE_URL), StandardCharsets.UTF_8)).thenReturn("publicVersions=2.2,2.3");
    UpdateCenter plugins = underTest.getUpdateCenter().get();
    verify(reader, times(1)).readString(new URI(BASE_URL), StandardCharsets.UTF_8);
    assertThat(plugins.getSonar().getVersions()).containsOnly(Version.create("2.2"), Version.create("2.3"));
    assertThat(underTest.getLastRefreshDate()).isNotNull();
  }

  @Test
  public void not_available_before_initialization() {
    assertThat(underTest.getLastRefreshDate()).isNull();
  }

  @Test
  public void ignore_connection_errors() {
    when(reader.readString(any(URI.class), eq(StandardCharsets.UTF_8))).thenThrow(new SonarException());
    assertThat(underTest.getUpdateCenter()).isAbsent();
  }

  @Test
  public void cache_data() throws Exception {
    when(reader.readString(new URI(BASE_URL), StandardCharsets.UTF_8)).thenReturn("sonar.versions=2.2,2.3");

    underTest.getUpdateCenter();
    underTest.getUpdateCenter();

    verify(reader, times(1)).readString(new URI(BASE_URL), StandardCharsets.UTF_8);
  }

  @Test
  public void forceRefresh() throws Exception {
    when(reader.readString(new URI(BASE_URL), StandardCharsets.UTF_8)).thenReturn("sonar.versions=2.2,2.3");

    underTest.getUpdateCenter();
    underTest.getUpdateCenter(true);

    verify(reader, times(2)).readString(new URI(BASE_URL), StandardCharsets.UTF_8);
  }

  @Test
  public void update_center_is_null_when_property_is_false() {
    settings.setProperty(UpdateCenterClient.ACTIVATION_PROPERTY, false);

    assertThat(underTest.getUpdateCenter()).isAbsent();
  }
}
