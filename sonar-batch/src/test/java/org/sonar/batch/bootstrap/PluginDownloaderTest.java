/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluginDownloaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_request_list_of_plugins() {
    TempDirectories tempDirs = mock(TempDirectories.class);
    ServerClient server = mock(ServerClient.class);
    when(server.request("/deploy/plugins/index.txt")).thenReturn("checkstyle,true\nsqale,false");
    PluginDownloader downloader = new PluginDownloader(tempDirs, server);

    List<RemotePlugin> plugins = downloader.downloadPluginIndex();
    assertThat(plugins).hasSize(2);
    assertThat(plugins.get(0).getKey()).isEqualTo("checkstyle");
    assertThat(plugins.get(0).isCore()).isTrue();
    assertThat(plugins.get(1).getKey()).isEqualTo("sqale");
    assertThat(plugins.get(1).isCore()).isFalse();
  }

  @Test
  public void should_download_plugin() throws Exception {
    TempDirectories tempDirs = mock(TempDirectories.class);
    File toDir = temp.newFolder();
    when(tempDirs.getDir("plugins/checkstyle")).thenReturn(toDir);
    ServerClient server = mock(ServerClient.class);
    PluginDownloader downloader = new PluginDownloader(tempDirs, server);

    RemotePlugin plugin = new RemotePlugin("checkstyle", true)
      .addFilename("checkstyle-plugin.jar")
      .addFilename("checkstyle-extensions.jar");
    List<File> files = downloader.downloadPlugin(plugin);

    File pluginFile = new File(toDir, "checkstyle-plugin.jar");
    File extFile = new File(toDir, "checkstyle-extensions.jar");
    assertThat(files).hasSize(2);
    assertThat(files).containsOnly(pluginFile, extFile);
    verify(server).download("/deploy/plugins/checkstyle/checkstyle-plugin.jar", pluginFile);
    verify(server).download("/deploy/plugins/checkstyle/checkstyle-extensions.jar", extFile);
  }
}
