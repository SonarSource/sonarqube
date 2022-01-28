/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginClassLoader;
import org.sonar.core.platform.PluginJarExploder;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.plugins.PluginType.EXTERNAL;

public class ServerPluginManagerTest {

  @Rule
  public LogTester logs = new LogTester();

  private PluginClassLoader pluginClassLoader = mock(PluginClassLoader.class);
  private PluginJarExploder jarExploder = mock(PluginJarExploder.class);
  private PluginJarLoader jarLoader = mock(PluginJarLoader.class);
  private PluginCompressor pluginCompressor = mock(PluginCompressor.class);
  private ServerPluginRepository pluginRepository = new ServerPluginRepository();
  private ServerPluginManager underTest = new ServerPluginManager(pluginClassLoader, jarExploder, jarLoader, pluginCompressor, pluginRepository);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void load_plugins() {
    ServerPluginInfo p1 = newPluginInfo("p1");
    ServerPluginInfo p2 = newPluginInfo("p2");
    when(jarLoader.loadPlugins()).thenReturn(Arrays.asList(p1, p2));
    when(jarExploder.explode(p1)).thenReturn(new ExplodedPlugin(p1, "p1", new File("p1Exploded.jar"), Collections.singletonList(new File("libP1.jar"))));
    when(jarExploder.explode(p2)).thenReturn(new ExplodedPlugin(p2, "p2", new File("p2Exploded.jar"), Collections.singletonList(new File("libP2.jar"))));

    Map<String, Plugin> instances = ImmutableMap.of("p1", mock(Plugin.class), "p2", mock(Plugin.class));
    when(pluginClassLoader.load(anyList())).thenReturn(instances);
    PluginFilesAndMd5 p1Files = newPluginFilesAndMd5("p1");
    PluginFilesAndMd5 p2Files = newPluginFilesAndMd5("p2");

    when(pluginCompressor.compress("p1", new File("p1.jar"), new File("p1Exploded.jar"))).thenReturn(p1Files);
    when(pluginCompressor.compress("p2", new File("p2.jar"), new File("p2Exploded.jar"))).thenReturn(p2Files);

    underTest.start();

    assertThat(pluginRepository.getPlugins())
      .extracting(ServerPlugin::getPluginInfo, ServerPlugin::getCompressed, ServerPlugin::getJar, ServerPlugin::getInstance)
      .containsOnly(tuple(p1, p1Files.getCompressedJar(), p1Files.getLoadedJar(), instances.get("p1")),
        tuple(p2, p2Files.getCompressedJar(), p2Files.getLoadedJar(), instances.get("p2")));
  }

  private static ServerPluginInfo newPluginInfo(String key) {
    ServerPluginInfo pluginInfo = mock(ServerPluginInfo.class);
    when(pluginInfo.getKey()).thenReturn(key);
    when(pluginInfo.getType()).thenReturn(EXTERNAL);
    when(pluginInfo.getNonNullJarFile()).thenReturn(new File(key + ".jar"));
    return pluginInfo;
  }

  private static PluginFilesAndMd5 newPluginFilesAndMd5(String name) {
    FileAndMd5 jar = mock(FileAndMd5.class);
    when(jar.getFile()).thenReturn(new File(name));
    when(jar.getMd5()).thenReturn(name + "-md5");

    FileAndMd5 compressed = mock(FileAndMd5.class);
    when(compressed.getFile()).thenReturn(new File(name + "-compressed"));
    when(compressed.getMd5()).thenReturn(name + "-compressed-md5");

    return new PluginFilesAndMd5(jar, compressed);
  }
}
