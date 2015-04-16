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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import java.net.URL;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.INCOMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.REQUIRE_SONAR_UPGRADE;
import static org.sonar.updatecenter.common.Version.create;

public class AvailablePluginsWsActionTest {
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  private static final String JSON_EMPTY_PLUGIN_LIST =
    "{" +
      "  \"plugins\":" + "[]" +
      "}";
  private static final Plugin PLUGIN_1 = new Plugin("p_key_1").setName("p_name_1");
  private static final Plugin PLUGIN_2 = new Plugin("p_key_2").setName("p_name_2").setDescription("p_desc_2");

  @Mock
  private UpdateCenterMatrixFactory updateCenterFactory;
  @Mock
  private UpdateCenter updateCenter;
  @InjectMocks
  private AvailablePluginsWsAction underTest;

  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Before
  public void createAndWireMocksTogether() throws Exception {
    initMocks(this);
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(updateCenter);
  }

  @Test
  public void action_available_is_defined() throws Exception {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("available");

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
    Plugin plugin = new Plugin("p_key")
      .setName("p_name")
      .setCategory("p_category")
      .setDescription("p_description")
      .setLicense("p_license")
      .setOrganization("p_orga_name")
      .setOrganizationUrl("p_orga_url")
      .setTermsConditionsUrl("p_t_and_c_url");
    Release pluginRelease = release(plugin, "1.12.1")
      .setDate(DateUtils.parseDate("2015-04-16"))
      .setDownloadUrl("http://p_file.jar")
      .addOutgoingDependency(release(PLUGIN_1, "0.3.6"))
      .addOutgoingDependency(release(PLUGIN_2, "1.0.0"));
    when(updateCenter.findAvailablePlugins()).thenReturn(of(
      pluginUpdate(pluginRelease, COMPATIBLE)
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(resource("properties_per_plugin.json"));
  }

  private Release release(Plugin plugin1, String version) {
    return new Release(plugin1, create(version));
  }

  @Test
  public void status_COMPATIBLE_is_displayed_COMPATIBLE_in_JSON() throws Exception {
    checkStatusDisplayedInJson(COMPATIBLE, "COMPATIBLE");
  }

  @Test
  public void status_INCOMPATIBLE_is_displayed_INCOMPATIBLE_in_JSON() throws Exception {
    checkStatusDisplayedInJson(INCOMPATIBLE, "INCOMPATIBLE");
  }

  @Test
  public void status_REQUIRE_SONAR_UPGRADE_is_displayed_REQUIRES_UPGRADE_in_JSON() throws Exception {
    checkStatusDisplayedInJson(REQUIRE_SONAR_UPGRADE, "REQUIRES_UPGRADE");
  }

  @Test
  public void status_DEPENDENCIES_REQUIRE_SONAR_UPGRADE_is_displayed_DEPS_REQUIRE_UPGRADE_in_JSON() throws Exception {
    checkStatusDisplayedInJson(DEPENDENCIES_REQUIRE_SONAR_UPGRADE, "DEPS_REQUIRE_UPGRADE");
  }

  private void checkStatusDisplayedInJson(PluginUpdate.Status status, String expectedValue) throws Exception {
    when(updateCenter.findAvailablePlugins()).thenReturn(of(
      pluginUpdate(release(PLUGIN_1, "1.0.0"), status)
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"update\": {" +
        "        \"status\": \"" + expectedValue + "\"" +
        "      }" +
        "    }" +
        "  ]" +
        "}"
      );
  }

  private static PluginUpdate pluginUpdate(Release pluginRelease, PluginUpdate.Status compatible) {
    return PluginUpdate.createWithStatus(pluginRelease, compatible);
  }

  private static URL resource(String s) {
    Class<AvailablePluginsWsActionTest> clazz = AvailablePluginsWsActionTest.class;
    return clazz.getResource(clazz.getSimpleName() + "/" + s);
  }
}
