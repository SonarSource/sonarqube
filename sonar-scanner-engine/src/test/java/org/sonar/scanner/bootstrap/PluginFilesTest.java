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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.scanner.bootstrap.ScannerPluginInstaller.InstalledPlugin;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;

import static org.apache.commons.io.FileUtils.moveFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.Mockito.mock;
import static org.sonar.scanner.bootstrap.PluginFiles.PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY;

public class PluginFilesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();

  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  private File userHome;
  private PluginFiles underTest;

  @Before
  public void setUp() throws Exception {
    HttpConnector connector = HttpConnector.newBuilder().url(server.url("/").toString()).build();
    GlobalAnalysisMode analysisMode = new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap()));
    DefaultScannerWsClient wsClient = new DefaultScannerWsClient(WsClientFactories.getDefault().newClient(connector), false,
      analysisMode, analysisWarnings);

    userHome = temp.newFolder();
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.userHome", userHome.getAbsolutePath());
    settings.setProperty(PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY, 1);

    underTest = new PluginFiles(wsClient, settings.asConfig());
  }

  @Test
  public void get_jar_from_cache_if_present() throws Exception {
    FileAndMd5 jar = createFileInCache("foo");

    File result = underTest.get(newInstalledPlugin("foo", jar.md5)).get();

    verifySameContent(result, jar);
    // no requests to server
    assertThat(server.getRequestCount()).isZero();
  }

  @Test
  public void download_and_add_jar_to_cache_if_missing() throws Exception {
    FileAndMd5 tempJar = new FileAndMd5();
    enqueueDownload(tempJar);

    InstalledPlugin plugin = newInstalledPlugin("foo", tempJar.md5);
    File result = underTest.get(plugin).get();

    verifySameContent(result, tempJar);
    HttpUrl requestedUrl = server.takeRequest().getRequestUrl();
    assertThat(requestedUrl.encodedPath()).isEqualTo("/api/plugins/download");
    assertThat(requestedUrl.encodedQuery()).isEqualTo("plugin=foo");

    // get from cache on second call
    result = underTest.get(plugin).get();
    verifySameContent(result, tempJar);
    assertThat(server.getRequestCount()).isOne();
  }

  @Test
  public void return_empty_if_plugin_not_found_on_server() {
    server.enqueue(new MockResponse().setResponseCode(404));

    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");
    Optional<File> result = underTest.get(plugin);

    assertThat(result).isEmpty();
  }

  @Test
  public void fail_if_integrity_of_download_is_not_valid() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    enqueueDownload(tempJar.file, "invalid_hash");
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "was expected to have checksum invalid_hash but had " + tempJar.md5,
      () -> underTest.get(plugin));
  }

  @Test
  public void fail_if_md5_header_is_missing_from_response() throws IOException {
    File tempJar = temp.newFile();
    enqueueDownload(tempJar, null);
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "did not return header Sonar-MD5", () -> underTest.get(plugin));
  }

  @Test
  public void fail_if_server_returns_error() {
    server.enqueue(new MockResponse().setResponseCode(500));
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "returned code 500", () -> underTest.get(plugin));
  }

  @Test
  public void getPlugin_whenTimeOutReached_thenDownloadFails() {
    MockResponse response = new MockResponse().setBody("test").setBodyDelay(2, TimeUnit.SECONDS);
    response.setHeader("Sonar-MD5", "md5");
    server.enqueue(response);
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    assertThatThrownBy(() -> underTest.get(plugin))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Fail to download plugin [" + plugin.key + "]")
      .cause().isInstanceOf(SocketTimeoutException.class);
  }

  @Test
  public void download_a_new_version_of_plugin_during_blue_green_switch() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    enqueueDownload(tempJar);

    // expecting to download plugin foo with checksum "abc"
    InstalledPlugin pluginV1 = newInstalledPlugin("foo", "abc");

    File result = underTest.get(pluginV1).get();
    verifySameContent(result, tempJar);

    // new version of downloaded jar is put in cache with the new md5
    InstalledPlugin pluginV2 = newInstalledPlugin("foo", tempJar.md5);
    result = underTest.get(pluginV2).get();
    verifySameContent(result, tempJar);
    assertThat(server.getRequestCount()).isOne();

    // v1 still requests server and downloads v2
    enqueueDownload(tempJar);
    result = underTest.get(pluginV1).get();
    verifySameContent(result, tempJar);
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test
  public void fail_if_cached_file_is_outside_cache_dir() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    enqueueDownload(tempJar);

    InstalledPlugin plugin = newInstalledPlugin("foo/bar", "abc");

    assertThatThrownBy(() -> underTest.get(plugin))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to download plugin [foo/bar]. Key is not valid.");
  }

  private FileAndMd5 createFileInCache(String pluginKey) throws IOException {
    FileAndMd5 tempFile = new FileAndMd5();
    return moveToCache(pluginKey, tempFile);
  }

  private FileAndMd5 moveToCache(String pluginKey, FileAndMd5 jar) throws IOException {
    File jarInCache = new File(userHome, "cache/" + jar.md5 + "/sonar-" + pluginKey + "-plugin.jar");
    moveFile(jar.file, jarInCache);
    return new FileAndMd5(jarInCache, jar.md5);
  }

  /**
   * Enqueue download of file with valid MD5
   */
  private void enqueueDownload(FileAndMd5 file) throws IOException {
    enqueueDownload(file.file, file.md5);
  }

  /**
   * Enqueue download of file with a MD5 that may not be returned (null) or not valid
   */
  private void enqueueDownload(File file, @Nullable String md5) throws IOException {
    Buffer body = new Buffer();
    body.write(FileUtils.readFileToByteArray(file));
    MockResponse response = new MockResponse().setBody(body);
    if (md5 != null) {
      response.setHeader("Sonar-MD5", md5);
    }
    server.enqueue(response);
  }

  private static InstalledPlugin newInstalledPlugin(String pluginKey, String fileChecksum) {
    InstalledPlugin plugin = new InstalledPlugin();
    plugin.key = pluginKey;
    plugin.hash = fileChecksum;
    return plugin;
  }

  private static void verifySameContent(File file1, FileAndMd5 file2) {
    assertThat(file1).isFile().exists();
    assertThat(file2.file).isFile().exists();
    assertThat(file1).hasSameContentAs(file2.file);
  }

  private void expectISE(String pluginKey, String message, ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Fail to download plugin [" + pluginKey + "]")
      .hasMessageContaining(message);
  }

  private class FileAndMd5 {
    private final File file;
    private final String md5;

    FileAndMd5(File file, String md5) {
      this.file = file;
      this.md5 = md5;
    }

    FileAndMd5() throws IOException {
      this.file = temp.newFile();
      FileUtils.write(this.file, RandomStringUtils.random(3));
      try (InputStream fis = FileUtils.openInputStream(this.file)) {
        this.md5 = DigestUtils.md5Hex(fis);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to compute md5 of " + this.file, e);
      }
    }

  }
}
