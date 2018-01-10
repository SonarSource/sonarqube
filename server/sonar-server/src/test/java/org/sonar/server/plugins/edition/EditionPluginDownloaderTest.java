/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.plugins.edition;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.log.LogTester;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EditionPluginDownloaderTest {
  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem fs = mock(ServerFileSystem.class);
  private HttpDownloader httpDownloader = mock(HttpDownloader.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);

  private File downloadDir;
  private File tmpDir;
  private EditionPluginDownloader downloader;

  @Before
  public void setUp() throws IOException {
    downloadDir = temp.newFolder("download");
    tmpDir = new File(downloadDir.getParentFile(), downloadDir.getName() + "_tmp");
    when(fs.getEditionDownloadedPluginsDir()).thenReturn(downloadDir);
    downloader = new EditionPluginDownloader(httpDownloader, fs);
  }

  @Test
  public void download_plugin_to_tmp() throws URISyntaxException {
    List<Release> releases = ImmutableList.of(createRelease("plugin1", "1.0", "http://host/plugin1.jar"),
      createRelease("plugin2", "1.0", "http://host/plugin2.jar"));

    when(updateCenter.findInstallablePlugins("plugins", Version.create(""))).thenReturn(releases);
    downloader.downloadEditionPlugins(Collections.singleton("plugins"), updateCenter);

    verify(httpDownloader).download(new URI("http://host/plugin1.jar"), new File(tmpDir, "plugin1.jar"));
    verify(httpDownloader).download(new URI("http://host/plugin2.jar"), new File(tmpDir, "plugin2.jar"));

    assertThat(logTester.logs()).containsOnly("Downloading plugin: plugin1", "Downloading plugin: plugin2");
    assertThat(downloadDir).isDirectory();
    assertThat(tmpDir).doesNotExist();
  }
  
  @Test
  public void download_plugin_to_tmp_with_file_uri() throws IOException {
    File plugin1 = temp.newFile("plugin1.jar");
    File plugin2 = temp.newFile("plugin2.jar");

    List<Release> releases = ImmutableList.of(createRelease("plugin1", "1.0", plugin1.toURI().toString()),
      createRelease("plugin2", "1.0", plugin2.toURI().toString()));

    when(updateCenter.findInstallablePlugins("plugins", Version.create(""))).thenReturn(releases);
    downloader.downloadEditionPlugins(Collections.singleton("plugins"), updateCenter);

    assertThat(logTester.logs()).containsOnly("Downloading plugin: plugin1", "Downloading plugin: plugin2");
    assertThat(downloadDir).isDirectory();
    assertThat(tmpDir).doesNotExist();
  }

  @Test
  public void dont_write_download_dir_if_download_fails() throws URISyntaxException {
    List<Release> releases = ImmutableList.of(createRelease("plugin1", "1.0", "http://host/plugin1.jar"),
      createRelease("plugin2", "1.0", "http://host/plugin2.jar"));

    doThrow(new IllegalStateException("error")).when(httpDownloader).download(new URI("http://host/plugin1.jar"), new File(tmpDir, "plugin1.jar"));

    when(updateCenter.findInstallablePlugins("plugins", Version.create(""))).thenReturn(releases);

    try {
      downloader.downloadEditionPlugins(Collections.singleton("plugins"), updateCenter);
      fail("expecting exception");
    } catch (IllegalStateException e) {

    }

    verify(httpDownloader).download(new URI("http://host/plugin1.jar"), new File(tmpDir, "plugin1.jar"));

    assertThat(downloadDir.list()).isEmpty();
    assertThat(tmpDir).doesNotExist();
  }

  private static Release createRelease(String key, String version, String url) {
    Release release = mock(Release.class);
    when(release.getKey()).thenReturn(key);
    when(release.getVersion()).thenReturn(Version.create(version));
    when(release.getDownloadUrl()).thenReturn(url);
    when(release.getArtifact()).thenReturn(Plugin.factory(key));
    return release;
  }
}
