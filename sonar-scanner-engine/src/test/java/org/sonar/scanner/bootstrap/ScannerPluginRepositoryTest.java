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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.core.plugin.PluginType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.ScannerProperties.PLUGIN_LOADING_OPTIMIZATION_KEY;

public class ScannerPluginRepositoryTest {

  PluginInstaller installer = mock(PluginInstaller.class);
  PluginClassLoader loader = mock(PluginClassLoader.class);
  PluginJarExploder exploder = new FakePluginJarExploder();
  Configuration properties = mock(Configuration.class);
  ScannerPluginRepository underTest = new ScannerPluginRepository(installer, exploder, loader, properties);

  @Test
  public void install_and_load_plugins() {
    PluginInfo squidInfo = new PluginInfo("squid");
    PluginInfo javaInfo = new PluginInfo("java");
    Map<String, ScannerPlugin> globalPlugins = Map.of("squid", new ScannerPlugin("squid", 1L, PluginType.BUNDLED, squidInfo));
    Map<String, ScannerPlugin> languagePlugins = Map.of("java", new ScannerPlugin("java", 1L, PluginType.EXTERNAL, javaInfo));
    Plugin squidInstance = mock(Plugin.class);
    Plugin javaInstance = mock(Plugin.class);
    when(loader.load(anyMap()))
      .thenReturn(ImmutableMap.of("squid", squidInstance))
      .thenReturn(ImmutableMap.of("java", javaInstance));

    when(properties.getBoolean(PLUGIN_LOADING_OPTIMIZATION_KEY)).thenReturn(Optional.of(true));
    when(installer.installRequiredPlugins()).thenReturn(globalPlugins);
    when(installer.installPluginsForLanguages(anySet())).thenReturn(languagePlugins);

    underTest.start();

    assertThat(underTest.getPluginInfos()).containsOnly(squidInfo);
    assertThat(underTest.getPluginsByKey()).isEqualTo(globalPlugins);
    assertThat(underTest.getPluginInfo("squid")).isSameAs(squidInfo);
    assertThat(underTest.getPluginInstance("squid")).isSameAs(squidInstance);

    Collection<PluginInfo> result = underTest.installPluginsForLanguages(new HashSet<>(List.of("java")));

    assertThat(result).containsOnly(javaInfo);
    assertThat(underTest.getPluginInfos()).containsExactlyInAnyOrder(squidInfo, javaInfo);
    assertThat(underTest.getExternalPluginsInfos()).containsExactlyInAnyOrder(javaInfo);
    assertThat(underTest.getPluginsByKey().values()).containsExactlyInAnyOrder(globalPlugins.get("squid"), languagePlugins.get("java"));

    underTest.stop();
    verify(loader).unload(anyCollection());
  }

  @Test
  public void should_install_all_plugins_when_downloadOnlyRequired_flag_is_false() {
    when(properties.getBoolean(PLUGIN_LOADING_OPTIMIZATION_KEY)).thenReturn(Optional.of(false));
    underTest.start();

    verify(installer).installAllPlugins();
    verify(installer, never()).installRequiredPlugins();

    Collection<PluginInfo> result = underTest.installPluginsForLanguages(Set.of("java"));

    assertThat(result).isEmpty();
    verify(installer, never()).installPluginsForLanguages(any());
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
