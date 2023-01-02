/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Collections;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.core.plugin.PluginType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScannerPluginRepositoryTest {

  PluginInstaller installer = mock(PluginInstaller.class);
  PluginClassLoader loader = mock(PluginClassLoader.class);
  PluginJarExploder exploder = new FakePluginJarExploder();
  ScannerPluginRepository underTest = new ScannerPluginRepository(installer, exploder, loader);

  @Test
  public void install_and_load_plugins() {
    PluginInfo info = new PluginInfo("java");
    ImmutableMap<String, ScannerPlugin> plugins = ImmutableMap.of("java", new ScannerPlugin("java", 1L, PluginType.EXTERNAL, info));
    Plugin instance = mock(Plugin.class);
    when(loader.load(anyMap())).thenReturn(ImmutableMap.of("java", instance));
    when(installer.installRemotes()).thenReturn(plugins);

    underTest.start();

    assertThat(underTest.getPluginInfos()).containsOnly(info);
    assertThat(underTest.getPluginsByKey()).isEqualTo(plugins);
    assertThat(underTest.getPluginInfo("java")).isSameAs(info);
    assertThat(underTest.getPluginInstance("java")).isSameAs(instance);
    assertThat(underTest.getPluginInstances()).containsOnly(instance);
    assertThat(underTest.getBundledPluginsInfos()).isEmpty();
    assertThat(underTest.getExternalPluginsInfos()).isEqualTo(underTest.getPluginInfos());

    underTest.stop();
    verify(loader).unload(anyCollection());
  }

  @Test
  public void fail_if_requesting_missing_plugin() {
    underTest.start();

    try {
      underTest.getPluginInfo("unknown");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }
    try {
      underTest.getPluginInstance("unknown");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }
  }

  private static class FakePluginJarExploder extends PluginJarExploder {
    @Override
    public ExplodedPlugin explode(PluginInfo plugin) {
      return new ExplodedPlugin(plugin, plugin.getKey(), new File(plugin.getKey() + ".jar"), Collections
        .singleton(new File(plugin.getKey() + "-lib.jar")));
    }

  }
}
