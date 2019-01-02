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

import com.google.common.base.Optional;
import java.net.URL;
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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.Version.create;

public abstract class AbstractUpdateCenterBasedPluginsWsActionTest {
  protected static final String DUMMY_CONTROLLER_KEY = "dummy";
  protected static final String JSON_EMPTY_PLUGIN_LIST = "{" +
    "  \"plugins\":" + "[]" +
    "}";
  protected static final Plugin PLUGIN_1 = Plugin.factory("pkey1").setName("p_name_1");
  protected static final Plugin PLUGIN_2 = Plugin.factory("pkey2").setName("p_name_2").setDescription("p_desc_2");

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
    Class<AvailableActionTest> clazz = AvailableActionTest.class;
    return clazz.getResource(clazz.getSimpleName() + "/" + s);
  }

  protected static PluginUpdate pluginUpdate(String key, String name) {
    return PluginUpdate.createWithStatus(
      new Release(Plugin.factory(key).setName(name), Version.create("1.0")),
      COMPATIBLE);
  }

  @Before
  public void wireMocksTogether() {
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
    when(updateCenter.getDate()).thenReturn(DateUtils.parseDateTime("2015-04-24T16:08:36+0200"));
  }
}
