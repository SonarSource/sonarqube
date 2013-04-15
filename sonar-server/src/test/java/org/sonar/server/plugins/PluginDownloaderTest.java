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
package org.sonar.server.plugins;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.net.URI;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class PluginDownloaderTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  private File downloadDir;

  private UpdateCenterMatrixFactory updateCenterMatrixFactory;
  private UpdateCenter updateCenter;
  private HttpDownloader httpDownloader;

  private PluginDownloader pluginDownloader;

  @Before
  public void before() throws Exception {
    updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class);
    updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(anyBoolean())).thenReturn(updateCenter);

    httpDownloader = mock(HttpDownloader.class);

    DefaultServerFileSystem defaultServerFileSystem = mock(DefaultServerFileSystem.class);
    downloadDir = testFolder.newFolder("downloads");
    when(defaultServerFileSystem.getDownloadedPluginsDir()).thenReturn(downloadDir);

    pluginDownloader = new PluginDownloader(updateCenterMatrixFactory, httpDownloader, defaultServerFileSystem);
  }

  @Test
  public void should_download_from_url() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", Version.create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.download("foo", Version.create("1.0"));
    verify(httpDownloader).download(any(URI.class), argThat(new HasFileName("test-1.0.jar")));
  }

  @Test
  public void should_throw_exception_if_download_dir_is_invalid() throws Exception {
    DefaultServerFileSystem defaultServerFileSystem = mock(DefaultServerFileSystem.class);
    // download dir is a file instead of being a directory
    File downloadDir = testFolder.newFile();
    when(defaultServerFileSystem.getDownloadedPluginsDir()).thenReturn(downloadDir);

    try {
      new PluginDownloader(updateCenterMatrixFactory, httpDownloader, defaultServerFileSystem);
      fail();
    } catch (SonarException e) {
      // ok
    }
  }

  @Test
  public void should_download_from_file() throws Exception {
    Plugin test = new Plugin("test");
    File file = testFolder.newFile("test-1.0.jar");
    file.createNewFile();
    Release test10 = new Release(test, "1.0").setDownloadUrl("file://" + FilenameUtils.separatorsToUnix(file.getCanonicalPath()));
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", Version.create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.download("foo", Version.create("1.0"));
    verify(httpDownloader, never()).download(any(URI.class), any(File.class));
    assertThat(pluginDownloader.hasDownloads()).isTrue();
  }

  @Test
  public void should_throw_exception_if_could_not_download() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("file://not_found");
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", Version.create("1.0"))).thenReturn(newArrayList(test10));

    try {
      pluginDownloader.download("foo", Version.create("1.0"));
      fail();
    } catch (SonarException e) {
      // ok
    }
  }

  @Test
  public void should_throw_exception_if_download_fail() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);
    when(updateCenter.findInstallablePlugins("foo", Version.create("1.0"))).thenReturn(newArrayList(test10));

    doThrow(new RuntimeException()).when(httpDownloader).download(any(URI.class), any(File.class));

    try {
      pluginDownloader.download("foo", Version.create("1.0"));
      fail();
    } catch (SonarException e) {
      // ok
    }
  }

  @Test
  public void should_read_download_folder() throws Exception {
    assertThat(pluginDownloader.getDownloads()).hasSize(0);

    File file1 = new File(downloadDir, "file1.jar");
    file1.createNewFile();
    File file2 = new File(downloadDir, "file2.jar");
    file2.createNewFile();

    assertThat(pluginDownloader.getDownloads()).hasSize(2);
  }

  @Test
  public void should_cancel_downloads() throws Exception {
    File file1 = new File(downloadDir, "file1.jar");
    file1.createNewFile();
    File file2 = new File(downloadDir, "file2.jar");
    file2.createNewFile();

    assertThat(pluginDownloader.hasDownloads()).isTrue();
    pluginDownloader.cancelDownloads();
    assertThat(pluginDownloader.hasDownloads()).isFalse();
  }

  class HasFileName extends ArgumentMatcher<File> {

    private final String name;

    HasFileName(String name) {
      this.name = name;
    }

    public boolean matches(Object obj) {
      File file = (File) obj;
      return file.getName().equals(name);
    }
  }

}
