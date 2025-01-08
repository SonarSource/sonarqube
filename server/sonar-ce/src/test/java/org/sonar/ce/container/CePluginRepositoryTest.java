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
package org.sonar.ce.container;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CePluginRepositoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem fs = mock(ServerFileSystem.class, Mockito.RETURNS_DEEP_STUBS);
  private PluginClassLoader pluginClassLoader = new DumbPluginClassLoader();
  private CePluginJarExploder cePluginJarExploder = new CePluginJarExploder(fs);
  private CePluginRepository underTest = new CePluginRepository(fs, pluginClassLoader, cePluginJarExploder);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void empty_plugins() throws Exception {
    // empty folder
    when(fs.getInstalledExternalPluginsDir()).thenReturn(temp.newFolder());

    underTest.start();

    assertThat(underTest.getPluginInfos()).isEmpty();
    assertThat(underTest.hasPlugin("foo")).isFalse();
  }

  @Test
  public void load_plugins() throws IOException {
    String pluginKey = "test";
    when(fs.getTempDir()).thenReturn(temp.newFolder());
    when(fs.getInstalledExternalPluginsDir()).thenReturn(new File("src/test/plugins/sonar-test-plugin/target"));

    underTest.start();

    assertThat(underTest.getPluginInfos()).extracting("key").containsOnly(pluginKey);
    assertThat(underTest.getPluginInfo(pluginKey).getKey()).isEqualTo(pluginKey);
    assertThat(underTest.getPluginInstance(pluginKey)).isNotNull();
    assertThat(underTest.getPluginInstances()).isNotEmpty();
    assertThat(underTest.hasPlugin(pluginKey)).isTrue();
  }

  @Test
  public void getPluginInfo_fails_if_plugin_does_not_exist() throws Exception {
    // empty folder
    when(fs.getInstalledExternalPluginsDir()).thenReturn(temp.newFolder());
    underTest.start();
    assertThatThrownBy(() -> underTest.getPluginInfo("foo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Plugin [foo] does not exist");
  }

  @Test
  public void getPluginInstance_fails_if_plugin_does_not_exist() throws Exception {
    // empty folder
    when(fs.getInstalledExternalPluginsDir()).thenReturn(temp.newFolder());
    underTest.start();
    assertThatThrownBy(() -> underTest.getPluginInstance("foo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Plugin [foo] does not exist");
  }

  @Test
  public void getPluginInstance_throws_ISE_if_repo_is_not_started() {
    assertThatThrownBy(() -> underTest.getPluginInstance("foo"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("not started yet");
  }

  @Test
  public void getPluginInfo_throws_ISE_if_repo_is_not_started() {
    assertThatThrownBy(() -> underTest.getPluginInfo("foo"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("not started yet");
  }

  @Test
  public void hasPlugin_throws_ISE_if_repo_is_not_started() {
    assertThatThrownBy(() -> underTest.hasPlugin("foo"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("not started yet");
  }

  @Test
  public void getPluginInfos_throws_ISE_if_repo_is_not_started() {
    assertThatThrownBy(() -> underTest.getPluginInfos())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("not started yet");
  }

  private static class DumbPluginClassLoader extends PluginClassLoader {

    public DumbPluginClassLoader() {
      super(null);
    }

    /**
     * Does nothing except returning the specified list of plugins
     */
    @Override
    public Map<String, Plugin> load(Map<String, ExplodedPlugin> infoByKeys) {
      Map<String, Plugin> result = new HashMap<>();
      for (String pluginKey : infoByKeys.keySet()) {
        result.put(pluginKey, mock(Plugin.class));
      }
      return result;
    }

    @Override
    public void unload(Collection<Plugin> plugins) {

    }
  }
}
