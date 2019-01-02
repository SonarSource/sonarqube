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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.PluginReferential;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginReferentialMetadataConverterTest {

  @Test
  public void should_convert_info_to_plugin_referential() {
    PluginInfo info = new PluginInfo("foo");

    PluginReferential pluginReferential = PluginReferentialMetadataConverter.getInstalledPluginReferential(newArrayList(info));
    assertThat(pluginReferential).isNotNull();
    assertThat(pluginReferential.getPlugins()).hasSize(1);
    assertThat(pluginReferential.getPlugins().get(0).getKey()).isEqualTo("foo");
  }

}
