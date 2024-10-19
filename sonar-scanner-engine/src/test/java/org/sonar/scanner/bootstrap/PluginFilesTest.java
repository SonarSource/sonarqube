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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.scanner.bootstrap.ScannerPluginInstaller.InstalledPlugin;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.bootstrap.PluginFiles.PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY;

class PluginFilesTest {

  @RegisterExtension
  static WireMockExtension sonarqube = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @TempDir
  private Path tempDir;

  private final SonarUserHome sonarUserHome = mock(SonarUserHome.class);

  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  private PluginFiles underTest;

  @BeforeEach
  void setUp(@TempDir Path sonarUserHomeDir) {
    when(sonarUserHome.getPath()).thenReturn(sonarUserHomeDir);

    HttpConnector connector = HttpConnector.newBuilder().acceptGzip(true).url(sonarqube.url("/")).build();
    GlobalAnalysisMode analysisMode = new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap()));
    DefaultScannerWsClient wsClient = new DefaultScannerWsClient(WsClientFactories.getDefault().newClient(connector), false,
      analysisMode, analysisWarnings);

    MapSettings settings = new MapSettings();
    settings.setProperty(PLUGINS_DOWNLOAD_TIMEOUT_PROPERTY, 1);

    underTest = new PluginFiles(wsClient, settings.asConfig(), sonarUserHome);
  }

  @Test
  void get_jar_from_cache_if_present() throws Exception {
    FileAndMd5 jar = createFileInCache("foo");

    File result = underTest.get(newInstalledPlugin("foo", jar.md5)).get();

    verifySameContent(result.toPath(), jar);
    // no requests to server
    sonarqube.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void download_and_add_jar_to_cache_if_missing() throws Exception {
    FileAndMd5 tempJar = new FileAndMd5();
    stubDownload(tempJar);

    InstalledPlugin plugin = newInstalledPlugin("foo", tempJar.md5);
    File result = underTest.get(plugin).get();

    verifySameContent(result.toPath(), tempJar);

    sonarqube.verify(exactly(1), getRequestedFor(urlEqualTo("/api/plugins/download?plugin=foo")));

    // get from cache on second call
    result = underTest.get(plugin).get();
    verifySameContent(result.toPath(), tempJar);

    sonarqube.verify(exactly(1), getRequestedFor(urlEqualTo("/api/plugins/download?plugin=foo")));
  }

  @Test
  void return_empty_if_plugin_not_found_on_server() {
    sonarqube.stubFor(get(anyUrl())
      .willReturn(notFound()));

    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");
    Optional<File> result = underTest.get(plugin);

    assertThat(result).isEmpty();
  }

  @Test
  void fail_if_integrity_of_download_is_not_valid() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    stubDownload(tempJar.file, "invalid_hash");
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "was expected to have checksum invalid_hash but had " + tempJar.md5,
      () -> underTest.get(plugin));
  }

  @Test
  void fail_if_md5_header_is_missing_from_response(@TempDir Path tempDir) throws IOException {
    var tempJar = Files.createTempFile(tempDir, "plugin", ".jar");
    stubDownload(tempJar, null);
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "did not return header Sonar-MD5", () -> underTest.get(plugin));
  }

  @Test
  void fail_if_server_returns_error() {
    sonarqube.stubFor(get(anyUrl())
      .willReturn(serverError()));

    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "returned code 500", () -> underTest.get(plugin));
  }

  @Test
  void getPlugin_whenTimeOutReached_thenDownloadFails() {
    sonarqube.stubFor(get(anyUrl())
      .willReturn(ok()
        .withFixedDelay(5000)));

    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    assertThatThrownBy(() -> underTest.get(plugin))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Fail to request url")
      .cause().isInstanceOf(SocketTimeoutException.class);
  }

  @Test
  void download_a_new_version_of_plugin_during_blue_green_switch() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    stubDownload(tempJar);

    // expecting to download plugin foo with checksum "abc"
    InstalledPlugin pluginV1 = newInstalledPlugin("foo", "abc");

    File result = underTest.get(pluginV1).get();
    verifySameContent(result.toPath(), tempJar);

    // new version of downloaded jar is put in cache with the new md5
    InstalledPlugin pluginV2 = newInstalledPlugin("foo", tempJar.md5);
    result = underTest.get(pluginV2).get();
    verifySameContent(result.toPath(), tempJar);

    sonarqube.verify(exactly(1), getRequestedFor(urlEqualTo("/api/plugins/download?plugin=foo")));

    // v1 still requests server and downloads v2
    stubDownload(tempJar);
    result = underTest.get(pluginV1).get();
    verifySameContent(result.toPath(), tempJar);

    sonarqube.verify(exactly(2), getRequestedFor(urlEqualTo("/api/plugins/download?plugin=foo")));
  }

  @Test
  void fail_if_cached_file_is_outside_cache_dir() throws IOException {
    FileAndMd5 tempJar = new FileAndMd5();
    stubDownload(tempJar);

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
    Path jarInCache = sonarUserHome.getPath().resolve("cache/" + jar.md5 + "/sonar-" + pluginKey + "-plugin.jar");
    moveFile(jar.file.toFile(), jarInCache.toFile());
    return new FileAndMd5(jarInCache, jar.md5);
  }

  /**
   * Enqueue download of file with valid MD5
   */
  private void stubDownload(FileAndMd5 file) throws IOException {
    stubDownload(file.file, file.md5);
  }

  /**
   * Enqueue download of file with a MD5 that may not be returned (null) or not valid
   */
  private void stubDownload(Path file, @Nullable String md5) throws IOException {
    var responseDefBuilder = ok();
    if (md5 != null) {
      responseDefBuilder.withHeader("Sonar-MD5", md5);
    }
    responseDefBuilder.withBody(Files.readAllBytes(file));
    sonarqube.stubFor(get(urlMatching("/api/plugins/download\\?plugin=.*"))
      .willReturn(responseDefBuilder));
  }

  private static InstalledPlugin newInstalledPlugin(String pluginKey, String fileChecksum) {
    InstalledPlugin plugin = new InstalledPlugin();
    plugin.key = pluginKey;
    plugin.hash = fileChecksum;
    return plugin;
  }

  private static void verifySameContent(Path file1, FileAndMd5 file2) {
    assertThat(file1).isRegularFile();
    assertThat(file2.file).isRegularFile();
    assertThat(file1).hasSameTextualContentAs(file2.file);
  }

  private void expectISE(String pluginKey, String message, ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Fail to download plugin [" + pluginKey + "]")
      .hasMessageContaining(message);
  }

  private class FileAndMd5 {
    private final Path file;
    private final String md5;

    FileAndMd5(Path file, String md5) {
      this.file = file;
      this.md5 = md5;
    }

    FileAndMd5() throws IOException {
      this.file = Files.createTempFile(tempDir, "jar", null);
      Files.write(this.file, RandomStringUtils.random(3).getBytes());
      try (InputStream fis = Files.newInputStream(this.file)) {
        this.md5 = DigestUtils.md5Hex(fis);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to compute md5 of " + this.file, e);
      }
    }

  }
}
