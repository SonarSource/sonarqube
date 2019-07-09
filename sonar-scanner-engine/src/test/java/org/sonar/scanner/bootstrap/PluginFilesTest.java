/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.scanner.bootstrap.ScannerPluginInstaller.InstalledPlugin;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;

import static org.apache.commons.io.FileUtils.moveFile;
import static org.assertj.core.api.Assertions.assertThat;

public class PluginFilesTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();

  private File userHome;
  private PluginFiles underTest;

  @Before
  public void setUp() throws Exception {
    HttpConnector connector = HttpConnector.newBuilder().url(server.url("/").toString()).build();
    GlobalAnalysisMode analysisMode = new GlobalAnalysisMode(new RawScannerProperties(Collections.emptyMap()));
    DefaultScannerWsClient wsClient = new DefaultScannerWsClient(WsClientFactories.getDefault().newClient(connector), false, analysisMode);

    userHome = temp.newFolder();
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.userHome", userHome.getAbsolutePath());

    underTest = new PluginFiles(wsClient, settings.asConfig());
  }

  @Test
  public void get_jar_from_cache_if_present() throws Exception {
    FileAndMd5 jar = createFileInCache("foo");

    File result = underTest.get(newInstalledPlugin("foo", jar.md5)).get();

    verifySameContent(result, jar);
    // no requests to server
    assertThat(server.getRequestCount()).isEqualTo(0);
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
    assertThat(requestedUrl.encodedQuery()).isEqualTo("plugin=foo&acceptCompressions=pack200");

    // get from cache on second call
    result = underTest.get(plugin).get();
    verifySameContent(result, tempJar);
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void download_compressed_and_add_uncompressed_to_cache_if_missing() throws Exception {
    FileAndMd5 jar = new FileAndMd5();
    enqueueCompressedDownload(jar, true);

    InstalledPlugin plugin = newInstalledPlugin("foo", jar.md5);
    File result = underTest.get(plugin).get();

    verifySameContentAfterCompression(jar.file, result);
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getRequestUrl().queryParameter("acceptCompressions")).isEqualTo("pack200");

    // get from cache on second call
    result = underTest.get(plugin).get();
    verifySameContentAfterCompression(jar.file, result);
    assertThat(server.getRequestCount()).isEqualTo(1);
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

    expectISE("foo", "was expected to have checksum invalid_hash but had " + tempJar.md5);

    underTest.get(plugin);
  }

  @Test
  public void fail_if_integrity_of_compressed_download_is_not_valid() throws Exception {
    FileAndMd5 jar = new FileAndMd5();
    enqueueCompressedDownload(jar, false);

    expectISE("foo", "was expected to have checksum invalid_hash but had ");
    InstalledPlugin plugin = newInstalledPlugin("foo", jar.md5);

    underTest.get(plugin).get();
  }

  @Test
  public void fail_if_md5_header_is_missing_from_response() throws IOException {
    File tempJar = temp.newFile();
    enqueueDownload(tempJar, null);
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "did not return header Sonar-MD5");

    underTest.get(plugin);
  }

  @Test
  public void fail_if_compressed_download_cannot_be_uncompressed() {
    MockResponse response = new MockResponse().setBody("not binary");
    response.setHeader("Sonar-MD5", DigestUtils.md5Hex("not binary"));
    response.setHeader("Sonar-UncompressedMD5", "abc");
    response.setHeader("Sonar-Compression", "pack200");
    server.enqueue(response);

    expectISE("foo", "Pack200 error");

    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");
    underTest.get(plugin).get();
  }

  @Test
  public void fail_if_server_returns_error() {
    server.enqueue(new MockResponse().setResponseCode(500));
    InstalledPlugin plugin = newInstalledPlugin("foo", "abc");

    expectISE("foo", "returned code 500");

    underTest.get(plugin);
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
    assertThat(server.getRequestCount()).isEqualTo(1);

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

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to download plugin [foo/bar]. Key is not valid.");

    underTest.get(plugin);
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

  /**
   * Enqueue download of file with a MD5 that may not be returned (null) or not valid
   */
  private void enqueueCompressedDownload(FileAndMd5 jar, boolean validMd5) throws IOException {
    Buffer body = new Buffer();

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(jar.file.toPath())));
      OutputStream output = new GZIPOutputStream(new BufferedOutputStream(bytes))) {
      Pack200.newPacker().pack(in, output);
    }
    body.write(bytes.toByteArray());

    MockResponse response = new MockResponse().setBody(body);
    response.setHeader("Sonar-MD5", validMd5 ? DigestUtils.md5Hex(bytes.toByteArray()) : "invalid_hash");
    response.setHeader("Sonar-UncompressedMD5", jar.md5);
    response.setHeader("Sonar-Compression", "pack200");
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

  /**
   * Packing and unpacking a JAR generates a different file.
   */
  private void verifySameContentAfterCompression(File file1, File file2) throws IOException {
    assertThat(file1).isFile().exists();
    assertThat(file2).isFile().exists();
    assertThat(packAndUnpackJar(file1)).hasSameContentAs(packAndUnpackJar(file2));
  }

  private File packAndUnpackJar(File source) throws IOException {
    File packed = temp.newFile();
    try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(source.toPath())));
      OutputStream out = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(packed.toPath())))) {
      Pack200.newPacker().pack(in, out);
    }

    File to = temp.newFile();
    try (InputStream input = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(packed.toPath())));
      JarOutputStream output = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(to.toPath())))) {
      Pack200.newUnpacker().unpack(input, output);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return to;
  }

  private void expectISE(String pluginKey, String message) {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String item) {
        return item.startsWith("Fail to download plugin [" + pluginKey + "]") && item.contains(message);
      }

      @Override
      public void describeTo(Description description) {
      }
    });
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
