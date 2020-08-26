/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.plugins.PluginType.EXTERNAL;

public class ServerPluginRepositoryTest {
  private ServerPluginRepository repository = new ServerPluginRepository();

  @Test
  public void get_plugin_data() {
    ServerPlugin plugin1 = newPlugin("plugin1");
    ServerPlugin plugin2 = newPlugin("plugin2");

    repository.addPlugins(Collections.singletonList(plugin1));
    repository.addPlugin(plugin2);
    assertThat(repository.getPluginInfos()).containsOnly(plugin1.getPluginInfo(), plugin2.getPluginInfo());
    assertThat(repository.getPluginInstance("plugin1")).isEqualTo(plugin1.getInstance());
    assertThat(repository.getPluginInstances()).containsOnly(plugin1.getInstance(), plugin2.getInstance());
    assertThat(repository.getPlugins()).containsOnly(plugin1, plugin2);
    assertThat(repository.getPlugin("plugin2")).isEqualTo(plugin2);
    assertThat(repository.findPlugin("plugin2")).contains(plugin2);
    assertThat(repository.hasPlugin("plugin2")).isTrue();

    assertThat(repository.findPlugin("nonexisting")).isEmpty();
    assertThat(repository.hasPlugin("nonexisting")).isFalse();
  }

  @Test
  public void fail_getPluginInstance_if_plugin_doesnt_exist() {
    ServerPlugin plugin1 = newPlugin("plugin1");
    ServerPlugin plugin2 = newPlugin("plugin2");

    repository.addPlugins(Arrays.asList(plugin1, plugin2));
    Assert.assertThrows("asd", IllegalArgumentException.class, () -> repository.getPluginInstance("plugin3"));
  }

  @Test
  public void fail_getPluginInfo_if_plugin_doesnt_exist() {
    ServerPlugin plugin1 = newPlugin("plugin1");
    ServerPlugin plugin2 = newPlugin("plugin2");

    repository.addPlugins(Arrays.asList(plugin1, plugin2));
    Assert.assertThrows("asd", IllegalArgumentException.class, () -> repository.getPluginInfo("plugin3"));
  }

  private PluginInfo newPluginInfo(String key) {
    PluginInfo pluginInfo = mock(PluginInfo.class);
    when(pluginInfo.getKey()).thenReturn(key);
    return pluginInfo;
  }

  private ServerPlugin newPlugin(String key) {
    return new ServerPlugin(newPluginInfo(key), EXTERNAL, mock(Plugin.class), mock(FileAndMd5.class), mock(FileAndMd5.class), mock(ClassLoader.class));
  }
}
