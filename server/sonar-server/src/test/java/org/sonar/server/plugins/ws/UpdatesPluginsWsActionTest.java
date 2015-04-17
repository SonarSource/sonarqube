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
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;

public class UpdatesPluginsWsActionTest extends AbstractUpdateCenterBasedPluginsWsActionTest {

  private UpdatesPluginsWsAction underTest = new UpdatesPluginsWsAction(updateCenterFactory, new PluginWSCommons());

  @Test
  public void action_updatable_is_defined() throws Exception {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("updates");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_array_is_returned_when_there_is_no_plugin_available() throws Exception {
    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void verify_properties_displayed_in_json_per_plugin() throws Exception {
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate(FULL_PROPERTIES_PLUGIN_RELEASE, COMPATIBLE)
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(resource("properties_per_plugin.json"));
  }

  @Test
  public void status_COMPATIBLE_is_displayed_COMPATIBLE_in_JSON() throws Exception {
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate(release(PLUGIN_1, "1.0.0"), COMPATIBLE)
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"update\": {" +
        "        \"status\": \"COMPATIBLE\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
      );
  }

  @Test
  public void plugins_are_sorted_by_name_then_key_and_made_unique() throws Exception {
    when(updateCenter.findPluginUpdates()).thenReturn(of(
      pluginUpdate("key2", "name2"),
      pluginUpdate("key1", "name2"),
      pluginUpdate("key2", "name2"),
      pluginUpdate("key0", "name0"),
      pluginUpdate("key1", "name1")
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"name0\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key1\"," +
        "      \"name\": \"name1\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key1\"," +
        "      \"name\": \"name2\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"name2\"," +
        "    }," +
        "  ]" +
        "}"
      );
  }

  private static PluginUpdate pluginUpdate(String key, String name) {
    return PluginUpdate.createWithStatus(
      new Release(new Plugin(key).setName(name), Version.create("1.0")),
      COMPATIBLE
      );
  }
}
