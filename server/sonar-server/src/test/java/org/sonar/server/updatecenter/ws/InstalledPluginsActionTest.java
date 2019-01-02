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
package org.sonar.server.updatecenter.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class InstalledPluginsActionTest {

  private ServerPluginRepository pluginRepository = mock(ServerPluginRepository.class);

  private WsActionTester ws = new WsActionTester(new InstalledPluginsAction(pluginRepository));

  @Test
  public void return_plugins() {
    when(pluginRepository.getPluginInfos()).thenReturn(asList(
      new PluginInfo("java").setName("Java").setVersion(Version.create("3.14")),
      new PluginInfo("xoo").setName("Xoo").setVersion(Version.create("1.0"))));

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("[" +
      "  {" +
      "    \"key\": \"java\",\n" +
      "    \"name\": \"Java\",\n" +
      "    \"version\": \"3.14\"\n" +
      "  }," +
      "  {" +
      "    \"key\": \"xoo\",\n" +
      "    \"name\": \"Xoo\",\n" +
      "    \"version\": \"1.0\"\n" +
      "  }" +
      "]");
  }

  @Test
  public void return_plugins_with_plugin_having_no_version() {
    when(pluginRepository.getPluginInfos()).thenReturn(singletonList(
      new PluginInfo("java").setName("Java")));

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo("[" +
      "  {" +
      "    \"key\": \"java\",\n" +
      "    \"name\": \"Java\"\n" +
      "  }" +
      "]");
  }

  @Test
  public void test_example() {
    when(pluginRepository.getPluginInfos()).thenReturn(asList(
      new PluginInfo("findbugs").setName("Findbugs").setVersion(Version.create("2.1")),
      new PluginInfo("l10nfr").setName("French Pack").setVersion(Version.create("1.10")),
      new PluginInfo("jira").setName("JIRA").setVersion(Version.create("1.2"))));

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(1);
  }

}
