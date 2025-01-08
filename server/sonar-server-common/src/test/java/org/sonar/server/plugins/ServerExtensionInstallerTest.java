/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.ListContainer;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerExtensionInstallerTest {
  private SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarQube(Version.parse("8.0"), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
  private TestPluginRepository pluginRepository = new TestPluginRepository();
  private TestServerExtensionInstaller underTest = new TestServerExtensionInstaller(sonarRuntime, pluginRepository);

  @Test
  public void add_plugin_to_container() {
    PluginInfo fooPluginInfo = newPlugin("foo", "Foo");
    Plugin fooPlugin = mock(Plugin.class);
    pluginRepository.add(fooPluginInfo, fooPlugin);
    ListContainer componentContainer = new ListContainer();

    underTest.installExtensions(componentContainer);

    assertThat(componentContainer.getAddedObjects()).contains(fooPlugin);
  }

  private static PluginInfo newPlugin(String key, String name) {
    PluginInfo plugin = mock(PluginInfo.class);
    when(plugin.getKey()).thenReturn(key);
    when(plugin.getName()).thenReturn(name);
    return plugin;
  }

  private static class TestPluginRepository implements PluginRepository {
    private final Map<String, PluginInfo> pluginsInfoMap = new HashMap<>();
    private final Map<String, Plugin> pluginsMap = new HashMap<>();

    void add(PluginInfo pluginInfo, Plugin plugin) {
      pluginsInfoMap.put(pluginInfo.getKey(), pluginInfo);
      pluginsMap.put(pluginInfo.getKey(), plugin);
    }

    @Override
    public Collection<PluginInfo> getPluginInfos() {
      return pluginsInfoMap.values();
    }

    @Override
    public PluginInfo getPluginInfo(String key) {
      if (!pluginsMap.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return pluginsInfoMap.get(key);
    }

    @Override
    public Plugin getPluginInstance(String key) {
      if (!pluginsMap.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return pluginsMap.get(key);
    }

    @Override
    public Collection<Plugin> getPluginInstances() {
      return pluginsMap.values();
    }

    @Override
    public boolean hasPlugin(String key) {
      return pluginsMap.containsKey(key);
    }
  }

  private static class TestServerExtensionInstaller extends ServerExtensionInstaller {

    protected TestServerExtensionInstaller(SonarRuntime sonarRuntime, PluginRepository pluginRepository) {
      super(mock(Configuration.class), sonarRuntime, pluginRepository, singleton(ServerSide.class));
    }
  }

}
