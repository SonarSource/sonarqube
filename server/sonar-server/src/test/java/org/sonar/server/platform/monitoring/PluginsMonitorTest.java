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

package org.sonar.server.platform.monitoring;

import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.updatecenter.common.Version;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginsMonitorTest {

  PluginRepository repo = mock(PluginRepository.class);
  PluginsMonitor underTest = new PluginsMonitor(repo);

  @Test
  public void name() {
    assertThat(underTest.name()).isEqualTo("Plugins");
  }

  @Test
  public void plugin_name_and_version() {
    when(repo.getPluginInfos()).thenReturn(Arrays.asList(
      new PluginInfo("key-1")
        .setName("plugin-1")
        .setVersion(Version.create("1.1")),
      new PluginInfo("key-2")
        .setName("plugin-2")
        .setVersion(Version.create("2.2")),
      new PluginInfo("no-version")
        .setName("No Version")));

    LinkedHashMap<String, Object> attributes = underTest.attributes();

    assertThat(attributes).containsKeys("key-1", "key-2");
    assertThat((Map) attributes.get("key-1"))
      .containsEntry("Name", "plugin-1")
      .containsEntry("Version", "1.1");
    assertThat((Map) attributes.get("key-2"))
      .containsEntry("Name", "plugin-2")
      .containsEntry("Version", "2.2");
    assertThat((Map) attributes.get("no-version"))
      .containsEntry("Name", "No Version")
      .doesNotContainKey("Version");
  }
}
