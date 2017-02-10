/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.io.File;
import java.io.StringReader;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.RemotePlugin;
import org.sonar.home.cache.FileCache;
import org.sonar.scanner.WsTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerPluginInstallerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FileCache fileCache = mock(FileCache.class);
  private ScannerWsClient wsClient;
  private ScannerPluginPredicate pluginPredicate = mock(ScannerPluginPredicate.class);

  @Before
  public void setUp() {
    wsClient = mock(ScannerWsClient.class);
  }

  @Test
  public void listRemotePlugins() {
    WsTestUtil.mockReader(wsClient, "/deploy/plugins/index.txt", new StringReader("checkstyle\nsqale"));
    ScannerPluginInstaller underTest = new ScannerPluginInstaller(wsClient, fileCache, pluginPredicate);

    List<RemotePlugin> remotePlugins = underTest.listRemotePlugins();
    assertThat(remotePlugins).extracting("key").containsOnly("checkstyle", "sqale");
  }

  @Test
  public void should_download_plugin() throws Exception {
    File pluginJar = temp.newFile();
    when(fileCache.get(eq("checkstyle-plugin.jar"), eq("fakemd5_1"), any(FileCache.Downloader.class))).thenReturn(pluginJar);

    ScannerPluginInstaller underTest = new ScannerPluginInstaller(wsClient, fileCache, pluginPredicate);

    RemotePlugin remote = new RemotePlugin("checkstyle").setFile("checkstyle-plugin.jar", "fakemd5_1");
    File file = underTest.download(remote);

    assertThat(file).isEqualTo(pluginJar);
  }

  @Test
  public void should_fail_to_get_plugin_index() {
    WsTestUtil.mockException(wsClient, "/deploy/plugins/index.txt", new IllegalStateException());
    thrown.expect(IllegalStateException.class);

    new ScannerPluginInstaller(wsClient, fileCache, pluginPredicate).installRemotes();
  }
}
