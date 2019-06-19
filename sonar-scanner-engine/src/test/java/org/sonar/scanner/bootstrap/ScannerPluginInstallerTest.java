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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.WsTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ScannerPluginInstallerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PluginFiles pluginFiles = mock(PluginFiles.class);
  private DefaultScannerWsClient wsClient = mock(DefaultScannerWsClient.class);
  private ScannerPluginInstaller underTest = new ScannerPluginInstaller(pluginFiles, wsClient);

  @Test
  public void download_installed_plugins() throws IOException {
    WsTestUtil.mockReader(wsClient, "api/plugins/installed", new InputStreamReader(getClass().getResourceAsStream("ScannerPluginInstallerTest/installed-plugins-ws.json")));
    enqueueDownload("scmgit", "abc");
    enqueueDownload("java", "def");

    Map<String, ScannerPlugin> result = underTest.installRemotes();

    assertThat(result.keySet()).containsExactlyInAnyOrder("scmgit", "java");
    ScannerPlugin gitPlugin = result.get("scmgit");
    assertThat(gitPlugin.getKey()).isEqualTo("scmgit");
    assertThat(gitPlugin.getInfo().getNonNullJarFile()).exists().isFile();
    assertThat(gitPlugin.getUpdatedAt()).isEqualTo(100L);

    ScannerPlugin javaPlugin = result.get("java");
    assertThat(javaPlugin.getKey()).isEqualTo("java");
    assertThat(javaPlugin.getInfo().getNonNullJarFile()).exists().isFile();
    assertThat(javaPlugin.getUpdatedAt()).isEqualTo(200L);
  }

  @Test
  public void fail_if_json_of_installed_plugins_is_not_valid() {
    WsTestUtil.mockReader(wsClient, "api/plugins/installed", new StringReader("not json"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to parse response of api/plugins/installed");

    underTest.installRemotes();
  }

  @Test
  public void reload_list_if_plugin_uninstalled_during_blue_green_switch() throws IOException {
    WsTestUtil.mockReader(wsClient, "api/plugins/installed",
      new InputStreamReader(getClass().getResourceAsStream("ScannerPluginInstallerTest/blue-installed.json")),
      new InputStreamReader(getClass().getResourceAsStream("ScannerPluginInstallerTest/green-installed.json")));
    enqueueNotFoundDownload("scmgit", "abc");
    enqueueDownload("java", "def");
    enqueueDownload("cobol", "ghi");

    Map<String, ScannerPlugin> result = underTest.installRemotes();

    assertThat(result.keySet()).containsExactlyInAnyOrder("java", "cobol");
  }

  @Test
  public void fail_if_plugin_not_found_two_times() throws IOException {
    WsTestUtil.mockReader(wsClient, "api/plugins/installed",
      new InputStreamReader(getClass().getResourceAsStream("ScannerPluginInstallerTest/blue-installed.json")),
      new InputStreamReader(getClass().getResourceAsStream("ScannerPluginInstallerTest/green-installed.json")));
    enqueueDownload("scmgit", "abc");
    enqueueDownload("cobol", "ghi");
    enqueueNotFoundDownload("java", "def");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to download plugin [java]. Not found.");

    underTest.installRemotes();
  }

  @Test
  public void installLocals_always_returns_empty() {
    // this method is used only by medium tests
    assertThat(underTest.installLocals()).isEmpty();
  }

  private void enqueueDownload(String pluginKey, String pluginHash) throws IOException {
    File jar = temp.newFile();
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().putValue("Plugin-Key", pluginKey);
    try (JarOutputStream output = new JarOutputStream(FileUtils.openOutputStream(jar), manifest)) {

    }
    doReturn(Optional.of(jar)).when(pluginFiles).get(argThat(p -> pluginKey.equals(p.key) && pluginHash.equals(p.hash)));
  }

  private void enqueueNotFoundDownload(String pluginKey, String pluginHash) {
    doReturn(Optional.empty()).when(pluginFiles).get(argThat(p -> pluginKey.equals(p.key) && pluginHash.equals(p.hash)));
  }
}
