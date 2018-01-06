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
package org.sonar.server.platform.monitoring;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class PluginsSectionTest {

  private PluginRepository repo = mock(PluginRepository.class);
  private PluginsSection underTest = new PluginsSection(repo);

  @Test
  public void name() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Plugins");
  }

  @Test
  public void plugin_name_and_version() {
    when(repo.getPluginInfos()).thenReturn(Arrays.asList(
      new PluginInfo("key-1")
        .setName("Plugin 1")
        .setVersion(Version.create("1.1")),
      new PluginInfo("key-2")
        .setName("Plugin 2")
        .setVersion(Version.create("2.2")),
      new PluginInfo("no-version")
        .setName("No Version")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "key-1", "1.1 [Plugin 1]");
    assertThatAttributeIs(section, "key-2", "2.2 [Plugin 2]");
    assertThatAttributeIs(section, "no-version", "[No Version]");
  }
}
