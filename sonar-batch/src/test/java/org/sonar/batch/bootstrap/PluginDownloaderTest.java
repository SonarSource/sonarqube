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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.cache.SonarCache;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluginDownloaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_request_list_of_plugins() {
    ServerClient server = mock(ServerClient.class);
    when(server.request("/deploy/plugins/index.txt")).thenReturn("checkstyle,true\nsqale,false");
    PluginDownloader downloader = new PluginDownloader(new BatchSonarCache(new Settings()), server);

    List<RemotePlugin> plugins = downloader.downloadPluginIndex();
    assertThat(plugins).hasSize(2);
    assertThat(plugins.get(0).getKey()).isEqualTo("checkstyle");
    assertThat(plugins.get(0).isCore()).isTrue();
    assertThat(plugins.get(1).getKey()).isEqualTo("sqale");
    assertThat(plugins.get(1).isCore()).isFalse();
  }

  @Test
  public void should_download_plugin_if_not_cached() throws Exception {
    SonarCache cache = mock(SonarCache.class);
    BatchSonarCache batchCache = mock(BatchSonarCache.class);
    when(batchCache.getCache()).thenReturn(cache);

    File fileInCache = temp.newFile();
    when(cache.cacheFile(Mockito.any(File.class), Mockito.anyString())).thenReturn("fakemd51").thenReturn("fakemd52");
    when(cache.getFileFromCache(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(null)
        .thenReturn(fileInCache)
        .thenReturn(null)
        .thenReturn(fileInCache);
    ServerClient server = mock(ServerClient.class);
    PluginDownloader downloader = new PluginDownloader(batchCache, server);

    RemotePlugin plugin = new RemotePlugin("checkstyle", true)
        .addFile("checkstyle-plugin.jar", "fakemd51")
        .addFile("checkstyle-extensions.jar", "fakemd52");
    List<File> files = downloader.downloadPlugin(plugin);

    assertThat(files).hasSize(2);
    verify(server).download(Mockito.eq("/deploy/plugins/checkstyle/checkstyle-plugin.jar"), Mockito.any(File.class));
    verify(server).download(Mockito.eq("/deploy/plugins/checkstyle/checkstyle-extensions.jar"), Mockito.any(File.class));
  }

  @Test
  public void should_not_download_plugin_if_cached() throws Exception {
    SonarCache cache = mock(SonarCache.class);
    BatchSonarCache batchCache = mock(BatchSonarCache.class);
    when(batchCache.getCache()).thenReturn(cache);

    File fileInCache = temp.newFile();
    when(cache.getFileFromCache(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(fileInCache)
        .thenReturn(fileInCache);
    ServerClient server = mock(ServerClient.class);
    PluginDownloader downloader = new PluginDownloader(batchCache, server);

    RemotePlugin plugin = new RemotePlugin("checkstyle", true)
        .addFile("checkstyle-plugin.jar", "fakemd51")
        .addFile("checkstyle-extensions.jar", "fakemd52");
    List<File> files = downloader.downloadPlugin(plugin);

    assertThat(files).hasSize(2);
    verify(server, never()).download(Mockito.anyString(), Mockito.any(File.class));
    verify(cache, never()).cacheFile(Mockito.any(File.class), Mockito.anyString());
  }

  @Test
  public void should_fail_to_get_plugin_index() throws Exception {
    thrown.expect(SonarException.class);

    ServerClient server = mock(ServerClient.class);
    doThrow(new SonarException()).when(server).request("/deploy/plugins/index.txt");

    new PluginDownloader(new BatchSonarCache(new Settings()), server).downloadPluginIndex();
  }
}
