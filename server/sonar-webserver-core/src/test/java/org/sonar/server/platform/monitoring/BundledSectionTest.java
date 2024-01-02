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
package org.sonar.server.platform.monitoring;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class BundledSectionTest {

  private ServerPluginRepository repo = mock(ServerPluginRepository.class);
  private BundledSection underTest = new BundledSection(repo);

  @Test
  public void name() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Bundled");
  }

  @Test
  public void toProtobuf_given3BundledPlugins_returnThree() {
    when(repo.getPluginsInfoByType(PluginType.BUNDLED)).thenReturn(Arrays.asList(
      new PluginInfo("java")
        .setName("Java")
        .setVersion(Version.create("20.0")),
      new PluginInfo("c++")
        .setName("C++")
        .setVersion(Version.create("1.0.2")),
      new PluginInfo("no-version")
        .setName("No Version")));

    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "java", "20.0 [Java]");
    assertThatAttributeIs(section, "c++", "1.0.2 [C++]");
    assertThatAttributeIs(section, "no-version", "[No Version]");
  }
}
