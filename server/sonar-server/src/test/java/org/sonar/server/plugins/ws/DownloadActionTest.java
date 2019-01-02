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
package org.sonar.server.plugins.ws;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.InstalledPlugin;
import org.sonar.server.plugins.InstalledPlugin.FileAndMd5;
import org.sonar.server.plugins.PluginFileSystem;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadActionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PluginFileSystem pluginFileSystem = mock(PluginFileSystem.class);
  private WsAction underTest = new DownloadAction(pluginFileSystem);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action def = tester.getDef();

    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("7.2");
    assertThat(def.params())
      .extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("plugin", "acceptCompressions");
  }

  @Test
  public void return_404_if_plugin_not_found() {
    when(pluginFileSystem.getInstalledPlugin("foo")).thenReturn(Optional.empty());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Plugin foo not found");

    tester.newRequest()
      .setParam("plugin", "foo")
      .execute();
  }

  @Test
  public void return_jar_if_plugin_exists() throws Exception {
    InstalledPlugin plugin = newPlugin();
    when(pluginFileSystem.getInstalledPlugin(plugin.getPluginInfo().getKey())).thenReturn(Optional.of(plugin));

    TestResponse response = tester.newRequest()
      .setParam("plugin", plugin.getPluginInfo().getKey())
      .execute();

    assertThat(response.getHeader("Sonar-MD5")).isEqualTo(plugin.getLoadedJar().getMd5());
    assertThat(response.getHeader("Sonar-Compression")).isNull();
    assertThat(response.getMediaType()).isEqualTo("application/java-archive");
    verifySameContent(response, plugin.getLoadedJar().getFile());
  }

  @Test
  public void return_uncompressed_jar_if_client_does_not_accept_compression() throws Exception {
    InstalledPlugin plugin = newCompressedPlugin();
    when(pluginFileSystem.getInstalledPlugin(plugin.getPluginInfo().getKey())).thenReturn(Optional.of(plugin));

    TestResponse response = tester.newRequest()
      .setParam("plugin", plugin.getPluginInfo().getKey())
      .execute();

    assertThat(response.getHeader("Sonar-MD5")).isEqualTo(plugin.getLoadedJar().getMd5());
    assertThat(response.getHeader("Sonar-Compression")).isNull();
    assertThat(response.getHeader("Sonar-UncompressedMD5")).isNull();
    assertThat(response.getMediaType()).isEqualTo("application/java-archive");
    verifySameContent(response, plugin.getLoadedJar().getFile());
  }

  @Test
  public void return_uncompressed_jar_if_client_requests_unsupported_compression() throws Exception {
    InstalledPlugin plugin = newCompressedPlugin();
    when(pluginFileSystem.getInstalledPlugin(plugin.getPluginInfo().getKey())).thenReturn(Optional.of(plugin));

    TestResponse response = tester.newRequest()
      .setParam("plugin", plugin.getPluginInfo().getKey())
      .setParam("acceptCompressions", "zip")
      .execute();

    assertThat(response.getHeader("Sonar-MD5")).isEqualTo(plugin.getLoadedJar().getMd5());
    assertThat(response.getHeader("Sonar-Compression")).isNull();
    assertThat(response.getHeader("Sonar-UncompressedMD5")).isNull();
    assertThat(response.getMediaType()).isEqualTo("application/java-archive");
    verifySameContent(response, plugin.getLoadedJar().getFile());
  }

  @Test
  public void return_compressed_jar_if_client_accepts_pack200() throws Exception {
    InstalledPlugin plugin = newCompressedPlugin();
    when(pluginFileSystem.getInstalledPlugin(plugin.getPluginInfo().getKey())).thenReturn(Optional.of(plugin));

    TestResponse response = tester.newRequest()
      .setParam("plugin", plugin.getPluginInfo().getKey())
      .setParam("acceptCompressions", "pack200")
      .execute();

    assertThat(response.getHeader("Sonar-MD5")).isEqualTo(plugin.getCompressedJar().getMd5());
    assertThat(response.getHeader("Sonar-UncompressedMD5")).isEqualTo(plugin.getLoadedJar().getMd5());
    assertThat(response.getHeader("Sonar-Compression")).isEqualTo("pack200");
    assertThat(response.getMediaType()).isEqualTo("application/octet-stream");
    verifySameContent(response, plugin.getCompressedJar().getFile());
  }

  private InstalledPlugin newPlugin() throws IOException {
    FileAndMd5 jar = new FileAndMd5(temp.newFile());
    return new InstalledPlugin(new PluginInfo("foo"), jar, null);
  }

  private InstalledPlugin newCompressedPlugin() throws IOException {
    FileAndMd5 jar = new FileAndMd5(temp.newFile());
    FileAndMd5 compressedJar = new FileAndMd5(temp.newFile());
    return new InstalledPlugin(new PluginInfo("foo"), jar, compressedJar);
  }

  private static void verifySameContent(TestResponse response, File file) throws IOException {
    assertThat(IOUtils.toByteArray(response.getInputStream())).isEqualTo(FileUtils.readFileToByteArray(file));
  }
}
