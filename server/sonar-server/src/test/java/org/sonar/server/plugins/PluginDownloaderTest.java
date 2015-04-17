/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.updatecenter.common.Version.create;

public class PluginDownloaderTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  File downloadDir;
  UpdateCenterMatrixFactory updateCenterMatrixFactory;
  UpdateCenter updateCenter;
  HttpDownloader httpDownloader;
  PluginDownloader pluginDownloader;
  ServerPluginJarInstaller installer;

  @Before
  public void before() throws Exception {
    updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class);
    updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(anyBoolean())).thenReturn(updateCenter);

    httpDownloader = mock(HttpDownloader.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock inv) throws Throwable {
        File toFile = (File) inv.getArguments()[1];
        touch(toFile);
        return null;
      }
    }).when(httpDownloader).download(any(URI.class), any(File.class));

    DefaultServerFileSystem defaultServerFileSystem = mock(DefaultServerFileSystem.class);
    downloadDir = testFolder.newFolder("downloads");
    when(defaultServerFileSystem.getDownloadedPluginsDir()).thenReturn(downloadDir);

    installer = new ServerPluginJarInstaller();
    pluginDownloader = new PluginDownloader(updateCenterMatrixFactory, httpDownloader, defaultServerFileSystem, installer);
  }

  @After
  public void stop() {
    pluginDownloader.stop();
  }

  @Test
  public void clean_temporary_files_at_startup() throws Exception {
    touch(new File(downloadDir, "sonar-php.jar"));
    touch(new File(downloadDir, "sonar-js.jar.tmp"));
    assertThat(downloadDir.listFiles()).hasSize(2);
    pluginDownloader.start();

    File[] files = downloadDir.listFiles();
    assertThat(files).hasSize(1);
    assertThat(files[0].getName()).isEqualTo("sonar-php.jar");
  }

  @Test
  public void download_from_url() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.start();
    pluginDownloader.download("foo", create("1.0"));

    // SONAR-4523: do not corrupt JAR files when restarting the server while a plugin is being downloaded.
    // The JAR file is downloaded in a temp file
    verify(httpDownloader).download(any(URI.class), argThat(new HasFileName("test-1.0.jar.tmp")));
    assertThat(new File(downloadDir, "test-1.0.jar")).exists();
    assertThat(new File(downloadDir, "test-1.0.jar.tmp")).doesNotExist();
  }

  /**
   * SONAR-4685
   */
  @Test
  public void download_from_redirect_url() throws Exception {
    Plugin test = new Plugin("plugin-test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("http://server/redirect?r=release&g=test&a=test&v=1.0&e=jar");
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.start();
    pluginDownloader.download("foo", create("1.0"));

    // SONAR-4523: do not corrupt JAR files when restarting the server while a plugin is being downloaded.
    // The JAR file is downloaded in a temp file
    verify(httpDownloader).download(any(URI.class), argThat(new HasFileName("plugin-test-1.0.jar.tmp")));
    assertThat(new File(downloadDir, "plugin-test-1.0.jar")).exists();
    assertThat(new File(downloadDir, "plugin-test-1.0.jar.tmp")).doesNotExist();
  }

  @Test
  public void throw_exception_if_download_dir_is_invalid() throws Exception {
    DefaultServerFileSystem defaultServerFileSystem = mock(DefaultServerFileSystem.class);
    // download dir is a file instead of being a directory
    File downloadDir = testFolder.newFile();
    when(defaultServerFileSystem.getDownloadedPluginsDir()).thenReturn(downloadDir);

    pluginDownloader = new PluginDownloader(updateCenterMatrixFactory, httpDownloader, defaultServerFileSystem, installer);
    try {
      pluginDownloader.start();
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void download_from_file() throws Exception {
    Plugin test = new Plugin("test");
    File file = testFolder.newFile("test-1.0.jar");
    file.createNewFile();
    Release test10 = new Release(test, "1.0").setDownloadUrl("file://" + separatorsToUnix(file.getCanonicalPath()));
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.start();
    pluginDownloader.download("foo", create("1.0"));
    verify(httpDownloader, never()).download(any(URI.class), any(File.class));
    assertThat(pluginDownloader.hasDownloads()).isTrue();
  }

  @Test
  public void throw_exception_if_could_not_download() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("file://not_found");
    test.addRelease(test10);

    when(updateCenter.findInstallablePlugins("foo", create("1.0"))).thenReturn(newArrayList(test10));

    pluginDownloader.start();
    try {
      pluginDownloader.download("foo", create("1.0"));
      fail();
    } catch (SonarException e) {
      // ok
    }
  }

  @Test
  public void throw_exception_if_download_fail() throws Exception {
    Plugin test = new Plugin("test");
    Release test10 = new Release(test, "1.0").setDownloadUrl("http://server/test-1.0.jar");
    test.addRelease(test10);
    when(updateCenter.findInstallablePlugins("foo", create("1.0"))).thenReturn(newArrayList(test10));

    doThrow(new RuntimeException()).when(httpDownloader).download(any(URI.class), any(File.class));

    pluginDownloader.start();
    try {
      pluginDownloader.download("foo", create("1.0"));
      fail();
    } catch (SonarException e) {
      // ok
    }
  }

  @Test
  public void read_download_folder() throws Exception {
    pluginDownloader.start();
    assertThat(pluginDownloader.getDownloadedPluginFilenames()).hasSize(0);

    copyFileToDirectory(new File(resource("foo-plugin-1.0.jar")), downloadDir);

    assertThat(pluginDownloader.getDownloadedPlugins()).hasSize(1);
    DefaultPluginMetadata metadata = pluginDownloader.getDownloadedPlugins().iterator().next();
    assertThat(metadata.getKey()).isEqualTo("foo");
    assertThat(metadata.getName()).isEqualTo("Foo");
    assertThat(metadata.getVersion()).isEqualTo("1.0");
    assertThat(metadata.getOrganization()).isEqualTo("SonarSource");
    assertThat(metadata.getOrganizationUrl()).isEqualTo("http://www.sonarsource.org");
    assertThat(metadata.getLicense()).isEqualTo("LGPL 3");
    assertThat(metadata.getMainClass()).isEqualTo("foo.Main");
  }

  private URI resource(String fileName) throws URISyntaxException {
    URL resource = getClass().getResource(getClass().getSimpleName() + "/" + fileName);
    return resource.toURI();
  }

  @Test
  public void getDownloadedPluginFilenames_reads_plugin_metadata_of_files_in_download_folder() throws Exception {
    pluginDownloader.start();
    assertThat(pluginDownloader.getDownloadedPlugins()).hasSize(0);

    File file1 = new File(downloadDir, "file1.jar");
    file1.createNewFile();
    File file2 = new File(downloadDir, "file2.jar");
    file2.createNewFile();

    assertThat(pluginDownloader.getDownloadedPluginFilenames()).hasSize(2);
  }

  @Test
  public void cancel_downloads() throws Exception {
    File file1 = new File(downloadDir, "file1.jar");
    file1.createNewFile();
    File file2 = new File(downloadDir, "file2.jar");
    file2.createNewFile();

    pluginDownloader.start();
    assertThat(pluginDownloader.hasDownloads()).isTrue();
    pluginDownloader.cancelDownloads();
    assertThat(pluginDownloader.hasDownloads()).isFalse();
  }

  // SONAR-5011
  @Test
  public void download_common_transitive_dependency() throws Exception {
    Plugin test1 = new Plugin("test1");
    Release test1R = new Release(test1, "1.0").setDownloadUrl("http://server/test1-1.0.jar");
    test1.addRelease(test1R);

    Plugin test2 = new Plugin("test2");
    Release test2R = new Release(test2, "1.0").setDownloadUrl("http://server/test2-1.0.jar");
    test2.addRelease(test2R);

    Plugin testDep = new Plugin("testdep");
    Release testDepR = new Release(testDep, "1.0").setDownloadUrl("http://server/testdep-1.0.jar");
    testDep.addRelease(testDepR);

    when(updateCenter.findInstallablePlugins("test1", create("1.0"))).thenReturn(newArrayList(test1R, testDepR));
    when(updateCenter.findInstallablePlugins("test2", create("1.0"))).thenReturn(newArrayList(test2R, testDepR));

    pluginDownloader.start();
    pluginDownloader.download("test1", create("1.0"));
    pluginDownloader.download("test2", create("1.0"));

    assertThat(new File(downloadDir, "test1-1.0.jar")).exists();
    assertThat(new File(downloadDir, "test2-1.0.jar")).exists();
    assertThat(new File(downloadDir, "testdep-1.0.jar")).exists();
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
