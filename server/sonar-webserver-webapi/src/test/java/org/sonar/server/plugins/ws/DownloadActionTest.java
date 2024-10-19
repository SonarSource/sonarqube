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
package org.sonar.server.plugins.ws;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;
import org.sonar.core.plugin.PluginType;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadActionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private final WsAction underTest = new DownloadAction(serverPluginRepository);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action def = tester.getDef();

    assertThat(def.isInternal()).isTrue();
    assertThat(def.since()).isEqualTo("7.2");
    assertThat(def.params())
      .extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("plugin");
    assertThat(def.changelog())
      .extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(new Tuple("9.8", "Parameter 'acceptCompressions' removed"));
  }

  @Test
  public void return_404_if_plugin_not_found() {
    when(serverPluginRepository.findPlugin("foo")).thenReturn(Optional.empty());

    TestRequest request = tester.newRequest().setParam("plugin", "foo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Plugin foo not found");
  }

  @Test
  public void return_jar_if_plugin_exists() throws Exception {
    ServerPlugin plugin = newPlugin();
    when(serverPluginRepository.findPlugin(plugin.getPluginInfo().getKey())).thenReturn(Optional.of(plugin));
    TestResponse response = tester.newRequest()
      .setParam("plugin", plugin.getPluginInfo().getKey())
      .execute();

    assertThat(response.getHeader("Sonar-MD5")).isEqualTo(plugin.getJar().getMd5());
    assertThat(response.getHeader("Sonar-Compression")).isNull();
    assertThat(response.getMediaType()).isEqualTo("application/java-archive");
    verifySameContent(response, plugin.getJar().getFile());
  }

  private ServerPlugin newPlugin() throws IOException {
    FileAndMd5 jar = new FileAndMd5(temp.newFile());
    return new ServerPlugin(new PluginInfo("foo"), PluginType.BUNDLED, null, jar, null);
  }

  private static void verifySameContent(TestResponse response, File file) throws IOException {
    assertThat(IOUtils.toByteArray(response.getInputStream())).isEqualTo(FileUtils.readFileToByteArray(file));
  }
}
