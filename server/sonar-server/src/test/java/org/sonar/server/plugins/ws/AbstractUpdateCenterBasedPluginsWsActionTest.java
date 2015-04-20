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
import org.sonar.api.server.ws.Request;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import java.net.URL;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.Version.create;

public class AbstractUpdateCenterBasedPluginsWsActionTest {
  protected static final String DUMMY_CONTROLLER_KEY = "dummy";
  protected static final String JSON_EMPTY_PLUGIN_LIST =
    "{" +
      "  \"plugins\":" + "[]" +
      "}";
  protected static final Plugin PLUGIN_1 = new Plugin("p_key_1").setName("p_name_1");
  protected static final Plugin PLUGIN_2 = new Plugin("p_key_2").setName("p_name_2").setDescription("p_desc_2");

  protected static final Plugin FULL_PROPERTIES_PLUGIN = new Plugin("p_key")
    .setName("p_name")
    .setCategory("p_category")
    .setDescription("p_description")
    .setLicense("p_license")
    .setOrganization("p_orga_name")
    .setOrganizationUrl("p_orga_url")
    .setTermsConditionsUrl("p_t_and_c_url");
  protected static final Release FULL_PROPERTIES_PLUGIN_RELEASE = release(FULL_PROPERTIES_PLUGIN, "1.12.1")
    .setDate(DateUtils.parseDate("2015-04-16"))
    .setDownloadUrl("http://p_file.jar")
    .addOutgoingDependency(release(PLUGIN_1, "0.3.6"))
    .addOutgoingDependency(release(PLUGIN_2, "1.0.0"));

  protected UpdateCenterMatrixFactory updateCenterFactory = mock(UpdateCenterMatrixFactory.class);
  protected UpdateCenter updateCenter = mock(UpdateCenter.class);
  protected Request request = mock(Request.class);
  protected WsTester.TestResponse response = new WsTester.TestResponse();

  protected static Release release(Plugin plugin1, String version) {
    return new Release(plugin1, create(version));
  }

  protected static PluginUpdate pluginUpdate(Release pluginRelease, PluginUpdate.Status compatible) {
    return PluginUpdate.createWithStatus(pluginRelease, compatible);
  }

  protected static URL resource(String s) {
    Class<AvailablePluginsWsActionTest> clazz = AvailablePluginsWsActionTest.class;
    return clazz.getResource(clazz.getSimpleName() + "/" + s);
  }

  protected static PluginUpdate pluginUpdate(String key, String name) {
    return PluginUpdate.createWithStatus(
        new Release(new Plugin(key).setName(name), Version.create("1.0")),
        COMPATIBLE
    );
  }

  @Before
  public void wireMocksTogether() throws Exception {
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(updateCenter);
  }
}
