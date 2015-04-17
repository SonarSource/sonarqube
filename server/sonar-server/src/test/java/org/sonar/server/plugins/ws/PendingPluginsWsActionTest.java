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
package org.sonar.server.plugins.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerPluginJarsInstaller;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class PendingPluginsWsActionTest {

  private static final String DUMMY_CONTROLLER_KEY = "dummy";

  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private ServerPluginJarsInstaller serverPluginJarsInstaller = mock(ServerPluginJarsInstaller.class);
  private PendingPluginsWsAction underTest = new PendingPluginsWsAction(pluginDownloader, serverPluginJarsInstaller);

  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Test
  public void action_pending_is_defined() throws Exception {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("pending");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_arrays_are_returned_when_there_nothing_pending() throws Exception {
    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_installing_plugin() throws Exception {
    when(pluginDownloader.getDownloads()).thenReturn(of("installed_file.jar"));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"installed_file.jar\"" +
        "      }" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_removing_plugin() throws Exception {
    when(serverPluginJarsInstaller.getUninstalledPluginFilenames()).thenReturn(of("removed_file.jar"));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": " +
        "  [" +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"removed_file.jar\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
      );
  }

  @Test
  public void installing_plugin_are_sorted_and_unique() throws Exception {
    when(pluginDownloader.getDownloads()).thenReturn(of("file2.jar", "file0.jar", "file0.jar", "file1.jar"));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file0.jar\"" +
        "      }" +
        "    }," +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file1.jar\"" +
        "      }" +
        "    }," +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file2.jar\"" +
        "      }" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void removing_plugin_are_sorted_and_unique() throws Exception {
    when(serverPluginJarsInstaller.getUninstalledPluginFilenames()).thenReturn(of("file2.jar", "file0.jar", "file0.jar", "file1.jar"));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": " +
        "  [" +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file0.jar\"" +
        "      }" +
        "    }," +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file1.jar\"" +
        "      }" +
        "    }," +
        "    {" +
        "      \"artifact\": {" +
        "        \"name\": \"file2.jar\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
      );
  }
}
