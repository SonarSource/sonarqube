/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.Plugin;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.plugin.PluginType.EXTERNAL;

public class ServerPluginManagerTest {

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private PluginClassLoader pluginClassLoader = mock(PluginClassLoader.class);
  private PluginJarExploder jarExploder = mock(PluginJarExploder.class);
  private PluginJarLoader jarLoader = mock(PluginJarLoader.class);
  private ServerPluginRepository pluginRepository = new ServerPluginRepository();
  private ServerPluginManager underTest = new ServerPluginManager(pluginClassLoader, jarExploder, jarLoader, pluginRepository);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void load_plugins() throws IOException {
    ServerPluginInfo p1 = newPluginInfo("p1");
    ServerPluginInfo p2 = newPluginInfo("p2");
    File p1ExplodedJar = temp.newFile("p1-exploded.jar");
    File p2ExplodedJar = temp.newFile("p2-exploded.jar");
    when(jarLoader.loadPlugins()).thenReturn(Arrays.asList(p1, p2));
    when(jarExploder.explode(p1)).thenReturn(new ExplodedPlugin(p1, "p1", p1ExplodedJar, Collections.singletonList(new File("libP1.jar"))));
    when(jarExploder.explode(p2)).thenReturn(new ExplodedPlugin(p2, "p2", p2ExplodedJar, Collections.singletonList(new File("libP2.jar"))));

    Map<String, Plugin> instances = ImmutableMap.of("p1", mock(Plugin.class), "p2", mock(Plugin.class));
    when(pluginClassLoader.load(anyList())).thenReturn(instances);

    underTest.start();

    assertEquals(2, pluginRepository.getPlugins().size());

    assertEquals(p1, pluginRepository.getPlugin("p1").getPluginInfo());
    assertEquals(newFileAndMd5(p1ExplodedJar).getFile(), pluginRepository.getPlugin("p1").getJar().getFile());
    assertEquals(newFileAndMd5(p1ExplodedJar).getMd5(), pluginRepository.getPlugin("p1").getJar().getMd5());
    assertEquals(instances.get("p1"), pluginRepository.getPlugin("p1").getInstance());

    assertEquals(p2, pluginRepository.getPlugin("p2").getPluginInfo());
    assertEquals(newFileAndMd5(p2ExplodedJar).getFile(), pluginRepository.getPlugin("p2").getJar().getFile());
    assertEquals(newFileAndMd5(p2ExplodedJar).getMd5(), pluginRepository.getPlugin("p2").getJar().getMd5());
    assertEquals(instances.get("p2"), pluginRepository.getPlugin("p2").getInstance());

    assertThat(pluginRepository.getPlugins()).extracting(ServerPlugin::getPluginInfo)
      .allMatch(p -> logTester.logs().contains(String.format("Deploy %s / %s / %s", p.getName(), p.getVersion(), p.getImplementationBuild())));
  }

  /**
   * For a ZIP-packaged plugin, {@link org.sonar.core.platform.PluginInfo#getNonNullJarFile()} is the
   * outer zip, not a real jar - only the exploded main file is a classloadable/downloadable jar.
   */
  @Test
  public void plugin_jar_is_based_on_the_exploded_main_file_not_the_installed_file() throws IOException {
    ServerPluginInfo p1 = newPluginInfo("p1");
    File explodedJar = temp.newFile("exploded-differs-from-installed.jar");
    when(jarLoader.loadPlugins()).thenReturn(Collections.singletonList(p1));
    when(jarExploder.explode(p1)).thenReturn(new ExplodedPlugin(p1, "p1", explodedJar, Collections.emptyList()));
    when(pluginClassLoader.load(anyList())).thenReturn(ImmutableMap.of("p1", mock(Plugin.class)));

    underTest.start();

    assertThat(pluginRepository.getPlugin("p1").getJar().getFile())
      .isEqualTo(explodedJar)
      .isNotEqualTo(p1.getNonNullJarFile());
  }

  private ServerPluginInfo newPluginInfo(String key) throws IOException {
    ServerPluginInfo pluginInfo = mock(ServerPluginInfo.class);
    when(pluginInfo.getKey()).thenReturn(key);
    when(pluginInfo.getType()).thenReturn(EXTERNAL);
    when(pluginInfo.getNonNullJarFile()).thenReturn(temp.newFile(key + ".jar"));
    when(pluginInfo.getName()).thenReturn(key + "_name");
    Version version = mock(Version.class);
    when(version.getName()).thenReturn(key + "_version");
    when(pluginInfo.getVersion()).thenReturn(version);
    when(pluginInfo.getImplementationBuild()).thenReturn(key + "_implementationBuild");
    return pluginInfo;
  }

  private static FileAndMd5 newFileAndMd5(File file) {
    return new PluginFilesAndMd5.FileAndMd5(file);
  }
}
